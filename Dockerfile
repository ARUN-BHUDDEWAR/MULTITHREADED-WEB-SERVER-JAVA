# STAGE 1: Build
FROM eclipse-temurin:17-jdk-focal AS build
WORKDIR /app
COPY . .

# Compile and list files to debug if it fails
RUN mkdir -p out && javac -d out $(find src -name "MServer.java")

# STAGE 2: Run
FROM eclipse-temurin:17-jre-focal
WORKDIR /app

# Copy compiled classes and the web folder
COPY --from=build /app/out ./out
COPY --from=build /app/web ./web

# Render needs the app to listen on 0.0.0.0
ENV PORT=8080
EXPOSE 8080

# Execute
CMD ["java", "-cp", "out", "multithread.MServer"]