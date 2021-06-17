# for production deployment

FROM maven:3.6.3-jdk-11-slim as builder
ARG JAR_NAME=rest-api-1.0.0.jar
COPY . /
RUN mvn clean package
RUN java -Djarmode=layertools -jar target/$JAR_NAME extract

FROM openjdk:11-jdk-slim
COPY --from=builder dependencies/ ./
COPY --from=builder snapshot-dependencies/ ./
COPY --from=builder spring-boot-loader/ ./
COPY --from=builder application/ ./
COPY ./*.sh ./
RUN chmod +x ./*.sh
CMD sh -c "java org.springframework.boot.loader.JarLauncher"
