

export IMAGE_NAME=shaungc/gd-scraper

docker build -f ./prod.Dockerfile -t "${IMAGE_NAME}:latest" ..

export SHORT_COMMIT=$(git rev-parse --short HEAD)

echo "docker tag '${IMAGE_NAME}:latest' '${IMAGE_NAME}:${SHORT_COMMIT}'"
docker tag "${IMAGE_NAME}:latest" "${IMAGE_NAME}:${SHORT_COMMIT}"

echo "docker push '${IMAGE_NAME}:latest'"
docker push "${IMAGE_NAME}:latest"

echo "docker push $IMAGE_NAME:$SHORT_COMMIT"
docker push "${IMAGE_NAME}:${SHORT_COMMIT}"
