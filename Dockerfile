# syntax=docker/dockerfile:1

# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY . .
# Build only the fat jar (zemberek-full.jar) and the modules it depends on.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -DskipTests -pl all -am package

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre
RUN useradd --create-home --uid 10001 zemberek
WORKDIR /app
COPY --from=build /src/all/target/zemberek-full.jar /app/zemberek-full.jar
USER zemberek

# Let the JVM size its heap from the container memory limit.
# Override with e.g. -e JAVA_TOOL_OPTIONS="-Xmx2g".
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

# gRPC server port
EXPOSE 6789

ENTRYPOINT ["java", "-jar", "/app/zemberek-full.jar"]
CMD ["StartGrpcServer", "--port", "6789"]
