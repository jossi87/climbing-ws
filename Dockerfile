FROM container-registry.oracle.com/java/openjdk:25 AS build
WORKDIR /app

RUN dnf install -y maven

COPY . .
RUN mvn clean package -DskipTests

FROM tomcat:10.1-jdk21-openjdk-slim

COPY --from=build /usr/java/jdk-25 /usr/java/jdk-25
ENV JAVA_HOME=/usr/java/jdk-25
ENV PATH=$JAVA_HOME/bin:$PATH

RUN rm -rf /usr/local/tomcat/webapps/*

COPY --from=build /app/target/com.buldreinfo.jersey.jaxb.war /usr/local/tomcat/webapps/

EXPOSE 8080
CMD ["catalina.sh", "run"]