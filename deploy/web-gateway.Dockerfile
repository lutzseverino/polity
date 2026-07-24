FROM node:24-alpine@sha256:a0b9bf06e4e6193cf7a0f58816cc935ff8c2a908f81e6f1a95432d679c54fbfd AS build

ENV COREPACK_HOME=/corepack
RUN corepack enable && corepack prepare pnpm@9.12.0 --activate

WORKDIR /workspace
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
COPY apps/web/package.json apps/web/package.json
RUN pnpm install --frozen-lockfile --filter @polity/web...

COPY apps/web apps/web
COPY scripts scripts
RUN pnpm build:web

FROM nginxinc/nginx-unprivileged:1.29-alpine@sha256:0c79d56aee561a1d81c63f00eee5fb5fe29279560cdc55e91425133104c7fbe6

COPY deploy/15-validate-web-gateway-env.sh /docker-entrypoint.d/15-validate-web-gateway-env.sh
COPY deploy/19-lock-web-gateway-envsubst.envsh /docker-entrypoint.d/19-lock-web-gateway-envsubst.envsh
COPY deploy/web-gateway.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /workspace/apps/web/dist /usr/share/nginx/html

ENV CARDO_IDENTITY_UPSTREAM=identity:8081 \
    POLITY_EXTERNAL_HOST=localhost \
    POLITY_EXTERNAL_PORT=8080 \
    POLITY_EXTERNAL_SCHEME=http \
    POLITY_SERVICE_UPSTREAM=polity:8084 \
    POLITY_WEB_PORT=8080

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --quiet --spider "http://127.0.0.1:${POLITY_WEB_PORT}/healthz" || exit 1
