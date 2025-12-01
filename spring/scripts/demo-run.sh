#!/usr/bin/env bash
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
cd "$ROOT"
require() { if ! command -v "$1" >/dev/null 2>&1; then echo "Missing command: $1" >&2; exit 1; fi }
require podman
require podman-compose
require curl
require ab
require jq
SPRING_PID=""
QUARKUS_PID=""
RUNNING_CONTAINERS=()
cleanup() {
  if [ -n "$SPRING_PID" ] && kill -0 "$SPRING_PID" >/dev/null 2>&1; then kill "$SPRING_PID" >/dev/null 2>&1 || true; wait "$SPRING_PID" >/dev/null 2>&1 || true; fi
  if [ -n "$QUARKUS_PID" ] && kill -0 "$QUARKUS_PID" >/dev/null 2>&1; then kill "$QUARKUS_PID" >/dev/null 2>&1 || true; wait "$QUARKUS_PID" >/dev/null 2>&1 || true; fi
  for c in "${RUNNING_CONTAINERS[@]}"; do podman stop "$c" >/dev/null 2>&1 || true; done
  podman-compose down >/dev/null 2>&1 || true
}
trap cleanup EXIT
log() { printf "\n[%s] %s\n" "$(date +%H:%M:%S)" "$1"; }
wait_for_log() {
  local file=$1 pattern=$2
  for _ in {1..60}; do
    if grep -q "$pattern" "$file"; then return 0; fi
    sleep 1
  done
  echo "Timeout waiting for $pattern" >&2
  exit 1
}
wait_for_http() {
  local url=$1
  for _ in {1..60}; do
    if curl -sf -o /dev/null "$url"; then return 0; fi
    sleep 1
  done
  echo "Failed to reach $url" >&2
  exit 1
}
container_stats() {
  local name=$1
  local mem=$(podman stats --no-stream --format "{{.MemUsage}}" "$name" | awk '{print $1}')
  mem=${mem//[^0-9.]/}
  local cpu=$(podman stats --no-stream --format "{{.CPUPerc}}" "$name")
  cpu=${cpu//[^0-9.]/}
  echo "$mem $cpu"
}
log "Preparing environment"
podman stop spring-banking quarkus-banking >/dev/null 2>&1 || true
podman-compose down >/dev/null 2>&1 || true
podman-compose up -d >/dev/null
sleep 5
log "Building Spring application"
(cd spring-banking && mvn -q clean package -DskipTests)
SPRING_LOG=".demo-spring.log"
rm -f "$SPRING_LOG"
log "Starting Spring Boot (JVM)"
java -jar spring-banking/target/spring-banking-1.0.0.jar >"$SPRING_LOG" 2>&1 &
SPRING_PID=$!
wait_for_log "$SPRING_LOG" "Started BankingApplication in"
SPRING_STARTUP=$(grep "Started BankingApplication in" "$SPRING_LOG" | tail -1 | sed -E 's/.* in ([0-9.]+) seconds.*/\1/')
sleep 10
SPRING_RSS=$(ps -p "$SPRING_PID" -o rss= | awk '{printf "%.2f", $1/1024}')
SPRING_HEAP=$(curl -s http://localhost:8080/actuator/metrics/jvm.memory.used | jq -r '.measurements[0].value' | awk '{printf "%.2f", $1/1048576}')
log "Testing Spring Boot endpoints"
curl -s -X POST http://localhost:8080/accounts -H "Content-Type: application/json" -d '{"accountNumber":"ACC001","ownerId":"user1","initialBalance":1000.00}' >/dev/null
curl -s http://localhost:8080/accounts/1/balance >/dev/null
kill "$SPRING_PID" >/dev/null 2>&1 || true
wait "$SPRING_PID" >/dev/null 2>&1 || true
SPRING_PID=""
log "Building Spring container image"
podman build -q -f spring-banking/src/main/docker/Containerfile.jvm -t spring-banking:latest spring-banking > /dev/null
SPRING_IMAGE_BYTES=$(podman image inspect localhost/spring-banking:latest --format '{{.Size}}')
SPRING_IMAGE_MB=$(awk -v size="$SPRING_IMAGE_BYTES" 'BEGIN {printf "%.2f", size/1048576}')
SPRING_CONTAINER="spring-banking-demo"
podman run -d --rm --name "$SPRING_CONTAINER" -p 8080:8080 -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.containers.internal:5432/banking spring-banking:latest >/dev/null
RUNNING_CONTAINERS+=("$SPRING_CONTAINER")
sleep 10
wait_for_http http://localhost:8080/accounts
read SPRING_MEM_BEFORE SPRING_CPU_BEFORE < <(container_stats "$SPRING_CONTAINER")
SPRING_ACCOUNT_NUMBER="SPRING$(date +%s)"
SPRING_PAYLOAD=$(printf '{"accountNumber":"%s","ownerId":"user1","initialBalance":1000.00}' "$SPRING_ACCOUNT_NUMBER")
SPRING_ACCOUNT_ID=$(curl -s -X POST http://localhost:8080/accounts -H "Content-Type: application/json" -d "$SPRING_PAYLOAD" | jq -r '.id')
SPRING_AB_OUTPUT=$(ab -n 5000 -c 50 http://localhost:8080/accounts/$SPRING_ACCOUNT_ID/balance 2>&1)
SPRING_RPS=$(echo "$SPRING_AB_OUTPUT" | awk '/Requests per second/{print $4}')
SPRING_LATENCY=$(echo "$SPRING_AB_OUTPUT" | awk '/Time per request:/{print $4; exit}')
SPRING_FAILED=$(echo "$SPRING_AB_OUTPUT" | awk '/Failed requests/{print $3}')
read SPRING_MEM_AFTER SPRING_CPU_AFTER < <(container_stats "$SPRING_CONTAINER")
podman stop "$SPRING_CONTAINER" >/dev/null
RUNNING_CONTAINERS=()
log "Building Quarkus application"
(cd quarkus-banking && ./mvnw -q clean package -DskipTests)
QUARKUS_LOG=".demo-quarkus.log"
rm -f "$QUARKUS_LOG"
log "Starting Quarkus (JVM)"
java -jar quarkus-banking/target/quarkus-app/quarkus-run.jar >"$QUARKUS_LOG" 2>&1 &
QUARKUS_PID=$!
wait_for_log "$QUARKUS_LOG" "started in"
QUARKUS_STARTUP=$(grep "started in" "$QUARKUS_LOG" | tail -1 | sed -E 's/.* started in ([0-9.]+)s.*/\1/')
sleep 5
QUARKUS_RSS=$(ps -p "$QUARKUS_PID" -o rss= | awk '{printf "%.2f", $1/1024}')
kill "$QUARKUS_PID" >/dev/null 2>&1 || true
wait "$QUARKUS_PID" >/dev/null 2>&1 || true
QUARKUS_PID=""
log "Building Quarkus container image"
podman build -q -f quarkus-banking/src/main/docker/Dockerfile.jvm -t quarkus-banking:latest quarkus-banking >/dev/null
QUARKUS_IMAGE_BYTES=$(podman image inspect localhost/quarkus-banking:latest --format '{{.Size}}')
QUARKUS_IMAGE_MB=$(awk -v size="$QUARKUS_IMAGE_BYTES" 'BEGIN {printf "%.2f", size/1048576}')
QUARKUS_CONTAINER="quarkus-banking-demo"
podman run -d --rm --name "$QUARKUS_CONTAINER" -p 8080:8080 -e QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://host.containers.internal:5432/banking quarkus-banking:latest >/dev/null
RUNNING_CONTAINERS+=("$QUARKUS_CONTAINER")
sleep 5
wait_for_http http://localhost:8080/accounts
read QUARKUS_MEM_BEFORE QUARKUS_CPU_BEFORE < <(container_stats "$QUARKUS_CONTAINER")
QUARKUS_ACCOUNT_NUMBER="QUARKUS$(date +%s)"
QUARKUS_PAYLOAD=$(printf '{"accountNumber":"%s","ownerId":"user2","initialBalance":2000.00}' "$QUARKUS_ACCOUNT_NUMBER")
QUARKUS_ACCOUNT_ID=$(curl -s -X POST http://localhost:8080/accounts -H "Content-Type: application/json" -d "$QUARKUS_PAYLOAD" | jq -r '.id')
QUARKUS_AB_OUTPUT=$(ab -n 5000 -c 50 http://localhost:8080/accounts/$QUARKUS_ACCOUNT_ID/balance 2>&1)
QUARKUS_RPS=$(echo "$QUARKUS_AB_OUTPUT" | awk '/Requests per second/{print $4}')
QUARKUS_LATENCY=$(echo "$QUARKUS_AB_OUTPUT" | awk '/Time per request:/{print $4; exit}')
QUARKUS_FAILED=$(echo "$QUARKUS_AB_OUTPUT" | awk '/Failed requests/{print $3}')
read QUARKUS_MEM_AFTER QUARKUS_CPU_AFTER < <(container_stats "$QUARKUS_CONTAINER")
podman stop "$QUARKUS_CONTAINER" >/dev/null
RUNNING_CONTAINERS=()
log "Collecting summary"
calc_percent() {
  awk -v spring="$1" -v quarkus="$2" 'BEGIN {
    if (spring == 0) { print "n/a"; exit }
    diff=((spring-quarkus)/spring)*100
    if (diff >= 0) printf "%.1f%% less", diff
    else printf "%.1f%% more", -diff
  }'
}
calc_speed() {
  awk -v spring="$1" -v quarkus="$2" 'BEGIN {
    if (quarkus == 0) { print "n/a"; exit }
    ratio = spring / quarkus
    if (ratio > 1) printf "%.1fx faster", ratio
    else printf "%.1fx slower", 1/ratio
  }'
}
STARTUP_GAIN=$(calc_speed "$SPRING_STARTUP" "$QUARKUS_STARTUP")
MEM_GAIN=$(calc_percent "$SPRING_MEM_BEFORE" "$QUARKUS_MEM_BEFORE")
CPU_GAIN=$(calc_percent "$SPRING_CPU_BEFORE" "$QUARKUS_CPU_BEFORE")
RPS_GAIN=$(calc_percent "$SPRING_RPS" "$QUARKUS_RPS")
LATENCY_GAIN=$(calc_percent "$SPRING_LATENCY" "$QUARKUS_LATENCY")
printf "\nSUMMARY\n"
printf "%-20s %-15s %-15s %-15s\n" "Metric" "Spring" "Quarkus" "Change"
printf "%-20s %-15s %-15s %-15s\n" "Startup (s)" "$SPRING_STARTUP" "$QUARKUS_STARTUP" "$STARTUP_GAIN"
printf "%-20s %-15s %-15s %-15s\n" "RSS (MB)" "$SPRING_RSS" "$QUARKUS_RSS" "n/a"
printf "%-20s %-15s %-15s %-15s\n" "Heap (MB)" "$SPRING_HEAP" "-" "-"
printf "%-20s %-15s %-15s %-15s\n" "Container RAM" "$SPRING_MEM_BEFORE" "$QUARKUS_MEM_BEFORE" "$MEM_GAIN"
printf "%-20s %-15s %-15s %-15s\n" "Container CPU" "$SPRING_CPU_BEFORE" "$QUARKUS_CPU_BEFORE" "$CPU_GAIN"
printf "%-20s %-15s %-15s %-15s\n" "Image (MB)" "$SPRING_IMAGE_MB" "$QUARKUS_IMAGE_MB" "$(calc_percent "$SPRING_IMAGE_MB" "$QUARKUS_IMAGE_MB")"
printf "%-20s %-15s %-15s %-15s\n" "RPS" "$SPRING_RPS" "$QUARKUS_RPS" "$RPS_GAIN"
printf "%-20s %-15s %-15s %-15s\n" "Latency (ms)" "$SPRING_LATENCY" "$QUARKUS_LATENCY" "$LATENCY_GAIN"
printf "%-20s %-15s %-15s %-15s\n" "Failed req" "$SPRING_FAILED" "$QUARKUS_FAILED" "-"
printf "\nDemo completed.\n"
