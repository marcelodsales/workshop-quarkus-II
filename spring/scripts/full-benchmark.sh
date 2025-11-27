#!/bin/bash

set -e

if [ $# -lt 4 ]; then
    echo "Usage: $0 <framework> <mode> <project-dir> <jar-path> [build-command]"
    echo "Example: $0 spring-boot jvm spring-banking spring-banking/target/spring-banking-1.0.0.jar 'mvn clean package -DskipTests'"
    exit 1
fi

FRAMEWORK="${1}"
MODE="${2}"
PROJECT_DIR="${3}"
JAR_PATH="${4}"
BUILD_CMD="${5:-mvn clean package -DskipTests}"

OUTPUT_DIR="benchmark-results"
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
RESULT_FILE="$OUTPUT_DIR/${FRAMEWORK}-${MODE}-${TIMESTAMP}.csv"

mkdir -p $OUTPUT_DIR

echo "Starting full benchmark for $FRAMEWORK in $MODE mode"
echo "Project: $PROJECT_DIR"
echo "JAR: $JAR_PATH"
echo "Results will be saved to: $RESULT_FILE"

echo "framework,mode,startup_ms,idle_rss_mb,idle_heap_used_mb,idle_heap_committed_mb,scenario,threads,requests,throughput_rps,latency_mean_ms,latency_p50_ms,latency_p95_ms,latency_p99_ms,latency_max_ms" > $RESULT_FILE

echo "Step 1: Building application..."
cd "$PROJECT_DIR"
eval $BUILD_CMD
cd ..

echo "Step 2: Starting PostgreSQL..."
podman-compose up -d postgres-banking
sleep 5

echo "Step 3: Measuring startup time..."
./scripts/measure-startup.sh "$JAR_PATH"
STARTUP_MS=$(cat /tmp/startup-time.txt)
echo "Startup time: ${STARTUP_MS}ms"

echo "Step 4: Starting application for load tests..."
java -jar "$JAR_PATH" > /tmp/app-run.log 2>&1 &
APP_PID=$!
echo "Application PID: $APP_PID"

sleep 10

echo "Step 5: Measuring idle metrics..."
./scripts/measure-memory.sh $APP_PID
IDLE_METRICS=$(cat /tmp/memory-metrics.txt)
echo "Idle metrics: $IDLE_METRICS"

echo "Step 6: Warm-up phase..."
curl -X POST http://localhost:8080/accounts \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer fake-jwt-token" \
    -d '{"accountNumber":"1000001","initialBalance":1000.0,"ownerId":"warmup"}' || true
sleep 5

echo "Step 7: Running load tests..."

for SCENARIO in account-creation balance-query transfer; do
    for THREADS in 10 50 100; do
        REQUESTS=$((THREADS * 100))
        echo "Testing $SCENARIO with $THREADS concurrent requests, total $REQUESTS requests..."
        
        RESULT=$(./scripts/run-load-test.sh $SCENARIO $THREADS $REQUESTS 2>&1 | tee /tmp/ab-result.txt)
        
        THROUGHPUT=$(echo "$RESULT" | grep "Requests per second:" | awk '{print $4}')
        LATENCY_MEAN=$(echo "$RESULT" | grep "Time per request:" | head -1 | awk '{print $4}')
        LATENCY_P50=$(echo "$RESULT" | grep "50%" | awk '{print $2}')
        LATENCY_P95=$(echo "$RESULT" | grep "95%" | awk '{print $2}')
        LATENCY_P99=$(echo "$RESULT" | grep "99%" | awk '{print $2}')
        LATENCY_MAX=$(echo "$RESULT" | grep "100%" | awk '{print $2}')
        
        echo "$FRAMEWORK,$MODE,$STARTUP_MS,$IDLE_METRICS,$SCENARIO,$THREADS,$REQUESTS,$THROUGHPUT,$LATENCY_MEAN,$LATENCY_P50,$LATENCY_P95,$LATENCY_P99,$LATENCY_MAX" >> $RESULT_FILE
        
        sleep 5
    done
done

echo "Step 8: Cleaning up..."
kill $APP_PID 2>/dev/null || true
wait $APP_PID 2>/dev/null || true
podman-compose down

echo "Benchmark completed successfully!"
echo "Results saved to: $RESULT_FILE"

