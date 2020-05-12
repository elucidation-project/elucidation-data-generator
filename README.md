# Elucidation Data Generator
This project contains a set of services and process for generating testing data for fortitudetec/elucidation>

## How to build test suite
From the root directory run the following command:

`./gradlew clean docker`

This will create the service jars for each service and then create the docker images for each service

## How to run the test suite
From the root directory run the following command:

`docker-compose up`

This will start all the child service containers.

### Items still not complete
1. Need to integrate elucidation client into services
2. Need to add elucidation server to `docker-compose`
3. Need to integrate asynchronous messaging clients into services
4. Need to add ActiveMQ Artemis server to `docker-compose`
5. Need to build and integrate the canary server to run the tests and spit out the collected data