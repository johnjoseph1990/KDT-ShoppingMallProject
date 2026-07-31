#!/usr/bin/env bash
# Azure Container Apps 인프라 초기 생성 스크립트
# 실행 전 필수: az login 으로 Azure에 로그인한 상태여야 함
# 실행 방법: bash azure/setup.sh
# 이 스크립트는 멱등하지 않음 — 이미 리소스가 있으면 일부 명령이 실패할 수 있음

set -euo pipefail

# ── 설정값 (필요시 여기만 수정) ────────────────────────────────────────────────
RESOURCE_GROUP="mins-shopping-rg"
LOCATION="koreacentral"
ACR_NAME="minsshoppingacr"           # 전역 고유해야 함 (소문자+숫자만)
ACA_ENV="mins-shopping-env"
BACKEND_APP="backend"
FRONTEND_APP="frontend"
PG_SERVER="mins-shopping-pg"         # 전역 고유해야 함
PG_DB="shoppingmall"
PG_USER="mins"
GITHUB_REPO="johnjoseph1990/KDT-ShoppingMallProject"

# ── 비밀값 (스크립트 실행 전 환경변수로 설정하거나 아래에 직접 입력) ──────────
# export DB_PASSWORD="your-db-password"
# export TOSS_SECRET_KEY="your-toss-secret-key"
# export AZURE_STORAGE_CONNECTION_STRING="DefaultEndpointsProtocol=https;..."
# export VITE_TOSS_CLIENT_KEY="your-toss-client-key"
DB_PASSWORD="${DB_PASSWORD:-}"
TOSS_SECRET_KEY="${TOSS_SECRET_KEY:-}"
AZURE_STORAGE_CONNECTION_STRING="${AZURE_STORAGE_CONNECTION_STRING:-}"
VITE_TOSS_CLIENT_KEY="${VITE_TOSS_CLIENT_KEY:-}"

if [[ -z "$DB_PASSWORD" || -z "$TOSS_SECRET_KEY" ]]; then
  echo "ERROR: DB_PASSWORD, TOSS_SECRET_KEY 환경변수를 먼저 설정하세요."
  echo "  export DB_PASSWORD='...'  TOSS_SECRET_KEY='...'  AZURE_STORAGE_CONNECTION_STRING='...'  VITE_TOSS_CLIENT_KEY='...'"
  exit 1
fi

echo "=== 1. Resource Group 생성 ==="
az group create --name "$RESOURCE_GROUP" --location "$LOCATION"

echo "=== 2. Azure Container Registry (ACR) 생성 ==="
az acr create \
  --resource-group "$RESOURCE_GROUP" \
  --name "$ACR_NAME" \
  --sku Basic \
  --admin-enabled true   # GitHub Actions에서 admin 계정으로 로그인하기 위해 활성화

ACR_LOGIN_SERVER=$(az acr show --name "$ACR_NAME" --query loginServer --output tsv)
ACR_USERNAME=$(az acr credential show --name "$ACR_NAME" --query username --output tsv)
ACR_PASSWORD=$(az acr credential show --name "$ACR_NAME" --query "passwords[0].value" --output tsv)
echo "ACR: $ACR_LOGIN_SERVER"

echo "=== 3. Container Apps Environment 생성 ==="
az containerapp env create \
  --name "$ACA_ENV" \
  --resource-group "$RESOURCE_GROUP" \
  --location "$LOCATION"

echo "=== 4. PostgreSQL Flexible Server 생성 ==="
# Burstable B1ms: 최소 비용 티어 (~$13/월). 운영 부하가 커지면 General Purpose로 업그레이드
az postgres flexible-server create \
  --resource-group "$RESOURCE_GROUP" \
  --name "$PG_SERVER" \
  --location "$LOCATION" \
  --admin-user "$PG_USER" \
  --admin-password "$DB_PASSWORD" \
  --sku-name Standard_B1ms \
  --tier Burstable \
  --storage-size 32 \
  --version 16 \
  --yes

# DB 생성
az postgres flexible-server db create \
  --resource-group "$RESOURCE_GROUP" \
  --server-name "$PG_SERVER" \
  --database-name "$PG_DB"

# Azure 서비스(Container Apps 포함)에서 접근 허용
# start/end IP를 0.0.0.0으로 설정하면 Azure 내부 IP 전체 허용
az postgres flexible-server firewall-rule create \
  --resource-group "$RESOURCE_GROUP" \
  --name "$PG_SERVER" \
  --rule-name AllowAzureServices \
  --start-ip-address 0.0.0.0 \
  --end-ip-address 0.0.0.0

PG_HOST="${PG_SERVER}.postgres.database.azure.com"
echo "PostgreSQL 호스트: $PG_HOST"

echo "=== 5. Backend Container App 생성 (내부 ingress) ==="
# --ingress internal: 인터넷에 직접 노출되지 않고, 같은 Environment 내 frontend만 접근 가능
az containerapp create \
  --name "$BACKEND_APP" \
  --resource-group "$RESOURCE_GROUP" \
  --environment "$ACA_ENV" \
  --image "mcr.microsoft.com/azuredocs/containerapps-helloworld:latest" \
  --target-port 8080 \
  --ingress internal \
  --registry-server "$ACR_LOGIN_SERVER" \
  --registry-username "$ACR_USERNAME" \
  --registry-password "$ACR_PASSWORD" \
  --min-replicas 0 \
  --max-replicas 2 \
  --secrets \
    "pg-password=${DB_PASSWORD}" \
    "toss-secret-key=${TOSS_SECRET_KEY}" \
    "azure-storage-conn=${AZURE_STORAGE_CONNECTION_STRING}" \
  --env-vars \
    "SPRING_DATASOURCE_URL=jdbc:postgresql://${PG_HOST}:5432/${PG_DB}?sslmode=require" \
    "SPRING_DATASOURCE_USERNAME=${PG_USER}" \
    "SPRING_DATASOURCE_PASSWORD=secretref:pg-password" \
    "SPRING_PROFILES_ACTIVE=prod" \
    "DDL_AUTO=update" \
    "TOSS_SECRET_KEY=secretref:toss-secret-key" \
    "AZURE_STORAGE_CONNECTION_STRING=secretref:azure-storage-conn" \
    "AZURE_STORAGE_CONTAINER_NAME=product-images"

echo "=== 6. Backend internal ingress HTTP 허용 ==="
# Azure Container Apps internal ingress는 기본적으로 HTTP → HTTPS 301 redirect
# nginx가 같은 환경 내부에서 HTTP로 백엔드에 접근하려면 allowInsecure가 필요
az containerapp ingress update \
  --name "$BACKEND_APP" \
  --resource-group "$RESOURCE_GROUP" \
  --allow-insecure

# 백엔드 internal FQDN 조회 — 프론트엔드의 BACKEND_URL로 사용
BACKEND_INTERNAL_FQDN=$(az containerapp show \
  --name "$BACKEND_APP" \
  --resource-group "$RESOURCE_GROUP" \
  --query "properties.configuration.ingress.fqdn" \
  --output tsv)

echo "=== 7. Frontend Container App 생성 (외부 ingress, HTTPS 자동) ==="
# --ingress external: 인터넷에서 HTTPS로 접근 가능. Azure가 TLS 인증서를 자동 발급·갱신
# BACKEND_URL: nginx가 백엔드 internal ingress FQDN으로 프록시 (full FQDN 필요)
az containerapp create \
  --name "$FRONTEND_APP" \
  --resource-group "$RESOURCE_GROUP" \
  --environment "$ACA_ENV" \
  --image "mcr.microsoft.com/azuredocs/containerapps-helloworld:latest" \
  --target-port 80 \
  --ingress external \
  --registry-server "$ACR_LOGIN_SERVER" \
  --registry-username "$ACR_USERNAME" \
  --registry-password "$ACR_PASSWORD" \
  --min-replicas 0 \
  --max-replicas 2 \
  --env-vars \
    "BACKEND_URL=http://${BACKEND_INTERNAL_FQDN}"

FRONTEND_URL=$(az containerapp show \
  --name "$FRONTEND_APP" \
  --resource-group "$RESOURCE_GROUP" \
  --query properties.configuration.ingress.fqdn \
  --output tsv)
echo "프론트엔드 URL: https://$FRONTEND_URL"

echo "=== 7. GitHub Actions용 Service Principal 생성 ==="
SUBSCRIPTION_ID=$(az account show --query id --output tsv)
SP_JSON=$(az ad sp create-for-rbac \
  --name "mins-shopping-github-actions" \
  --role Contributor \
  --scopes "/subscriptions/${SUBSCRIPTION_ID}/resourceGroups/${RESOURCE_GROUP}" \
  --sdk-auth)

echo ""
echo "============================================================"
echo "  GitHub Secrets 설정 (Settings → Secrets → Actions)"
echo "============================================================"
echo ""
echo "AZURE_CREDENTIALS:"
echo "$SP_JSON"
echo ""
echo "ACR_LOGIN_SERVER:  $ACR_LOGIN_SERVER"
echo "ACR_USERNAME:      $ACR_USERNAME"
echo "ACR_PASSWORD:      $ACR_PASSWORD"
echo ""
echo "TOSS_SECRET_KEY:               (직접 입력)"
echo "VITE_TOSS_CLIENT_KEY:          (직접 입력)"
echo "AZURE_STORAGE_CONNECTION_STRING: (직접 입력)"
echo ""
echo "GitHub Variables (Settings → Variables → Actions):"
echo "AZURE_RESOURCE_GROUP: $RESOURCE_GROUP"
echo ""
echo "============================================================"
echo "  설정 완료 후 master에 push하면 자동 배포가 시작됩니다."
echo "============================================================"
