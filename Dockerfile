FROM openjdk:8-jre-stretch

RUN echo -e "***Deploy JAR***"

ADD target/backend*.jar /opt/dashboard.jar
CMD ["java", "-jar", "/opt/dashboard.jar"]
