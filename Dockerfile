FROM eclipse-temurin:21-jre
EXPOSE 8080

COPY build/libs/app.jar app.jar
CMD ["java", "-jar", "app.jar"]
