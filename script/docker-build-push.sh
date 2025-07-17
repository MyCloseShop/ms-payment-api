#!/bin/bash

# Vérifier si un argument de version a été fourni
if [ -z "$1" ]; then
  echo "Usage: $0 <version>"
  exit 1
fi

VERSION=$1
IMAGE_NAME="redsnoww/ms-payment-api"

# Build et push pour AMD
docker buildx build --platform linux/amd64 -t ${IMAGE_NAME}:${VERSION}-amd .
docker push ${IMAGE_NAME}:${VERSION}-amd

# Build et push pour ARM
docker buildx build --platform linux/arm64 -t ${IMAGE_NAME}:${VERSION}-arm .
docker push ${IMAGE_NAME}:${VERSION}-arm

# Créer et push le manifest
docker manifest create ${IMAGE_NAME}:${VERSION} \
  --amend ${IMAGE_NAME}:${VERSION}-amd \
  --amend ${IMAGE_NAME}:${VERSION}-arm

docker manifest push ${IMAGE_NAME}:${VERSION}

echo "Build and push completed for version ${VERSION}"