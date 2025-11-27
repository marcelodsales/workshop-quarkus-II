#!/bin/bash

set -e

SCENARIO="${1:-account-creation}"
THREADS="${2:-10}"
REQUESTS="${3:-1000}"
BASE_URL="http://localhost:8080"
AUTH_HEADER="Authorization: Bearer fake-jwt-token"

echo "Running load test: $SCENARIO with $THREADS concurrent requests, total $REQUESTS requests"

case $SCENARIO in
    "account-creation")
        ab -n $REQUESTS -c $THREADS \
           -T "application/json" \
           -H "$AUTH_HEADER" \
           -p load-tests/account-creation.json \
           $BASE_URL/accounts
        ;;
    "balance-query")
        ab -n $REQUESTS -c $THREADS \
           -H "$AUTH_HEADER" \
           $BASE_URL/accounts/1/balance
        ;;
    "transfer")
        ab -n $REQUESTS -c $THREADS \
           -T "application/json" \
           -H "$AUTH_HEADER" \
           -p load-tests/transfer.json \
           $BASE_URL/accounts/transfer
        ;;
    *)
        echo "Unknown scenario: $SCENARIO"
        echo "Available scenarios: account-creation, balance-query, transfer"
        exit 1
        ;;
esac

