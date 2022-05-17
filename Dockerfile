FROM 598045728665.dkr.ecr.ap-south-1.amazonaws.com/graalvm:latest AS graalvm
ADD . /app
WORKDIR /app
RUN ./gradlew nativeCompile
FROM frolvlad/alpine-glibc:alpine-3.12
COPY --from=graalvm /app/build/native/nativeCompile/ares /app/ares
EXPOSE 8083
ENTRYPOINT ["/app/ares"]