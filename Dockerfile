FROM maven:3.5-jdk-8-alpine AS Build
WORKDIR /app
COPY . .
RUN mvn install -DskipTests


FROM openjdk:8-jre-alpine
RUN echo -e "***Deploy JAR***"
COPY --from=Build /app/target/backend*.jar /dashboard.jar
CMD ["java", "-jar", "/dashboard.jar"]
