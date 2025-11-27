#!/bin/bash

set -e

./scripts/full-benchmark.sh \
    spring-boot \
    jvm \
    spring-banking \
    spring-banking/target/spring-banking-1.0.0.jar \
    "mvn clean package -DskipTests"

