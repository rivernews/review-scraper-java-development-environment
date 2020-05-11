FROM maven:3-jdk-11-openj9 AS builder

WORKDIR /tmp/src

COPY shaungc-java-dev/ /tmp/src/

RUN mvn package

# CMD ["sh", "-c", "mvn clean install && mvn exec:java"]

FROM adoptopenjdk/openjdk11:alpine-jre

WORKDIR /tmp/build

COPY --from=builder /tmp/src/target/shaungc-java-dev-1.1.jar .

# setup our entry point

COPY .devcontainer/entry_point.sh /tmp/entry_point.sh

RUN chmod +x /tmp/entry_point.sh

ENTRYPOINT [ "/tmp/entry_point.sh" ]
