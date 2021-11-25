FROM openjdk:17-slim
EXPOSE 8080 8081
COPY light.jar /service/
COPY config.yml /service
RUN java -jar /service/light.jar db migrate /service/config.yml
CMD java -jar /service/light.jar server /service/config.yml
