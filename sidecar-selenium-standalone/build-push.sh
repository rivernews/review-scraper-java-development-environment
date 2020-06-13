# make sure every command succeed
# https://stackoverflow.com/a/19622569/9814131

if [ "$1" = "debug" ]; then
    export IMAGE_NAME=shaungc/gd-selenium-standalone-debug
    DOCKERFILE='./debug.Dockerfile'
else
    export IMAGE_NAME=shaungc/gd-selenium-standalone
    DOCKERFILE='./Dockerfile'
fi

if docker build -f ${DOCKERFILE} -t "${IMAGE_NAME}:latest" . ; then
    echo 'Build success'
else
    echo 'Build failed'
    return
fi

set -e

export SHORT_COMMIT=$(openssl rand -hex 12)

echo "docker tag '${IMAGE_NAME}:latest' '${IMAGE_NAME}:${SHORT_COMMIT}'"
docker tag "${IMAGE_NAME}:latest" "${IMAGE_NAME}:${SHORT_COMMIT}"

echo "docker push '${IMAGE_NAME}:latest'"
docker push "${IMAGE_NAME}:latest"

echo "docker push $IMAGE_NAME:$SHORT_COMMIT"
docker push "${IMAGE_NAME}:${SHORT_COMMIT}"
