FROM eclipse-temurin:21-jre

ARG SERVICE_DIR
ARG SERVICE_NAME
WORKDIR /app

COPY ${SERVICE_DIR}/target/${SERVICE_NAME}-0.1.0.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
