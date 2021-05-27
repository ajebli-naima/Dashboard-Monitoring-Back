FROM maven:3.5-jdk-8-alpine AS Build
RUN ls /
WORKDIR /workspace
COPY . .
RUN ls
RUN mvn clean package -DskipTests
RUN ls


FROM openjdk:8-jre-alpine
RUN echo -e "***Deploy JAR***"
WORKDIR /workspace

COPY --from=Build target/backend-0.0.1-SNAPSHOT.jar dashboard.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "dashboard.jar"]

