# ---------- Build stage ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY . .
# 모듈이 PolarisManagerDemo인 경우 그 모듈만 빌드
RUN mvn -f PolarisManagerDemo/pom.xml -DskipTests package \
 && JAR=$(ls PolarisManagerDemo/target/*.jar | head -n 1) \
 && cp "$JAR" /tmp/app.jar

# ---------- Runtime stage ----------
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /tmp/app.jar /app/app.jar
ENV SERVER_PORT=8080
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
