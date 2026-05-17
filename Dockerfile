# Stage 1: Build
FROM eclipse-temurin:17-jdk-focal AS build
WORKDIR /app
COPY . .
RUN mkdir -p out && javac -d out src/multithread/MServer.java

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-focal
WORKDIR /app
COPY --from=build /app/out ./out
COPY --from=build /app/web ./web
EXPOSE 8080
ENV PORT=8080
CMD ["java", "-cp", "out", "multithread.MServer"]