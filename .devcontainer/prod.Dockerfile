FROM maven:3-jdk-11-openj9

WORKDIR /tmp/src

COPY shaungc-java-dev/ /tmp/src/

RUN mvn clean install

CMD ["sh", "-c", "mvn exec:java"]
