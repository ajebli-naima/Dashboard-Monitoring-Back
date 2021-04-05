FROM maven:3.5-jdk-8-alpine AS Build
WORKDIR /app
COPY . .
RUN mvn install -DskipTests


FROM openjdk:8-jre-alpine
RUN echo -e "***Deploy JAR***"
WORKDIR /app
COPY --from=Build target/backend-0.0.1-SNAPSHOT.jar /dashboard.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/dashboard.jar"]

