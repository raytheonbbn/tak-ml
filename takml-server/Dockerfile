FROM eclipse-temurin:21

EXPOSE 8234

RUN mkdir /opt/takml_server
WORKDIR /opt/takml_server

COPY run.sh /opt/takml_server/run.sh
COPY takml_server-2.0.jar /opt/takml_server/takml_server-2.0.jar
COPY application.properties /opt/takml_server/application.properties
COPY tak_certs /opt/takml_server/tak_certs

VOLUME /opt/takml_server/example_models
VOLUME /opt/takml_server/logs

RUN apt update -y && \
    apt install -y postgresql openjdk-21-jdk

ENTRYPOINT ["bash", "-c", "/opt/takml_server/run.sh >> /opt/takml_server/logs/takml_server.log 2>&1"]
