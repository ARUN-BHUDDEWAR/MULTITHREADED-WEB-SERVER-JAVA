# Stage 1: Build
FROM eclipse-temurin:17-jdk-focal AS build
WORKDIR /app
COPY . .

# Compile all Java files into the 'out' directory
RUN mkdir -p out && javac -d out $(find src -name "*.java")

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-focal
WORKDIR /app

# Copy the compiled classes and the web folder
COPY --from=build /app/out ./out
COPY --from=build /app/web ./web

# Set environment variables
EXPOSE 8080
ENV PORT=8080

# The command must point exactly to the class including the package name
CMD ["java", "-cp", "out", "MulthiThread.MServer"]