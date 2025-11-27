#!/bin/bash

set -e

MODE="${1:-jvm}"

if [ "$MODE" = "jvm" ]; then
    JAR_PATH="quarkus-banking/target/quarkus-app/quarkus-run.jar"
    BUILD_CMD="mvn clean package -DskipTests"
elif [ "$MODE" = "native" ]; then
    JAR_PATH="quarkus-banking/target/quarkus-banking-1.0.0-runner"
    BUILD_CMD="mvn clean package -DskipTests -Pnative"
else
    echo "Invalid mode: $MODE. Use 'jvm' or 'native'"
    exit 1
fi

./scripts/full-benchmark.sh \
    quarkus \
    "$MODE" \
    quarkus-banking \
    "$JAR_PATH" \
    "$BUILD_CMD"

