FROM selenium/standalone-chrome-debug:3.141.59-20200525
# Dockerfile at
# https://github.com/SeleniumHQ/docker-selenium/blob/master/StandaloneChrome/Dockerfile
#
# Instructions at
# https://github.com/SeleniumHQ/docker-selenium

COPY entry_point.sh /opt/bin/entry_point.sh

# configure script permission so selenium user can run it
# https://serverfault.com/a/967604
USER root

RUN chmod +x /opt/bin/entry_point.sh

# change user back to selenium container's settings
USER seluser
