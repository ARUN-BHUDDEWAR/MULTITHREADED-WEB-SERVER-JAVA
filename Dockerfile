# Stage 1: Build
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

# Copy everything
COPY . .

# Use 'find' to locate the MServer.java file so we don't have to worry about folder casing
RUN rm -rf out || true && \
    mkdir -p out && \
    javac -d out $(find src -name "MServer.java")

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=build /app/out ./out
COPY --from=build /app/web ./web

EXPOSE 8080
ENV PORT=8080

# We use lowercase here because the 'javac -d out' command 
# creates the folder structure based on the 'package' name inside the file.
CMD ["java", "-cp", "out", "multithread.MServer"]