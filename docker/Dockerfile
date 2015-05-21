FROM java:8

MAINTAINER cluda

EXPOSE 8888

WORKDIR .

ADD streams.jar streams.jar

ENTRYPOINT ["java", "-jar", "streams.jar"]