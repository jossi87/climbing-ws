FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:25-jre-jammy
WORKDIR /app
RUN apt-get update && apt-get install -y ffmpeg && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/climbing-ws.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]