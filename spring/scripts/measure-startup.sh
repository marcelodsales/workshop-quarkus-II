#!/bin/bash

set -e

APP_JAR="${1:-spring-banking/target/spring-banking-1.0.0.jar}"
TIMEOUT=120

echo "Measuring startup time for: $APP_JAR"

if [ ! -f "$APP_JAR" ]; then
    echo "Error: JAR file not found at $APP_JAR"
    exit 1
fi

START=$(date +%s%3N)

java -jar "$APP_JAR" > /tmp/app-startup.log 2>&1 &
APP_PID=$!

echo "Application PID: $APP_PID"

for i in $(seq 1 $TIMEOUT); do
    if curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health | grep -q "200"; then
        END=$(date +%s%3N)
        STARTUP_TIME=$((END - START))
        echo "Startup completed in ${STARTUP_TIME}ms"
        
        kill $APP_PID 2>/dev/null || true
        wait $APP_PID 2>/dev/null || true
        
        echo "$STARTUP_TIME" > /tmp/startup-time.txt
        exit 0
    fi
    sleep 1
done

echo "Error: Application did not start within ${TIMEOUT} seconds"
kill $APP_PID 2>/dev/null || true
exit 1

