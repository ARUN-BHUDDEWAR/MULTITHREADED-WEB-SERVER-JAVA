# Stage 1: Build
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

# Copy project files
COPY . .

# Compile Java files into 'out' folder using a robust, null-safe xargs invocation
RUN mkdir -p out \
 && find src -name "*.java" -print0 | xargs -0 javac -d out

# Package compiled classes into a runnable JAR to avoid classpath issues
RUN jar --create --file app.jar -C out .

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy compiled classes and web assets from build stage
COPY --from=build /app/out ./out
COPY --from=build /app/web ./web
COPY --from=build /app/app.jar ./app.jar

# Debug: list compiled classes and web files so Render logs show what's present
RUN echo "== /app/out content ==" && ls -la /app/out || true
RUN echo "== /app/out/multithread ==" && ls -la /app/out/multithread || true
RUN echo "== /app/web content ==" && ls -la /app/web || true

EXPOSE 8080
ENV PORT=8080

# At container start, print out compiled classes for debugging, then exec the server
CMD ["sh", "-c", "echo '=== runtime: /app/out ===' && ls -la /app/out || true && echo '=== runtime: /app/out/multithread ===' && ls -la /app/out/multithread || true && echo '=== runtime: app.jar ===' && ls -la /app/app.jar || true && exec java -jar app.jar"]