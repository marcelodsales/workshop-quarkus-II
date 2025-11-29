package com.redhat.rest.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Money transfer request between accounts")
public record TransferRequest(
        @Schema(description = "Source account number", required = true)
        @NotEmpty String fromAccountId,
        
        @Schema(description = "Destination account number", required = true)
        @NotEmpty String toAccountId,
        
        @Schema(description = "Transfer amount", minimum = "0.01", required = true)
        @NotNull @DecimalMin("0.01") BigDecimal amount
) {
}

