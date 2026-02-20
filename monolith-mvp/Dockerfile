FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml /workspace/pom.xml
COPY monolith-mvp/pom.xml /workspace/monolith-mvp/pom.xml
COPY monolith-mvp/src /workspace/monolith-mvp/src

RUN mvn -q -f /workspace/monolith-mvp/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /opt/app

COPY --from=build /workspace/monolith-mvp/target/monolith-mvp-0.0.1-SNAPSHOT.jar /opt/app/app.jar

EXPOSE 8099

ENTRYPOINT ["java","-jar","/opt/app/app.jar"]