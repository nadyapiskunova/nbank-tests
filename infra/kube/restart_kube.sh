#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Проверяем локальный файл с секретами
if [ ! -f "$PROJECT_DIR/.env.kube" ]; then
  echo "ERROR: .env.kube not found"
  exit 1
fi

# Загружаем переменные окружения
set -a
source "$PROJECT_DIR/.env.kube"
set +a

# Проверяем, что секреты заполнены
if [ -z "$POSTGRES_USER" ] || [ -z "$POSTGRES_PASSWORD" ]; then
  echo "ERROR: POSTGRES_USER or POSTGRES_PASSWORD is empty"
  exit 1
fi

echo "=== Start Minikube ==="
minikube start --driver=docker

echo "=== Remove previous Helm release if it exists ==="
helm uninstall nbank 2>/dev/null || true

echo "=== Wait for old pods to terminate ==="
kubectl wait --for=delete pod \
  -l 'app in (backend,frontend,postgres,selenoid,selenoid-ui)' \
  --timeout=60s 2>/dev/null || true

echo "=== Create PostgreSQL Secret ==="
kubectl delete secret postgres-secret --ignore-not-found

kubectl create secret generic postgres-secret \
  --from-literal=username="$POSTGRES_USER" \
  --from-literal=password="$POSTGRES_PASSWORD"

echo "=== Install NBank Helm chart ==="
helm install nbank "$SCRIPT_DIR/nbank-chart"

echo "=== Wait for deployments ==="
kubectl rollout status deployment/postgres --timeout=120s
kubectl rollout status deployment/backend --timeout=120s
kubectl rollout status deployment/frontend --timeout=120s
kubectl rollout status deployment/selenoid --timeout=120s
kubectl rollout status deployment/selenoid-ui --timeout=120s

echo "=== Helm releases ==="
helm list

echo "=== Kubernetes pods ==="
kubectl get pods

echo "=== Kubernetes services ==="
kubectl get svc

echo "=== ConfigMaps ==="
kubectl get configmap

echo "=== Secrets ==="
kubectl get secret

echo "=== Backend logs ==="
kubectl logs deployment/backend --tail=30

echo "=== Frontend logs ==="
kubectl logs deployment/frontend --tail=30

echo "=== PostgreSQL logs ==="
kubectl logs deployment/postgres --tail=30

echo "=== Selenoid logs ==="
kubectl logs deployment/selenoid --tail=30

echo "=== Selenoid UI logs ==="
kubectl logs deployment/selenoid-ui --tail=30

echo "=== Start port-forward ==="

kubectl port-forward svc/backend 4111:4111 \
  > /tmp/nbank-backend-port-forward.log 2>&1 &
BACKEND_PID=$!

kubectl port-forward svc/frontend 3000:80 \
  > /tmp/nbank-frontend-port-forward.log 2>&1 &
FRONTEND_PID=$!

kubectl port-forward svc/selenoid 4444:4444 \
  > /tmp/nbank-selenoid-port-forward.log 2>&1 &
SELENOID_PID=$!

kubectl port-forward svc/selenoid-ui 8080:8080 \
  > /tmp/nbank-selenoid-ui-port-forward.log 2>&1 &
SELENOID_UI_PID=$!

sleep 3

echo ""
echo "=== Application is available ==="
echo "Backend:     http://localhost:4111"
echo "Frontend:    http://localhost:3000"
echo "Selenoid:    http://localhost:4444"
echo "Selenoid UI: http://localhost:8080"
echo ""
echo "Port-forward processes:"
echo "Backend PID:     $BACKEND_PID"
echo "Frontend PID:    $FRONTEND_PID"
echo "Selenoid PID:    $SELENOID_PID"
echo "Selenoid UI PID: $SELENOID_UI_PID"
