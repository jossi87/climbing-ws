# Build stage
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Install Maven
RUN apt-get update && apt-get install -y curl && \
    curl -fsSL https://archive.apache.org/dist/maven/maven-3/3.9.16/binaries/apache-maven-3.9.16-bin.tar.gz | tar -xzC /opt && \
    ln -s /opt/apache-maven-3.9.16/bin/mvn /usr/bin/mvn

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY . .
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:25-jre-jammy
WORKDIR /app
RUN apt-get update && apt-get install -y ffmpeg && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/climbing-ws.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]