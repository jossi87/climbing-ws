# --- Stage 1: Build Stage (Java 25 + Maven) ---
FROM container-registry.oracle.com/java/openjdk:25 AS build
WORKDIR /app

# Oracle Linux uses microdnf
RUN microdnf install -y maven

COPY . .
RUN mvn clean package -DskipTests

# --- Stage 2: Runtime Stage (Tomcat 10 + Java 25) ---
FROM tomcat:10.1-jdk21-openjdk-slim

# Swap the runtime to Java 25
COPY --from=build /usr/java/openjdk-25 /usr/java/openjdk-25
ENV JAVA_HOME=/usr/java/openjdk-25
ENV PATH=$JAVA_HOME/bin:$PATH

RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=build /app/target/*.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080
CMD ["catalina.sh", "run"]