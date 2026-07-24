FROM node:24-alpine

WORKDIR /test
COPY mock-upstreams.mjs mock-upstreams.mjs

USER node

ENTRYPOINT ["node", "/test/mock-upstreams.mjs"]
