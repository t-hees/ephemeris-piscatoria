# Build Stage
FROM node:26-alpine AS build

WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# Production Stage
FROM docker.io/library/nginx:stable-alpine AS production
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
# Rate limits per client address used by the template. The e2e stack raises them, as all its requests
# come from one address. Registration: one per minute after the first 3.
ENV RATE_LIMIT_REGISTER=1r/m \
    RATE_LIMIT_REGISTER_BURST=3 \
    RATE_LIMIT_LOGIN=10r/m \
    RATE_LIMIT_LOGIN_BURST=5 \
    RATE_LIMIT_API=10r/s \
    RATE_LIMIT_API_BURST=30
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
