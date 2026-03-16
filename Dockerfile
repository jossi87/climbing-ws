FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

RUN apt-get update && apt-get install -y maven

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY . .
RUN mvn clean package -DskipTests

FROM tomcat:11.0-jdk25-temurin-jammy

RUN apt-get update && \
    apt-get install -y ffmpeg && \
    rm -rf /var/lib/apt/lists/*

RUN rm -rf /usr/local/tomcat/webapps/*

COPY --from=build /app/target/com.buldreinfo.jersey.jaxb.war /usr/local/tomcat/webapps/

EXPOSE 8080
CMD ["catalina.sh", "run"]