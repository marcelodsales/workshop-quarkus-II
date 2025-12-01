package com.redhat.rest.dto;

import java.math.BigDecimal;

public record AccountData(
        String accountNumber,
        String ownerId,
        BigDecimal balance
) implements DataItem {
}

