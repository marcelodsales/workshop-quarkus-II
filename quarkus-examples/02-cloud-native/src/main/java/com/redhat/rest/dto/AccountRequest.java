package com.redhat.rest.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Account creation request")
public record AccountRequest(
        @Schema(description = "Unique account number", required = true)
        @NotBlank String accountNumber,
        
        @Schema(description = "Account owner identifier", required = true)
        @NotBlank String ownerId,
        
        @Schema(description = "Initial account balance", minimum = "0.0", required = true)
        @DecimalMin("0.0") BigDecimal initialBalance
) {

}

