# ── Stage 1: build ──────────────────────────────────────────────────────────
FROM node:16-alpine AS builder

WORKDIR /app

COPY package.json yarn.lock ./
RUN yarn --frozen-lockfile --ignore-scripts

COPY . .

RUN yarn build --mode docker

FROM nginx:1.25-alpine

COPY --from=builder /app/dist /usr/share/nginx/html

COPY docker/nginx.conf.template /etc/nginx/nginx.conf.template

COPY docker/docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

EXPOSE 80

ENTRYPOINT ["/docker-entrypoint.sh"]
