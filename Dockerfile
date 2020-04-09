FROM java:8-alpine
MAINTAINER Your Name <you@example.com>

ADD target/n2p-service-0.0.1-SNAPSHOT-standalone.jar /n2p-service/app.jar

EXPOSE 8080

CMD ["java", "-jar", "/n2p-service/app.jar"]
