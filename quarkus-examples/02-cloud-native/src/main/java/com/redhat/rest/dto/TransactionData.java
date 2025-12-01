package com.redhat.rest.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.math.BigDecimal;

@RegisterForReflection
public record TransactionData(
        String accountNumber,
        String type,
        BigDecimal amount,
        String description
) implements DataItem {
}

