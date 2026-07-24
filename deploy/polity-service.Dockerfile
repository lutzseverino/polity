FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S polity && adduser -S -G polity polity

WORKDIR /app
COPY services/polity/target/polity-*.jar polity.jar

ENV POLITY_PORT=8084
EXPOSE 8084

USER polity

HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD wget --quiet --spider "http://127.0.0.1:${POLITY_PORT}/actuator/health/readiness" || exit 1

ENTRYPOINT ["java", "-jar", "/app/polity.jar"]
