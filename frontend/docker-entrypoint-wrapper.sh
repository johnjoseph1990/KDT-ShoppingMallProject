#!/bin/sh
# /etc/resolv.conf에서 실제 DNS nameserver 주소를 읽어 NGINX_RESOLVER 환경변수로 설정
# nginx.conf.template의 ${NGINX_RESOLVER}가 이 값으로 치환되어, 컨테이너 환경에 맞는
# DNS를 사용하게 됨 (Azure Container Apps의 Kubernetes CoreDNS 주소가 환경마다 다르므로)
export NGINX_RESOLVER=$(awk '/^nameserver/{print $2; exit}' /etc/resolv.conf)
echo "NGINX_RESOLVER=${NGINX_RESOLVER}"
# nginx 공식 entrypoint(/docker-entrypoint.sh)를 실행 — envsubst 처리 + nginx 시작
exec /docker-entrypoint.sh "$@"
