FROM openjdk:17-jdk-slim AS build
WORKDIR /app

# Copy full project and compile all Java sources into /app/out
COPY . .
RUN mkdir -p out \
 && find src -name "*.java" -print | xargs javac -d out || true

FROM openjdk:17-jre-slim
WORKDIR /app

# Copy compiled classes and static web assets from build stage
COPY --from=build /app/out ./out
COPY --from=build /app/web ./web

ENV PORT=8080
EXPOSE 8080

# Run the server; it binds ServerSocket to the configured PORT (default 8080)
CMD ["sh", "-c", "java -cp out multithread.MServer"]
