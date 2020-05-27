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

## Getting the generated data
Once the test suite is complete, a message will appear telling you that the data has been generated.
You can find the generated data in `./export_data/` and the file will be named `elucidation-events-{date}.csv` where `date` is the current date/time.

## Current tests built to generate data
* Basic CRUD actions