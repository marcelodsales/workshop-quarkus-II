# Benchmark Execution Guide

## Prerequisites

### Required Tools

```bash
sudo dnf install httpd-tools bc curl
```

### Application Setup

1. Ensure PostgreSQL is running:
```bash
podman-compose up -d postgres
```

2. Build the application:
```bash
cd spring-banking
mvn clean package
cd ..
```

## Quick Start

### Run Full Automated Benchmark

```bash
./scripts/full-benchmark.sh spring-boot jvm
```

This will:
- Build the application
- Start PostgreSQL
- Measure startup time
- Measure idle memory and CPU
- Run load tests for all scenarios (10, 50, 100 threads)
- Generate CSV results in `benchmark-results/`

## Manual Measurements

### 1. Measure Startup Time

```bash
./scripts/measure-startup.sh spring-banking/target/spring-banking-1.0.0.jar
```

Output: Startup time in milliseconds
Result saved to: `/tmp/startup-time.txt`

### 2. Measure Memory Usage

Start the application first:
```bash
java -jar spring-banking/target/spring-banking-1.0.0.jar &
APP_PID=$!
```

Then measure:
```bash
./scripts/measure-memory.sh $APP_PID
```

Output: RSS, Heap Used, Heap Committed (in MB)
Result saved to: `/tmp/memory-metrics.txt`

### 3. Run Load Tests

Available scenarios:
- `account-creation`: POST /accounts
- `balance-query`: GET /accounts/{id}/balance
- `transfer`: POST /accounts/transfer

```bash
./scripts/run-load-test.sh account-creation 10 1000
./scripts/run-load-test.sh balance-query 50 5000
./scripts/run-load-test.sh transfer 100 10000
```

Parameters:
1. Scenario name
2. Number of concurrent threads
3. Total number of requests

## Monitoring During Tests

### Real-time Memory and CPU

```bash
podman stats
```

Or for specific process:
```bash
ps aux | grep java
top -p <PID>
```

### Application Metrics

While application is running:

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/metrics
curl http://localhost:8080/actuator/metrics/jvm.memory.used
curl http://localhost:8080/actuator/metrics/jvm.memory.committed
curl http://localhost:8080/actuator/metrics/process.cpu.usage
curl http://localhost:8080/actuator/prometheus
```

### Binary Size

```bash
ls -lh spring-banking/target/spring-banking-1.0.0.jar
```

## Results Analysis

Results are saved in CSV format in `benchmark-results/` directory.

### CSV Schema

```
framework,mode,startup_ms,idle_rss_mb,idle_heap_used_mb,idle_heap_committed_mb,scenario,threads,duration,throughput_rps,latency_avg_ms,latency_max_ms
```

### Example Analysis

```bash
cat benchmark-results/spring-boot-jvm-20250121-143022.csv
```

## Troubleshooting

### Application Won't Start

Check logs:
```bash
tail -f /tmp/app-startup.log
tail -f /tmp/app-run.log
```

### PostgreSQL Not Available

```bash
podman-compose ps
podman-compose logs postgres
```

Restart if needed:
```bash
podman-compose down
podman-compose up -d postgres
```

### Port Already in Use

Kill existing process:
```bash
lsof -ti:8080 | xargs kill -9
```

### ab Not Finding JSON Payloads

Ensure you run from project root:
```bash
cd /home/masales/RedHat/Workshops/Quarkus_II/spring
./scripts/run-load-test.sh account-creation 10 1000
```

## Next Steps

After collecting Spring Boot metrics:
1. Migrate application to Quarkus with Spring extensions
2. Run same benchmark suite
3. Compare results using CSV files
4. Document performance improvements/regressions

