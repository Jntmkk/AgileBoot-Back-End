FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

COPY agileboot-admin/target/agileboot-admin-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=basic,dev"]
