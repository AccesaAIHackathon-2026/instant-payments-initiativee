# Stage 1 — build
FROM node:22-alpine AS builder
WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .

# Build-time variables — baked into the static bundle.
# Pass via docker-compose build args (sourced from poc/.env).
ARG BANK_API_KEY=blinkpay-poc-key
ARG BANK_A_BASE_URL=http://localhost:8080
ARG BANK_B_BASE_URL=http://localhost:8082
ENV VITE_BANK_API_KEY=${BANK_API_KEY}
ENV VITE_BANK_A_BASE_URL=${BANK_A_BASE_URL}
ENV VITE_BANK_B_BASE_URL=${BANK_B_BASE_URL}

RUN npm run build

# Stage 2 — serve
FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
EXPOSE 80
