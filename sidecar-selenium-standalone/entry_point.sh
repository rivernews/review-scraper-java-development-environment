#!/usr/bin/env bash

# this script is copied from selenium docker images
# this script is placed in `/opt/bin/entry_point.sh` by selenium docker images

#==============================================
# OpenShift or non-sudo environments support
# https://docs.openshift.com/container-platform/3.11/creating_images/guidelines.html#openshift-specific-guidelines
#==============================================

if ! whoami &> /dev/null; then
  if [ -w /etc/passwd ]; then
    echo "${USER_NAME:-default}:x:$(id -u):0:${USER_NAME:-default} user:${HOME}:/sbin/nologin" >> /etc/passwd
  fi
fi

/usr/bin/supervisord --configuration /etc/supervisord.conf &

SUPERVISOR_PID=$!

# creating sidecar selenium server container
# https://medium.com/@cotton_ori/how-to-terminate-a-side-car-container-in-kubernetes-job-2468f435ca99
# https://www.kalc.io/blog/kubernetes-jobs-and-the-sidecar-problem

# wait for scraper job container (kubernetes job) to finish
#
# make sure your pod configs volume:
# volumes: [{ name: 'scraper-job-share', emptyDir:{} }]
#
# then have a volume mount on both container, scaper job container and selenium server container:
# volumeMounts: [{ name: 'scraper-job-share', mountPath: '/tmp/scraper-job-share', readOnly: you can make this `true` on selenium container }]
echo 'Configure termination file for sidecar selenium container'
TERMINATE_FILE=/tmp/scraper-job-share/terminated
(while true; do if [[ -f "${TERMINATE_FILE}" ]]; then kill ${SUPERVISOR_PID}; fi; sleep 3; done) &

function shutdown {
    echo "Trapped SIGTERM/SIGINT/x so shutting down supervisord..."
    kill -s SIGTERM ${SUPERVISOR_PID}
    wait ${SUPERVISOR_PID}
    echo "Shutdown complete"
}

trap shutdown SIGTERM SIGINT

# waits for process to finish
# https://stackoverflow.com/questions/13296863/difference-between-wait-and-sleep
wait ${SUPERVISOR_PID}

if [[ -f "${TERMINATE_FILE}" ]]; then exit 0; echo 'Selenium server shutdown. Job completed with exit 0. Exiting...'; fi
