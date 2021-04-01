FROM maven:3.6.1-jdk-8-slim AS build
RUN mkdir -p /workspace
WORKDIR /workspace
COPY pom.xml /workspace
COPY src /workspace/src
RUN mvn -f pom.xml clean package -DskipTests


FROM openjdk:8-jre-stretch
RUN echo -e "***Deploy JAR***"
ADD target/backend*.jar /opt/dashboard.jar
CMD ["java", "-jar", "/opt/dashboard.jar"]
