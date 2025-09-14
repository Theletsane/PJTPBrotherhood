#!/bin/bash
set -e
echo "Building the Spring Boot application..."
./mvnw clean package -DskipTests
JAR_FILE=$(find target -type f -name "*.jar" | head -n 1)
if [ -z "$JAR_FILE" ]; then
  echo "Error: No jar file found in target/ directory"
  exit 1
fi
echo "Running $JAR_FILE..."
java -jar "$JAR_FILE"
