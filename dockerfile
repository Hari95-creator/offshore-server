FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/target/offShoreProxyServer.jar offShoreProxyServerDocker.jar
EXPOSE 9000
LABEL maintainer="offshore-proxy-server"
ENTRYPOINT ["java", "-jar", "offShoreProxyServerDocker.jar"]