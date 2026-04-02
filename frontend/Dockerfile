# ─── Build Stage ──────────────────────────────────────────────────────────
FROM node:20-alpine AS build
WORKDIR /app

# Install dependencies based on the preferred package manager
COPY package.json package-lock.json ./
RUN npm ci

# Copy source and build
COPY . .

# Backend URL is baked into rewrites at build time.
# In Docker Compose this is set to http://backend:8080 via build args.
ARG BACKEND_URL=http://localhost:8080
ENV BACKEND_URL=$BACKEND_URL
ENV NEXT_TELEMETRY_DISABLED=1

RUN npm run build

# ─── Runtime Stage ────────────────────────────────────────────────────────
FROM node:20-alpine AS runner
WORKDIR /app

ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1

# BACKEND_URL is needed at runtime for Next.js rewrites (evaluated on server start).
# Docker Compose passes it via the environment key; the default matches local dev.
ARG BACKEND_URL=http://localhost:8080
ENV BACKEND_URL=$BACKEND_URL

RUN addgroup --system --gid 1001 nodejs
RUN adduser --system --uid 1001 nextjs

# Copy only the standalone server + static assets (no full node_modules)
COPY --from=build --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=build --chown=nextjs:nodejs /app/.next/static ./.next/static

USER nextjs

EXPOSE 3000

ENV PORT=3000
ENV HOSTNAME="0.0.0.0"

CMD ["node", "server.js"]
