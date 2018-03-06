# A JDK 9 with Debian slim
FROM openjdk:9.0.1-jre-slim

# Set up env variables
ENV JAVA_OPTS=""

ARG JAR_FILE
ADD ${JAR_FILE} app.jar

CMD java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -jar /app.jar