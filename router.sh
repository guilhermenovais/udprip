#!/bin/bash

# This script launches a router instance

if [ "$#" -lt 2 ] || [ "$#" -gt 3 ]; then
    echo "Usage: $0 <address> <period> [startup]"
    exit 1
fi

ADDRESS=$1
PERIOD=$2
STARTUP=$3

# Check if the JAR file exists
JAR_FILE="target/udprip-1.0-SNAPSHOT-jar-with-dependencies.jar"

if [ ! -f "$JAR_FILE" ]; then
    echo "JAR file not found. Building the project..."
    mvn clean package

    if [ $? -ne 0 ]; then
        echo "Build failed. Please fix any errors and try again."
        exit 1
    fi
fi

# Launch the router
if [ -z "$STARTUP" ]; then
    java -jar "$JAR_FILE" "$ADDRESS" "$PERIOD"
else
    java -jar "$JAR_FILE" "$ADDRESS" "$PERIOD" "$STARTUP"
fi
