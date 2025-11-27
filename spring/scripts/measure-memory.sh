#!/bin/bash

set -e

PID="${1}"

if [ -z "$PID" ]; then
    echo "Usage: $0 <pid>"
    exit 1
fi

if ! ps -p $PID > /dev/null; then
    echo "Error: Process $PID not found"
    exit 1
fi

RSS=$(ps -o rss= -p $PID)
RSS_MB=$((RSS / 1024))

HEAP_INFO=$(jcmd $PID GC.heap_info 2>/dev/null | grep "garbage-first heap")

if [ -z "$HEAP_INFO" ]; then
    echo "Error: Could not retrieve heap info from JVM"
    exit 1
fi

HEAP_TOTAL_KB=$(echo "$HEAP_INFO" | grep -oP 'total \K[0-9]+')
HEAP_USED_KB=$(echo "$HEAP_INFO" | grep -oP 'used \K[0-9]+')

HEAP_USED_MB=$(awk "BEGIN {printf \"%.2f\", $HEAP_USED_KB / 1024}")
HEAP_COMMITTED_MB=$(awk "BEGIN {printf \"%.2f\", $HEAP_TOTAL_KB / 1024}")

echo "RSS: ${RSS_MB}MB"
echo "Heap Used: ${HEAP_USED_MB}MB"
echo "Heap Committed: ${HEAP_COMMITTED_MB}MB"

echo "${RSS_MB},${HEAP_USED_MB},${HEAP_COMMITTED_MB}" > /tmp/memory-metrics.txt
