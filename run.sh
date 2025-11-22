#!/bin/bash

if [ -f .env ]; then
    echo "Loading environment variables from .env"
    export $(cat .env | grep -v '^#' | xargs)
else
    echo "WARNING: .env file not found. Ensure environment variables are set."
fi

echo "Starting Spring Boot application..."
mvn spring-boot:run

