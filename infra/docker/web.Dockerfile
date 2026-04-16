FROM node:24-alpine AS builder

WORKDIR /workspace
COPY package.json .npmrc tsconfig.base.json eslint.config.js prettier.config.mjs ./
COPY apps/web/package.json apps/web/package.json
COPY packages/ui/package.json packages/ui/package.json
COPY packages/contracts/package.json packages/contracts/package.json
RUN npm install

COPY . .
RUN npm run build

FROM nginx:1.27-alpine

COPY infra/docker/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /workspace/apps/web/dist /usr/share/nginx/html
EXPOSE 80
