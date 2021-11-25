FROM openjdk:17-slim
EXPOSE 8080 8081
COPY thermostat.jar /service/
COPY config.yml /service
RUN java -jar /service/thermostat.jar db migrate /service/config.yml
CMD java -jar /service/thermostat.jar server /service/config.yml
