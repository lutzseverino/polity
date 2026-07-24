FROM eclipse-temurin:21-jre-alpine@sha256:3f08b13888f595cc49edabea7250ba69499ba25602b267da591720769400e08c

RUN addgroup -S polity && adduser -S -G polity polity

WORKDIR /app
COPY services/polity/target/polity-*.jar polity.jar

ENV POLITY_PORT=8084
EXPOSE 8084

USER polity

HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD wget --quiet --spider "http://127.0.0.1:${POLITY_PORT}/actuator/health/readiness" || exit 1

ENTRYPOINT ["java", "-jar", "/app/polity.jar"]
