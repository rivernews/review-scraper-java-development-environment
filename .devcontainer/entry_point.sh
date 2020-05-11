#!/usr/bin/env sh

echo 'Started entry point script for java scraper container'

ls /tmp

set -e

echo 'true' > /tmp/scraper-job-share/live

# trap 'touch /tmp/scraper-job-share/terminated' EXIT SIGTERM SIGINT

java -jar shaungc-java-dev-1.1.jar

echo 'true' > /tmp/scraper-job-share/terminated

echo 'Finish java scraper container and wrote termination file to volume'
