FROM openjdk:17-slim
EXPOSE 8080 8081
COPY home.jar /service/
COPY config.yml /service
RUN java -jar /service/home.jar db migrate /service/config.yml
CMD java -jar /service/home.jar server /service/config.yml
