package com.redhat.rest.dto;

import java.util.List;

public record DataLoadResponse(
        List<AccountData> accounts,
        List<TransactionData> transactions
) {
}

