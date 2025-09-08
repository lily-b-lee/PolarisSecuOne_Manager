# ---------- Build stage ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src

# (1) 의존성 캐시를 위해 pom만 먼저 복사
COPY PolarisManagerDemo/pom.xml PolarisManagerDemo/pom.xml
RUN mvn -f PolarisManagerDemo/pom.xml -q -DskipTests dependency:go-offline

# (2) 실제 소스 복사 후 빌드
COPY PolarisManagerDemo/src PolarisManagerDemo/src
RUN mvn -f PolarisManagerDemo/pom.xml -DskipTests package \
 && cp PolarisManagerDemo/target/*.jar /tmp/app.jar

# ---------- Runtime stage ----------
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /tmp/app.jar /app/app.jar
ENV SERVER_PORT=8080
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
