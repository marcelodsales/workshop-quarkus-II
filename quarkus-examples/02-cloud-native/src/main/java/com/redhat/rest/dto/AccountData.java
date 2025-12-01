package com.redhat.rest.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.math.BigDecimal;

@RegisterForReflection
public record AccountData(
        String accountNumber,
        String ownerId,
        BigDecimal balance
) implements DataItem {
}

