# Dockerfile backend GMS (Agent 1). Build 2 giai doan: Maven + JDK 17, chay bang JRE 17, khong chay bang root.
# Test da chay o Jenkins (mvn verify) nen build image bo qua test de nhanh hon.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp -DskipTests dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp -DskipTests package \
 && cp target/gym-management-system-*.jar /app.jar

FROM eclipse-temurin:17-jre-jammy
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl tzdata \
 && rm -rf /var/lib/apt/lists/* \
 && useradd --system --create-home --uid 10001 gms \
 && mkdir -p /app/uploads \
 && chown -R gms:gms /app
WORKDIR /app
COPY --from=build --chown=gms:gms /app.jar /app/app.jar
USER gms
ENV TZ=Asia/Ho_Chi_Minh \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
