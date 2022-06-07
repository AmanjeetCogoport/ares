FROM ghcr.io/graalvm/jdk:ol7-java17 as graalvm
EXPOSE 8086
COPY /api/build/libs/*.jar app.jar
ENV JAVA_OPTS=""
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app.jar" ]