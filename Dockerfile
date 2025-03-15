FROM openjdk:17-alpine
VOLUME /tmp
COPY target/leader-election-k8s-1.0.0-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]

# docker build -t leader-election-k8s:1.0.0-SNAPSHOT .