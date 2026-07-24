#!/bin/sh

set -eu

ROOT=$(CDPATH='' cd -- "$(dirname -- "$0")/.." && pwd)
TEST_ID="polity-runtime-smoke-$$"
NETWORK="$TEST_ID"
MOCK_CONTAINER="$TEST_ID-upstreams"
GATEWAY_CONTAINER="$TEST_ID-gateway"
SERVICE_IMAGE="$TEST_ID-service"
GATEWAY_IMAGE="$TEST_ID-web"
MOCK_IMAGE="$TEST_ID-upstreams"
RESULTS=$(mktemp -d "${TMPDIR:-/tmp}/polity-runtime-smoke.XXXXXX")
INVALID_CONTAINER=

cleanup() {
  if [ -n "$INVALID_CONTAINER" ]; then
    docker rm --force "$INVALID_CONTAINER" >/dev/null 2>&1 || true
  fi
  docker rm --force "$GATEWAY_CONTAINER" "$MOCK_CONTAINER" >/dev/null 2>&1 || true
  docker network rm "$NETWORK" >/dev/null 2>&1 || true
  docker image rm --force "$SERVICE_IMAGE" "$GATEWAY_IMAGE" "$MOCK_IMAGE" >/dev/null 2>&1 || true
  rm -rf "$RESULTS"
}
trap cleanup EXIT HUP INT TERM

cd "$ROOT"

if ! find services/polity/target -maxdepth 1 -name 'polity-*.jar' -print -quit 2>/dev/null |
  grep -q .; then
  echo "Package the Polity service before running the runtime smoke test." >&2
  exit 1
fi

docker build --file deploy/polity-service.Dockerfile --tag "$SERVICE_IMAGE" .
docker build --file deploy/web-gateway.Dockerfile --tag "$GATEWAY_IMAGE" .
docker build \
  --file deploy/test/mock-upstreams.Dockerfile \
  --tag "$MOCK_IMAGE" \
  deploy/test

assert_gateway_rejects() {
  case_name=$1
  variable=$2
  value=$3
  INVALID_CONTAINER="$TEST_ID-invalid-$case_name"

  docker run \
    --detach \
    --env "$variable=$value" \
    --name "$INVALID_CONTAINER" \
    "$GATEWAY_IMAGE" >/dev/null

  attempt=0
  while [ "$(docker inspect --format '{{.State.Running}}' "$INVALID_CONTAINER")" = true ]; do
    attempt=$((attempt + 1))
    if [ "$attempt" -ge 3 ]; then
      docker logs "$INVALID_CONTAINER" >&2
      echo "Gateway accepted invalid $variable for case $case_name." >&2
      exit 1
    fi
    sleep 1
  done

  exit_code=$(docker inspect --format '{{.State.ExitCode}}' "$INVALID_CONTAINER")
  docker logs "$INVALID_CONTAINER" >"$RESULTS/invalid-$case_name.log" 2>&1
  docker rm "$INVALID_CONTAINER" >/dev/null
  INVALID_CONTAINER=

  if [ "$exit_code" -eq 0 ]; then
    echo "Gateway did not fail closed for invalid $variable in case $case_name." >&2
    exit 1
  fi
  if ! grep -q "Invalid $variable" "$RESULTS/invalid-$case_name.log"; then
    echo "Gateway did not identify invalid $variable in case $case_name." >&2
    cat "$RESULTS/invalid-$case_name.log" >&2
    exit 1
  fi
}

assert_gateway_accepts_ipv4() {
  INVALID_CONTAINER="$TEST_ID-valid-ipv4"
  docker run \
    --detach \
    --env CARDO_IDENTITY_UPSTREAM=192.0.2.10:8081 \
    --env POLITY_EXTERNAL_HOST=198.51.100.20 \
    --env POLITY_EXTERNAL_PORT=443 \
    --env POLITY_EXTERNAL_SCHEME=https \
    --env POLITY_SERVICE_UPSTREAM=203.0.113.30:8084 \
    --env POLITY_WEB_PORT=8080 \
    --name "$INVALID_CONTAINER" \
    "$GATEWAY_IMAGE" >/dev/null

  sleep 1
  if [ "$(docker inspect --format '{{.State.Running}}' "$INVALID_CONTAINER")" != true ]; then
    docker logs "$INVALID_CONTAINER" >&2
    echo "Gateway rejected valid IPv4 runtime inputs." >&2
    exit 1
  fi

  docker rm --force "$INVALID_CONTAINER" >/dev/null
  INVALID_CONTAINER=
}

assert_gateway_rejects scheme-syntax POLITY_EXTERNAL_SCHEME 'https;include /tmp/injected.conf'
assert_gateway_rejects scheme-value POLITY_EXTERNAL_SCHEME ftp
assert_gateway_rejects external-host POLITY_EXTERNAL_HOST 'polity.example.test;return 200'
assert_gateway_rejects ipv4-leading-zero POLITY_EXTERNAL_HOST 192.168.001.10
assert_gateway_rejects ipv4-range POLITY_EXTERNAL_HOST 192.168.0.256
assert_gateway_rejects external-port-syntax POLITY_EXTERNAL_PORT '443 default_server'
assert_gateway_rejects external-port-low POLITY_EXTERNAL_PORT 0
assert_gateway_rejects web-port-high POLITY_WEB_PORT 65536
assert_gateway_rejects identity-upstream CARDO_IDENTITY_UPSTREAM 'identity:8081/api'
assert_gateway_rejects polity-upstream POLITY_SERVICE_UPSTREAM 'polity:8084;include'
assert_gateway_rejects ipv6-external-host POLITY_EXTERNAL_HOST '[2001:db8::1]'
assert_gateway_rejects ipv6-upstream CARDO_IDENTITY_UPSTREAM '[2001:db8::1]:8081'
assert_gateway_accepts_ipv4

docker network create "$NETWORK" >/dev/null
docker run --detach --network "$NETWORK" --name "$MOCK_CONTAINER" "$MOCK_IMAGE" >/dev/null
docker run \
  --detach \
  --env CARDO_IDENTITY_UPSTREAM="$MOCK_CONTAINER:8081" \
  --env NGINX_ENVSUBST_FILTER='.*' \
  --env NGINX_ENVSUBST_OUTPUT_DIR=/tmp/runtime-conf \
  --env NGINX_ENVSUBST_STREAM_OUTPUT_DIR=/tmp/runtime-stream-conf \
  --env NGINX_ENVSUBST_STREAM_TEMPLATE_SUFFIX=.template \
  --env NGINX_ENVSUBST_TEMPLATE_DIR=/tmp/runtime-templates \
  --env NGINX_ENVSUBST_TEMPLATE_SUFFIX=.runtime-template \
  --env POLITY_EXTERNAL_HOST=polity.example.test \
  --env POLITY_EXTERNAL_PORT=443 \
  --env POLITY_EXTERNAL_SCHEME=https \
  --env POLITY_SERVICE_UPSTREAM="$MOCK_CONTAINER:8084" \
  --env remote_addr=198.51.100.66 \
  --env uri=/runtime-injected.html \
  --name "$GATEWAY_CONTAINER" \
  --network "$NETWORK" \
  --publish 127.0.0.1::8080 \
  "$GATEWAY_IMAGE" >/dev/null

GATEWAY_PORT=$(docker port "$GATEWAY_CONTAINER" 8080/tcp | sed -n '1s/.*://p')
if [ -z "$GATEWAY_PORT" ]; then
  echo "Could not determine the gateway's published port." >&2
  exit 1
fi
GATEWAY_URL="http://127.0.0.1:$GATEWAY_PORT"

request_with_spoofed_forwarding() {
  curl --fail --silent --show-error \
    --header 'Forwarded: for=198.51.100.1;host=forwarded.invalid;proto=http' \
    --header 'Host: host.invalid' \
    --header 'X-Forwarded-For: 198.51.100.2, 198.51.100.3' \
    --header 'X-Forwarded-Host: forwarded-host.invalid' \
    --header 'X-Forwarded-Port: 81' \
    --header 'X-Forwarded-Prefix: /spoofed' \
    --header 'X-Forwarded-Proto: http' \
    --header 'X-Forwarded-Server: forwarded-server.invalid' \
    --header 'X-Forwarded-Ssl: on' \
    --header 'X-Real-IP: 198.51.100.4' \
    "$1"
}

attempt=0
until curl --fail --silent --show-error "$GATEWAY_URL/healthz" >"$RESULTS/health.json"; do
  attempt=$((attempt + 1))
  if [ "$attempt" -ge 30 ]; then
    docker logs "$GATEWAY_CONTAINER" >&2
    exit 1
  fi
  sleep 1
done

curl --fail --silent --show-error \
  "$GATEWAY_URL/api/v1/identity/sessions?flow=exact" \
  >"$RESULTS/identity-exact.json"
request_with_spoofed_forwarding \
  "$GATEWAY_URL/api/v1/identity/sessions/current?flow=descendant" \
  >"$RESULTS/identity-descendant.json"
request_with_spoofed_forwarding \
  "$GATEWAY_URL/api/v1/polities/example?view=full" \
  >"$RESULTS/polity.json"
curl --fail --silent --show-error \
  "$GATEWAY_URL/polities/example/government" \
  >"$RESULTS/spa.html"
docker exec "$GATEWAY_CONTAINER" nginx -T >"$RESULTS/nginx.txt" 2>&1
docker image inspect "$SERVICE_IMAGE" >"$RESULTS/service-image.json"

node --input-type=module - "$RESULTS" "$MOCK_CONTAINER" <<'EOF'
import { readFileSync } from "node:fs";
import { join } from "node:path";

const directory = process.argv[2];
const mockContainer = process.argv[3];
const readJson = (name) =>
  JSON.parse(readFileSync(join(directory, name), "utf8"));
const fail = (message) => {
  throw new Error(message);
};
const assert = (condition, message) => {
  if (!condition) {
    fail(message);
  }
};
const assertForwardingPolicy = (response, service) => {
  assert(
    response.headers.host === "polity.example.test",
    `${service} upstream Host did not use the external host`,
  );
  assert(
    response.headers["x-forwarded-host"] === "polity.example.test",
    `${service} forwarded host did not use the configured external host`,
  );
  assert(
    response.headers["x-forwarded-port"] === "443",
    `${service} forwarded port did not use the configured external port`,
  );
  assert(
    response.headers["x-forwarded-proto"] === "https",
    `${service} internal HTTP hop downgraded the browser-visible HTTPS scheme`,
  );
  assert(
    response.headers["x-forwarded-for"] &&
      !response.headers["x-forwarded-for"].includes("198.51.100.") &&
      !response.headers["x-forwarded-for"].includes(","),
    `${service} received a client-supplied forwarded-for chain`,
  );
  assert(
    response.headers["x-real-ip"] === response.headers["x-forwarded-for"],
    `${service} transport-peer address headers disagree`,
  );
  for (const header of [
    "forwarded",
    "x-forwarded-prefix",
    "x-forwarded-server",
    "x-forwarded-ssl",
  ]) {
    assert(!(header in response.headers), `client-supplied ${header} reached ${service}`);
  }
};

const health = readJson("health.json");
assert(health.status === "UP", "gateway liveness did not report UP");

const identityExact = readJson("identity-exact.json");
assert(identityExact.service === "identity", "exact Identity session route used the wrong upstream");
assert(
  identityExact.url === "/api/v1/identity/sessions?flow=exact",
  "exact Identity session path or query was rewritten",
);

const identity = readJson("identity-descendant.json");
assert(identity.service === "identity", "descendant Identity session route used the wrong upstream");
assert(
  identity.url === "/api/v1/identity/sessions/current?flow=descendant",
  "descendant Identity session path or query was rewritten",
);
assertForwardingPolicy(identity, "Identity");

const polity = readJson("polity.json");
assert(polity.service === "polity", "Polity API route used the wrong upstream");
assert(
  polity.url === "/api/v1/polities/example?view=full",
  "Polity API path or query was rewritten",
);
assertForwardingPolicy(polity, "Polity");

const spa = readFileSync(join(directory, "spa.html"), "utf8");
assert(spa.includes('<div id="root"></div>'), "SPA fallback did not return the web application");

const nginx = readFileSync(join(directory, "nginx.txt"), "utf8");
assert(!nginx.includes("${"), "rendered Nginx configuration contains unsubstituted variables");
assert(
  nginx.includes("proxy_set_header X-Forwarded-For $remote_addr;"),
  "runtime envsubst overrides substituted an Nginx built-in variable",
);
assert(
  nginx.includes("try_files $uri $uri/ /index.html;"),
  "runtime envsubst overrides substituted an Nginx URI variable",
);
for (const [snippet, input] of [
  ["listen 8080;", "POLITY_WEB_PORT"],
  ["proxy_set_header Host polity.example.test;", "POLITY_EXTERNAL_HOST"],
  ["proxy_set_header X-Forwarded-Port 443;", "POLITY_EXTERNAL_PORT"],
  ["proxy_set_header X-Forwarded-Proto https;", "POLITY_EXTERNAL_SCHEME"],
  [`proxy_pass http://${mockContainer}:8081;`, "CARDO_IDENTITY_UPSTREAM"],
  [`proxy_pass http://${mockContainer}:8084;`, "POLITY_SERVICE_UPSTREAM"],
]) {
  assert(nginx.includes(snippet), `rendered Nginx configuration lost ${input}`);
}

const [serviceImage] = readJson("service-image.json");
assert(serviceImage.Config.User === "polity", "Polity service image does not run as the non-root user");
assert(
  serviceImage.Config.Healthcheck?.Test?.join(" ").includes("/actuator/health/readiness"),
  "Polity service image does not probe service readiness",
);
EOF

echo "Runtime image smoke test passed."
