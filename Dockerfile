# Stage 1: Build
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app
COPY . .

# Find the file regardless of folder casing and compile
RUN rm -rf out || true && \
    mkdir -p out && \
    javac -d out $(find src -name "MServer.java")

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy the 'multithread' package folder and 'web' assets
COPY --from=build /app/out ./out
COPY --from=build /app/web ./web

EXPOSE 8080
ENV PORT=8080

# 'out' is the classpath. It contains the 'multithread' folder.
CMD ["java", "-cp", "out", "multithread.MServer"]