# Performance Metrics Comparison Guide

## Objective
Compare Spring Boot vs Quarkus performance using the same banking application migrated with Spring compatibility extensions.

## Key Metrics

### 1. Startup Time
**What:** Time from process start until application is ready to accept HTTP requests

**Why:** Critical for cloud/containerized environments with frequent scaling and deployments

**How to measure:**
- JVM mode: Time until first successful HTTP 200 response
- Native mode (Quarkus only): Same as above

**Tool:** `/usr/bin/time -v` or application logs

**Target:** Record time in milliseconds

---

### 2. Memory Footprint

#### 2.1 RSS (Resident Set Size)
**What:** Actual physical memory used by the process

**Why:** Direct cost in cloud environments (per GB pricing)

**How to measure:**
- At startup (idle state)
- Under load (during stress test)
- After 5 minutes of sustained load

**Tool:** `ps`, `podman stats`, `/usr/bin/time -v`

**Target:** Record in MB

#### 2.2 Heap Usage
**What:** JVM heap memory consumption

**Why:** Understanding memory management efficiency

**How to measure:**
- JVM metrics via Actuator/Micrometer
- Initial heap vs max heap

**Tool:** Spring Boot Actuator `/actuator/metrics/jvm.memory.used`

**Target:** Record heap used and committed in MB

---

### 3. Throughput
**What:** Number of requests processed per second

**Why:** Direct measure of application capacity

**How to measure:**
- Run load test with fixed concurrency (10, 50, 100 threads)
- Duration: 30 seconds per scenario
- Warm-up: 10 seconds before measurement

**Scenarios:**
1. Account creation (POST /accounts)
2. Balance query (GET /accounts/{id}/balance)
3. Money transfer (POST /accounts/transfer)

**Tool:** `ab` (Apache Bench)

**Target:** Record req/sec for each scenario

---

### 4. Response Time (Latency)
**What:** Time to complete a single request

**Why:** User experience and consistency under load

**Percentiles to measure:**
- p50 (median): Typical user experience
- p95: 95% of users experience this or better
- p99: Worst case for most users
- max: Absolute worst case

**How to measure:**
- During throughput tests
- Measure for each scenario

**Tool:** `ab` (built-in latency percentiles)

**Target:** Record in milliseconds

---

### 5. CPU Usage
**What:** CPU consumption by the application process

**Why:** Cost and resource efficiency

**How to measure:**
- At idle (no requests)
- Under load (during stress test)
- Average during 30s sustained load

**Tool:** `podman stats`, `top`, `htop`

**Target:** Record as percentage of 1 CPU core

---

### 6. Binary Size
**What:** Size of the deployable artifact

**Why:** Container image size, deployment time, storage costs

**How to measure:**
- JAR file size (Spring Boot)
- Native executable size (Quarkus native)
- Container image size

**Tool:** `ls -lh`, `podman images`

**Target:** Record in MB

---

## Test Environment Requirements

### Hardware Specs (to be documented)
- CPU model and cores
- RAM available
- Storage type (SSD/HDD)
- Operating system

### Software Versions
- Java/JDK version
- Spring Boot version
- Quarkus version
- PostgreSQL version
- Podman version

### Network
- Localhost tests (eliminate network latency)
- Application and database on same host

---

## Test Procedure

### Phase 1: Startup and Idle Metrics
1. Start PostgreSQL
2. Start application
3. Measure startup time
4. Wait 30 seconds for stabilization
5. Measure idle memory and CPU
6. Record metrics

### Phase 2: Warm-up
1. Run light load for 10 seconds
2. Discard results
3. Purpose: JIT compilation, connection pool initialization

### Phase 3: Load Testing
For each scenario:
1. Run test with 10 concurrent users for 30s
2. Record throughput, latency (p50, p95, p99, max)
3. Record memory and CPU during test
4. Wait 10 seconds between scenarios

### Phase 4: Sustained Load
1. Run mixed workload for 5 minutes
2. Monitor memory growth (check for leaks)
3. Record final memory and CPU usage

### Phase 5: Container Metrics
1. Stop application
2. Measure binary size
3. Build container image
4. Measure image size

---

## Data Collection Format

### CSV Template
```
framework,mode,startup_ms,idle_rss_mb,idle_heap_used_mb,idle_heap_committed_mb,scenario,threads,requests,throughput_rps,latency_mean_ms,latency_p50_ms,latency_p95_ms,latency_p99_ms,latency_max_ms
```

### Example Row
```
spring-boot,jvm,2341,245,180,256,account-creation,10,1000,1250,8.0,8.2,15.3,22.1,145.7
```

---

## Comparison Criteria

### Performance Improvements Expected in Quarkus
1. **Startup time:** 10-50x faster (native mode)
2. **Memory:** 50-70% reduction (native mode)
3. **Throughput:** Similar or slightly better
4. **Latency:** Similar in JVM, potentially better in native
5. **Binary size:** Larger in native, but container image smaller

### Trade-offs to Document
- Build time (native compilation is slower)
- Ecosystem limitations (if any)
- Development experience differences

---

## Tools Installation

### Apache Bench (Load Testing)
```bash
# Fedora/RHEL
sudo dnf install httpd-tools

# Verify
ab -V
```

---

## Reference Articles
- Quarkus for Spring developers: https://quarkus.io/spring/
- Migration guide: https://medium.com/@murungarumungai/spring-boot-to-quarkus-migration-guide-52a7214b76f2
- Real-world experience: https://blog.touret.info/2025/01/22/moving-from-spring-to-quarkus/

