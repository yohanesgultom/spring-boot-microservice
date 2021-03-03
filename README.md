# spring-boot-microservice

![Build and Test](https://github.com/yohanesgultom/spring-boot-microservice/actions/workflows/test.yml/badge.svg)

Spring Boot REST API microservice integrated with:

* [Kafka](https://kafka.apache.org/): communication with other service
* [MS SQL Server](https://www.microsoft.com/en-us/sql-server): relational database
* [Couchbase](https://www.couchbase.com/): non-relational database

> Designed to work with Unix-based OS (Linux/macOS). Tested only on Ubuntu 20 LTS. Some adjustments may be needed to make it work on Windows

## API documentation

The REST API documentation is automatically generated by [SpringFox](https://springfox.github.io/springfox/) and by default can be accessed from http://localhost:8080/swagger-ui/. The documentation also includes interface to try the APIs interactively.

## Dependencies

Dependencies to run the service:

* Docker 20.x
* Docker-compose 1.26.x

## Testing

Unit testing (only tests controller logics. The rest (repo, producer, listener) are mocked):

```
docker build -t spring-boot-microservice_service:latest service/
docker run -v $HOME/.m2:/root/.m2 -v service:/root/service spring-boot-microservice_service:latest mvn clean test
```

> The `-v $HOME/.m2:/root/.m2` option is to mount local Maven repository to avoid redownloading available packages

Integration testing (end-to-end testing using containers):

```
docker-compose run service /bin/sh -c "./wait-for-it.sh mssql:1433 -s -t 90 -- mvn clean test -Dtests=integration"; docker-compose down -v;
```

or in Bash console:

```
./integration-test.sh
```

## Running

Steps to run the service:

* Make sure all `ports` mentioned in docker-compose.yml are free
* Build and run containers: `docker-compose up -d`
* Open the Swagger API doc at http://localhost:8080/swagger-ui/ to test it interactively
* Shutdown containers: `docker-compose down -v`
