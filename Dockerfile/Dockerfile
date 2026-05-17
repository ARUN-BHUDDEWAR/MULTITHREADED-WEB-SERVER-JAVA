FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app
COPY . /app
RUN javac -d out src/MulthiThread/MServer.java

FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app
COPY --from=build /app/out /app/out
COPY web /app/web
EXPOSE 8080
ENV PORT=8080
CMD ["java", "-cp", "out", "multithread.MServer"]
