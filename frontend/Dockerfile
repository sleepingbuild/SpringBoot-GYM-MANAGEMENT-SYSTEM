# Dockerfile frontend GMS (Agent 1). Next.js 14 che do standalone.
# NEXT_PUBLIC_API_BASE_URL duoc nhung vao ma JS luc BUILD (trinh duyet goi thang backend), nen phai truyen bang build arg.
FROM node:20-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

FROM node:20-alpine AS build
WORKDIR /app
ARG NEXT_PUBLIC_API_BASE_URL=http://localhost:8082
ENV NEXT_PUBLIC_API_BASE_URL=$NEXT_PUBLIC_API_BASE_URL \
    NEXT_TELEMETRY_DISABLED=1
COPY --from=deps /app/node_modules ./node_modules
COPY . .
RUN npm run build

FROM node:20-alpine AS run
WORKDIR /app
ENV NODE_ENV=production \
    NEXT_TELEMETRY_DISABLED=1 \
    PORT=3000 \
    HOSTNAME=0.0.0.0
RUN addgroup -S gms && adduser -S gms -G gms
COPY --from=build --chown=gms:gms /app/public ./public
COPY --from=build --chown=gms:gms /app/.next/standalone ./
COPY --from=build --chown=gms:gms /app/.next/static ./.next/static
USER gms
EXPOSE 3000
CMD ["node", "server.js"]
