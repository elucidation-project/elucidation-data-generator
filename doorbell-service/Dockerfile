FROM openjdk:11-jre-slim
EXPOSE 8080 8081
COPY doorbell.jar /service/
COPY config.yml /service
RUN java -jar /service/doorbell.jar db migrate /service/config.yml
CMD java -jar /service/doorbell.jar server /service/config.yml
