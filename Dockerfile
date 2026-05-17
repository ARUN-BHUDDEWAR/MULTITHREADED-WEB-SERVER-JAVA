# Stage 1: Build
FROM eclipse-temurin:17-jdk-focal AS build
WORKDIR /app

# Copy everything
COPY . .

# Compile Java files into 'out' folder
RUN mkdir -p out && javac -d out $(find src -name "*.java")

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-focal
WORKDIR /app

# Copy compiled classes from build stage
COPY --from=build /app/out ./out

# Copy the web folder (HTML/CSS) from build stage
COPY --from=build /app/web ./web

# Debug step: This will list files in the Render logs so you can verify CSS is there
RUN ls -R /app/web

# Set Production Environment
EXPOSE 8080
ENV PORT=8080

# Execute MServer from the MulthiThread package
CMD ["java", "-cp", "out", "MulthiThread.MServer"]