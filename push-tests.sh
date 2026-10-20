#!/bin/bash

IMAGE_NAME="nbank-tests"
DOCKERHUB_USER="whereisnadya"
TAG="latest"

LOCAL_IMAGE="${IMAGE_NAME}:${TAG}"
REMOTE_IMAGE="${DOCKERHUB_USER}/${IMAGE_NAME}:${TAG}"

set -a
source .env
set +a

echo "$DOCKERHUB_TOKEN" | docker login --username "$DOCKERHUB_USER" --password-stdin

docker tag "$LOCAL_IMAGE" "$REMOTE_IMAGE"

docker push "$REMOTE_IMAGE"

echo ">>> Образ успешно загружен в Docker Hub"
echo ">>> Образ: $REMOTE_IMAGE"
echo ">>> Скачать образ: docker pull $REMOTE_IMAGE"