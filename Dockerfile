# Build Stage
FROM eclipse-temurin:17-jdk-focal AS build
WORKDIR /app
COPY . .
# This finds MServer.java even if it's in src/ or src/multithread/
RUN mkdir -p out && javac -d out $(find src -name "MServer.java")

# Run Stage
FROM eclipse-temurin:17-jre-focal
WORKDIR /app
COPY --from=build /app/out ./out
COPY --from=build /app/web ./web
EXPOSE 8080
ENV PORT=8080
# If your package is 'multithread', keep the prefix. If no package, use "MServer"
CMD ["java", "-cp", "out", "multithread.MServer"]