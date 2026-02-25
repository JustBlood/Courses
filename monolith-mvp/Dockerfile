FROM eclipse-temurin:21-jre

COPY ./monolith-mvp/target/monolith-mvp-0.0.1-SNAPSHOT.jar /opt/app/app.jar

EXPOSE 8099

ENTRYPOINT ["java","-jar","/opt/app/app.jar"]
