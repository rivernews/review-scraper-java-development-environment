#!/usr/bin/env sh

IMAGE_NAME=shaungc/gd-scraper

docker build -f ./prod.Dockerfile -t $IMAGE_NAME:latest ..
# echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
# export SHORT_TRAVIS_COMMIT=$(git rev-parse --short ${TRAVIS_COMMIT})
export SHORT_COMMIT=$(git rev-parse --short HEAD)
docker tag $IMAGE_NAME:latest $IMAGE_NAME:$SHORT_COMMIT
docker push $IMAGE_NAME:latest
docker push $IMAGE_NAME:$SHORT_COMMIT
