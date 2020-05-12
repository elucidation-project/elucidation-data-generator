FROM openjdk:11-jre-slim
EXPOSE 8080 8081
COPY appliance.jar /service/
COPY config.yml /service
RUN java -jar /service/appliance.jar db migrate /service/config.yml
CMD java -jar /service/appliance.jar server /service/config.yml
