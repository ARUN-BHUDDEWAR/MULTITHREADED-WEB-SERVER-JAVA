# Stage 1: Build
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

# Copy project files
COPY . .

# Compile Java files into 'out' folder using a robust, null-safe xargs invocation
RUN mkdir -p out \
 && find src -name "*.java" -print0 | xargs -0 javac -d out

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy compiled classes and web assets from build stage
COPY --from=build /app/out ./out
COPY --from=build /app/web ./web

# Debug: list compiled classes and web files so Render logs show what's present
RUN echo "== /app/out content ==" && ls -la /app/out || true
RUN echo "== /app/out/multithread ==" && ls -la /app/out/multithread || true
RUN echo "== /app/web content ==" && ls -la /app/web || true

EXPOSE 8080
ENV PORT=8080

# Run the server with the correct package name (lowercase `multithread`)
CMD ["sh", "-c", "java -cp out multithread.MServer"]