FROM maven:3.5-jdk-8-alpine AS Build

WORKDIR /build
COPY . /build/
RUN mvn clean package -DskipTests

FROM openjdk:8-jre-alpine
WORKDIR /app
COPY --from=Build  /build/backend-0.0.1-SNAPSHOT.jar /app/dashboard.jar
EXPOSE 8080

CMD ["java", "-jar", "dashboard.jar"]

