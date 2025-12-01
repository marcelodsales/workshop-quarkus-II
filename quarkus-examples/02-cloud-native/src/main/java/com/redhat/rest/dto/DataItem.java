package com.redhat.rest.dto;

public sealed interface DataItem permits AccountData, TransactionData {
}

