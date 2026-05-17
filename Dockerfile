# Stage 1: Build
FROM eclipse-temurin:17-jdk-focal AS build
WORKDIR /app
COPY . .

# This command finds ANY .java file in src and compiles it to 'out'
RUN mkdir -p out && javac -d out $(find src -name "*.java")

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-focal
WORKDIR /app

# Copy the compiled code and web files
COPY --from=build /app/out ./out
COPY --from=build /app/web ./web

EXPOSE 8080
ENV PORT=8080

# Check if the package exists or not
# If you have 'package multithread;' at the top of MServer.java, use the first one.
# If you don't have a package line, use the second one.
CMD ["java", "-cp", "out", "multithread.MServer"]
# CMD ["java", "-cp", "out", "MServer"]