FROM openjdk:11

EXPOSE 8080
EXPOSE 8081

VOLUME /var/log/idman

ADD config/docker.yml config/docker.yml
ADD target/idman-server*.jar idman-server.jar
ADD startup.sh startup.sh

CMD ./startup.sh
