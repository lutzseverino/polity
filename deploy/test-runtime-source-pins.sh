#!/bin/sh

set -eu

ROOT=$(CDPATH='' cd -- "$(dirname -- "$0")/.." && pwd)
CHECKER="$ROOT/deploy/check-runtime-source-pins.mjs"
FIXTURES=$(mktemp -d "${TMPDIR:-/tmp}/polity-runtime-source-pins.XXXXXX")
DIGEST=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef

cleanup() {
  rm -rf "$FIXTURES"
}
trap cleanup EXIT HUP INT TERM

write_fixture() {
  name=$1
  content=$2
  directory="$FIXTURES/$name"
  mkdir -p "$directory"
  printf '%s\n' "$content" >"$directory/Dockerfile"
}

assert_passes() {
  name=$1
  content=$2
  write_fixture "$name" "$content"
  if ! "$CHECKER" "$FIXTURES/$name" >"$FIXTURES/$name.stdout" 2>"$FIXTURES/$name.stderr"; then
    cat "$FIXTURES/$name.stderr" >&2
    echo "Expected runtime source pin fixture to pass: $name" >&2
    exit 1
  fi
}

assert_fails() {
  name=$1
  expected=$2
  content=$3
  write_fixture "$name" "$content"
  if "$CHECKER" "$FIXTURES/$name" >"$FIXTURES/$name.stdout" 2>"$FIXTURES/$name.stderr"; then
    echo "Expected runtime source pin fixture to fail: $name" >&2
    exit 1
  fi
  if ! grep -Fq "$expected" "$FIXTURES/$name.stderr"; then
    cat "$FIXTURES/$name.stderr" >&2
    echo "Runtime source pin fixture did not report '$expected': $name" >&2
    exit 1
  fi
}

"$CHECKER" "$ROOT/deploy"

assert_passes flags-and-internal-stage-reuse \
  "FROM --platform=linux/amd64 example/runtime:1.2-alpine@sha256:$DIGEST AS base
FROM --platform=linux/amd64 base AS final
RUN true"

assert_passes immutable-scratch \
  "FROM ScRaTcH AS empty
FROM empty AS final"

assert_fails unpinned-single-line \
  "must include a sha256 digest" \
  "FROM example/runtime:1.2-alpine"

assert_fails split-keyword-unpinned \
  "must include a sha256 digest" \
  'FR\
OM example/runtime:1.2-alpine'

assert_fails latest-tag \
  "readable, non-latest tag" \
  "FROM example/runtime:latest@sha256:$DIGEST"

assert_fails missing-tag \
  "readable, non-latest tag" \
  "FROM example/runtime@sha256:$DIGEST"

assert_fails short-digest \
  "full sha256 digest" \
  "FROM example/runtime:1.2-alpine@sha256:0123456789abcdef"

assert_fails uppercase-digest \
  "lowercase hexadecimal sha256 digest" \
  "FROM example/runtime:1.2-alpine@sha256:0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdeF"

assert_fails unsupported-dynamic-from \
  "dynamic FROM expressions are unsupported" \
  "ARG BASE_IMAGE
FROM \${BASE_IMAGE}"

assert_fails unsupported-escape-directive \
  "only backslash Dockerfile continuations are supported" \
  '# escape=`
FROM example/runtime:1.2-alpine'

assert_fails unsupported-syntax-directive \
  "custom Dockerfile syntax frontends are unsupported" \
  '  # SyNtAx = example/custom-dockerfile:latest
FROM example/runtime:1.2-alpine'

assert_fails unsupported-heredoc \
  "Dockerfile heredocs are unsupported" \
  "FROM example/runtime:1.2-alpine@sha256:$DIGEST
RUN <<EOF
payload
EOF"

assert_fails supported-instruction-heredoc-delimiter \
  "Dockerfile heredocs are unsupported" \
  "FROM example/runtime:1.2-alpine@sha256:$DIGEST
RUN <<RUN
RUN true
RUN"

echo "Runtime source pin tests passed."
