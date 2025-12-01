package com.redhat.rest.dto;

import java.math.BigDecimal;

public record TransactionData(
        String accountNumber,
        String type,
        BigDecimal amount,
        String description
) implements DataItem {
}

