# Stage 1: Build
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

# Copy everything
COPY . .

# Clean and Compile - using lowercase 'multithread' to match standard Java packages
RUN rm -rf out || true && \
    mkdir -p out && \
    javac -d out src/multithread/MServer.java

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-focal
WORKDIR /app

# Copy the compiled output and the web directory
# Placing them both in the root (/app) ensures relative paths work
COPY --from=build /app/out ./out
COPY --from=build /app/web ./web

# Set Environment
EXPOSE 8080
ENV PORT=8080

# Run directly without 'sh' to ensure signal handling and path consistency
# Classpath 'out' means it looks inside /app/out for multithread/MServer.class
CMD ["java", "-cp", "out", "multithread.MServer"]