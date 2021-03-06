

export IMAGE_NAME=shaungc/gd-scraper

if docker build -f ./prod.Dockerfile -t "${IMAGE_NAME}:latest" .. ; then
    echo 'Build success'
else
    echo 'Build failed'
    return
fi

cd ..

export SHORT_COMMIT=$(git rev-parse --short HEAD)

cd .devcontainer

echo "docker tag '${IMAGE_NAME}:latest' '${IMAGE_NAME}:${SHORT_COMMIT}'"
docker tag "${IMAGE_NAME}:latest" "${IMAGE_NAME}:${SHORT_COMMIT}"

echo "docker push '${IMAGE_NAME}:latest'"
docker push "${IMAGE_NAME}:latest"

echo "docker push $IMAGE_NAME:$SHORT_COMMIT"
docker push "${IMAGE_NAME}:${SHORT_COMMIT}"
