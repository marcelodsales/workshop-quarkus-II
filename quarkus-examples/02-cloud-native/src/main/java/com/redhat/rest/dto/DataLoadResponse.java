package com.redhat.rest.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.List;

@RegisterForReflection
public record DataLoadResponse(
        List<AccountData> accounts,
        List<TransactionData> transactions
) {
}

