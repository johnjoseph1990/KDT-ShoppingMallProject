# ── 빌드 스테이지 ────────────────────────────────────────────────────────────
FROM node:20-alpine AS builder
WORKDIR /app

# package*.json만 먼저 복사해 npm ci 레이어를 캐시.
# 소스 파일만 바뀌면 npm ci를 다시 실행하지 않아 빌드가 빨라진다.
COPY package*.json ./
RUN npm ci

COPY . .
# Vite 프로덕션 빌드 → dist/ 생성
RUN npm run build

# ── 서빙 스테이지 ─────────────────────────────────────────────────────────────
# Nginx: 정적 파일 서빙 + /api 프록시를 담당하는 경량 웹서버
FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
# 기본 Nginx 설정을 덮어써 SPA fallback과 API 프록시를 적용한다
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
