# ── 빌드 스테이지 ────────────────────────────────────────────────────────────
FROM node:20-alpine AS builder
WORKDIR /app

# package*.json만 먼저 복사해 npm ci 레이어를 캐시.
# 소스 파일만 바뀌면 npm ci를 다시 실행하지 않아 빌드가 빨라진다.
COPY package*.json ./
RUN npm ci

COPY . .

# Vite의 VITE_* 환경변수는 런타임이 아니라 빌드 시점에 번들에 박힌다.
# ARG로 빌드 인자를 받아 ENV로 노출해야 npm run build 중에 값이 적용된다.
ARG VITE_TOSS_CLIENT_KEY
ENV VITE_TOSS_CLIENT_KEY=$VITE_TOSS_CLIENT_KEY

# Vite 프로덕션 빌드 → dist/ 생성
RUN npm run build

# ── 서빙 스테이지 ─────────────────────────────────────────────────────────────
# Nginx: 정적 파일 서빙 + /api 프록시를 담당하는 경량 웹서버
FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
# nginx:alpine 1.19+ 는 /etc/nginx/templates/*.template 파일을 컨테이너 기동 시
# envsubst로 처리해 /etc/nginx/conf.d/ 에 출력한다.
# → BACKEND_URL 환경변수가 런타임에 주입되어 로컬/Azure 환경 모두 동일 이미지 사용 가능.
COPY nginx.conf /etc/nginx/templates/default.conf.template

# 컨테이너 시작 시 /etc/resolv.conf에서 DNS resolver를 읽어 NGINX_RESOLVER 환경변수로 설정
# 이 값이 nginx.conf.template의 ${NGINX_RESOLVER}로 치환됨
COPY docker-entrypoint-wrapper.sh /docker-entrypoint-wrapper.sh
RUN chmod +x /docker-entrypoint-wrapper.sh
ENTRYPOINT ["/docker-entrypoint-wrapper.sh"]
CMD ["nginx", "-g", "daemon off;"]
EXPOSE 80
