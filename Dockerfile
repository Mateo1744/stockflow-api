FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -DskipTests dependency:go-offline
COPY src src
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy AS runtime
RUN apt-get update \
    && apt-get install --no-install-recommends -y curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system stockflow \
    && useradd --system --gid stockflow --home-dir /app --shell /usr/sbin/nologin stockflow
WORKDIR /app
COPY --from=build /workspace/target/stockflow-api-*.jar app.jar
USER stockflow
EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=3s --start-period=30s --retries=5 \
    CMD curl --fail --silent "http://localhost:${PORT:-8080}/actuator/health" || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
