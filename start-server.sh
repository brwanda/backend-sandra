#!/bin/bash

echo "Starting EaraConnect Backend Server..."
echo

echo "Compiling application..."
mvn compile
if [ $? -ne 0 ]; then
    echo
    echo "❌ Compilation failed! Please fix the errors above."
    exit 1
fi

echo
echo "✅ Compilation successful!"
echo
echo "Starting Spring Boot server on http://localhost:8080"
echo "Press Ctrl+C to stop the server"
echo

mvn spring-boot:run
