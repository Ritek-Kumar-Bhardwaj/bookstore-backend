# Use an official OpenJDK base image with JDK 21
FROM openjdk:21-jdk

# Set the working directory inside the container
WORKDIR /app

# Copy the built .jar file into the container
COPY target/api-0.0.1-SNAPSHOT.jar /app/api-0.0.1-SNAPSHOT.jar
# Expose the port your Spring Boot app will run on
EXPOSE 8080

# Run the Spring Boot app
ENTRYPOINT ["java", "-jar", "api-0.0.1-SNAPSHOT.jar"]
