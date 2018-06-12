FROM java:8-alpine
MAINTAINER Your Name <you@example.com>

ADD target/uberjar/running.jar /running/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/running/app.jar"]
