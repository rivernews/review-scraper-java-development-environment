FROM maven:3-jdk-11-openj9 AS builder

WORKDIR /tmp/src

COPY shaungc-java-dev/ /tmp/src/

RUN mvn package

# CMD ["sh", "-c", "mvn clean install && mvn exec:java"]

FROM adoptopenjdk/openjdk11:alpine-jre

WORKDIR /tmp/build

COPY --from=builder target/shaungc-java-dev-1.1.jar shaungc-java-dev-1.1.jar

CMD [ "java", "-jar", "shaungc-java-dev-1.1.jar" ]
