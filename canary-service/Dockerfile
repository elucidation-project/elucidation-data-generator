FROM openjdk:11-jre-slim
EXPOSE 8080 8081
COPY canary.jar /service/
COPY config.yml /service
CMD java -jar /service/canary.jar server /service/config.yml
