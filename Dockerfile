FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

RUN apt-get update && apt-get install -y curl && \
    curl -fsSL https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.tar.gz | tar -xzC /opt && \
    ln -s /opt/apache-maven-3.9.9/bin/mvn /usr/bin/mvn

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY . .
RUN mvn clean package -DskipTests

FROM tomcat:11.0-jdk25-temurin-jammy

RUN apt-get update && \
    apt-get install -y --no-install-recommends ffmpeg && \
    rm -rf /var/lib/apt/lists/*

RUN rm -rf /usr/local/tomcat/webapps/*

COPY --from=build /app/target/com.buldreinfo.jersey.jaxb.war /usr/local/tomcat/webapps/

EXPOSE 8080
CMD ["catalina.sh", "run"]