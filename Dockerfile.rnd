FROM maven:3.6.3-jdk-8 as BUILD
COPY src /usr/src/app/src
COPY pom.xml /usr/src/app
RUN mvn -f /usr/src/app/pom.xml clean package
 
FROM openliberty/open-liberty:microProfile2-java8-openj9


# COPY --from=BUILD /usr/src/app/target/JavaHelloWorldApp.war /config/dropins/
COPY --from=BUILD /usr/src/app/target/JavaHelloWorldApp.war /config/apps/
COPY ./src/main/wlp/*.xml /config/
RUN configure.sh