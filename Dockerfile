# --- Stage 1: Build Stage (Java 25 + Maven) ---
FROM openjdk:25-slim AS build
RUN apt-get update && apt-get install -y maven
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# --- Stage 2: Runtime Stage (Tomcat 10 + Java 25) ---
FROM tomcat:10.1-jdk21-slim

# Replace the default JDK 21 with JDK 25 from the build stage
COPY --from=build /usr/java/openjdk-25 /usr/java/openjdk-25
ENV JAVA_HOME=/usr/java/openjdk-25
ENV PATH=$JAVA_HOME/bin:$PATH

# Standard Tomcat cleanup and deployment
RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=build /app/target/*.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080
CMD ["catalina.sh", "run"]