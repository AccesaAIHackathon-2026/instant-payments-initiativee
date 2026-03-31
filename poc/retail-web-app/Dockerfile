# Stage 1 — build
FROM node:22-alpine AS builder
WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .

# VITE_BANK_API_KEY is a build-time variable — baked into the static bundle.
# Pass it via docker-compose build args (sourced from poc/.env).
ARG BANK_API_KEY=blinkpay-poc-key
ENV VITE_BANK_API_KEY=${BANK_API_KEY}

RUN npm run build

# Stage 2 — serve
FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
EXPOSE 80
