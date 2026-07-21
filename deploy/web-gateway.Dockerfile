FROM node:24-alpine AS build

ENV COREPACK_HOME=/corepack
RUN corepack enable && corepack prepare pnpm@9.12.0 --activate

WORKDIR /workspace
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
COPY apps/web/package.json apps/web/package.json
RUN pnpm install --frozen-lockfile --filter @polity/web...

COPY apps/web apps/web
COPY scripts scripts
RUN pnpm build:web

FROM nginxinc/nginx-unprivileged:1.29-alpine

COPY deploy/web-gateway.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /workspace/apps/web/dist /usr/share/nginx/html

ENV CARDO_IDENTITY_UPSTREAM=identity:8081 \
    NGINX_ENVSUBST_FILTER="^(CARDO_IDENTITY_UPSTREAM|POLITY_SERVICE_UPSTREAM|POLITY_WEB_PORT)$" \
    POLITY_SERVICE_UPSTREAM=polity:8084 \
    POLITY_WEB_PORT=8080

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --quiet --spider "http://127.0.0.1:${POLITY_WEB_PORT}/healthz" || exit 1
