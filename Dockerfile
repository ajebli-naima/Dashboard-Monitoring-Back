FROM maven:3.5-jdk-8-alpine AS Build
COPY . .
RUN mvn clean package -DskipTests
RUN ls
COPY  target/backend-0.0.1-SNAPSHOT.jar dashboard.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "dashboard.jar"]

