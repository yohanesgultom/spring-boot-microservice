FROM maven:3.6.3-jdk-11-slim
USER root
RUN mkdir /root/service
COPY . /root/service
RUN chmod +x /root/service/*.sh
WORKDIR /root/service
EXPOSE 8080
