FROM ghcr.io/graalvm/jdk:ol7-java17

 WORKDIR /home/app
 COPY . /home/app/

 EXPOSE 8080

 CMD ["./gradlew", "flywayMigrate", "run"]