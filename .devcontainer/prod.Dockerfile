FROM maven:3-jdk-11-openj9

WORKDIR /tmp/src

COPY shaungc-java-dev/ /tmp/src/

CMD ["sh", "-c", "mvn clean install && mvn exec:java"]
