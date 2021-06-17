# spring-boot-microservice

![Build and Test](https://github.com/yohanesgultom/spring-boot-microservice/actions/workflows/test.yml/badge.svg)

Spring Boot REST API microservice with:

* [Kafka](https://kafka.apache.org/) integration
* [MS SQL Server](https://www.microsoft.com/en-us/sql-server) integration through Spring Data
* [Couchbase](https://www.couchbase.com/) multi-buckets integration through Spring Couchbase
* [SpringFox](https://springfox.github.io/springfox/) auto-generated Swagger API documentation

## API documentation

The REST API documentation (Swagger) is automatically generated by [SpringFox](https://springfox.github.io/springfox/) and by default can be accessed from http://localhost:8080/swagger-ui/. The documentation also includes interface to try the APIs interactively.

## Dependencies

Dependencies to run the service:

* Docker 20.x
* Docker-compose 1.26.x

## Testing

> Designed to work with Unix-based OS (Linux/macOS)

Unit testing (only tests controller logics. The rest (repo, producer, listener) are mocked):

```
docker build -t spring-boot-microservice_service:latest service/
docker run -v $HOME/.m2:/root/.m2 -v service:/root/service spring-boot-microservice_service:latest mvn clean test
```

> The `-v $HOME/.m2:/root/.m2` option is to mount local Maven repository to avoid redownloading available packages

Integration testing (end-to-end testing using containers):

```
./integration-test.sh
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
