FROM openjdk:18
WORKDIR /opt
ENV PORT 8080
EXPOSE ${PORT}
COPY ./target/*.jar /opt/app.jar
ENTRYPOINT exec java $JAVA_OPTS  -jar app.jar --server.port=$PORT
