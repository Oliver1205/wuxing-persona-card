ARG NODE_IMAGE=node:20-alpine
ARG NGINX_IMAGE=nginx:1.27-alpine

FROM ${NODE_IMAGE} AS frontend-build
ARG VITE_ICP_RECORD_NO=""
ARG VITE_ICP_LINK="https://beian.miit.gov.cn/"
ENV VITE_ICP_RECORD_NO=${VITE_ICP_RECORD_NO}
ENV VITE_ICP_LINK=${VITE_ICP_LINK}

WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM ${NGINX_IMAGE}

COPY deploy/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=frontend-build /app/frontend/dist /usr/share/nginx/html

EXPOSE 80
