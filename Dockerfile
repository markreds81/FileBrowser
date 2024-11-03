FROM gradle:8.6-jdk21 AS build

ARG VERSION=latest
WORKDIR /app
COPY . ./

RUN gradle bootJar

#PROD
FROM openjdk:21-jdk-bookworm

ENV SERVICE_PROFILES=prod

RUN apt-get update \
  && apt-get install -y --no-install-recommends curl jq \
  && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/build/libs/*.jar /app.jar

EXPOSE 8021

ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=${SERVICE_PROFILES}", "/app.jar"]

HEALTHCHECK --start-period=30s --interval=30s --timeout=3s --retries=3 \
  CMD curl --silent --fail --request GET http://localhost:8080/actuator/health \
  | jq --exit-status '.status == "UP"' || exit 1
