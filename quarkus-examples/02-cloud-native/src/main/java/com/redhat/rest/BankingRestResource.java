package com.redhat.rest;

import com.redhat.config.BankingConfig;
import com.redhat.model.Account;
import com.redhat.rest.dto.AccountRequest;
import com.redhat.rest.dto.TransactionResponse;
import com.redhat.rest.dto.TransferRequest;
import com.redhat.service.BankingService;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import io.smallrye.mutiny.Multi;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.rest.dto.AccountData;
import com.redhat.rest.dto.DataItem;
import com.redhat.rest.dto.DataLoadResponse;
import com.redhat.rest.dto.TransactionData;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Banking API", description = "Account and transaction management operations")
@Slf4j
public class BankingRestResource {

    @Inject
    protected BankingService bankingService;

    @Inject
    BankingConfig bankingConfig;

    @POST
    @Path("/accounts")
    @Operation(summary = "Create account", description = "Creates a new bank account")
    @APIResponse(responseCode = "201", description = "Account created successfully", content = @Content(schema = @Schema(implementation = Account.class), examples = {@ExampleObject(name = "CreatedAccount", summary = "Example of a created account", value = "{\"accountNumber\":\"ACC001\",\"balance\":1000.00,\"ownerId\":\"OWNER123\"}")}))
    @APIResponse(responseCode = "400", description = "Invalid request")
    @APIResponse(responseCode = "409", description = "Account already exists")
    public Response createAccount(@Valid @RequestBody(description = "Account creation request", required = true, content = @Content(schema = @Schema(implementation = AccountRequest.class), examples = {@ExampleObject(name = "ValidRequest", summary = "Example of a valid account creation request", value = "{\"accountNumber\":\"ACC001\",\"ownerId\":\"OWNER123\",\"initialBalance\":1000.00}"), @ExampleObject(name = "InvalidRequest", summary = "Example of an invalid request (negative balance)", value = "{\"accountNumber\":\"ACC002\",\"ownerId\":\"OWNER456\",\"initialBalance\":-50.00}")})) AccountRequest request) {
        Account account = bankingService.createAccount(request.accountNumber(), request.ownerId(), request.initialBalance());
        return Response.status(Response.Status.CREATED).entity(account).build();
    }

    @GET
    @Path("/accounts")
    @Operation(summary = "List accounts", description = "Retrieves all bank accounts")
    @APIResponse(responseCode = "200", description = "Accounts retrieved successfully",
            content = @Content(schema = @Schema(implementation = Account.class),
                    examples = {@ExampleObject(name = "AccountList", summary = "List of accounts",
                            value = "[{\"accountNumber\":\"ACC001\",\"balance\":1000.00,\"ownerId\":\"OWNER123\"},{\"accountNumber\":\"ACC002\",\"balance\":500.00,\"ownerId\":\"OWNER456\"}]")}))
    public Response listAccounts() {
        return Response.status(Response.Status.OK).entity(bankingService.getAllAccounts()).build();
    }

    @POST
    @Path("/accounts/{accountNumber}/deposit")
    @Operation(summary = "Deposit money", description = "Deposits money into an account")
    @APIResponse(responseCode = "200", description = "Deposit successful",
            content = @Content(schema = @Schema(implementation = Account.class),
                    examples = {@ExampleObject(name = "DepositSuccess", summary = "Successful deposit",
                            value = "{\"accountNumber\":\"ACC001\",\"balance\":1250.00,\"ownerId\":\"OWNER123\"}")}))
    @APIResponse(responseCode = "400", description = "Invalid amount")
    @APIResponse(responseCode = "404", description = "Account not found")
    public Response deposit(
            @Parameter(description = "Account number", required = true, example = "ACC001")
            @PathParam("accountNumber") String accountNumber,
            @Valid @RequestBody(description = "Deposit amount", required = true,
                    content = @Content(schema = @Schema(implementation = BigDecimal.class),
                            examples = {@ExampleObject(name = "DepositAmount", summary = "Amount to deposit", value = "250.00")}))
            @DecimalMin("0.01") BigDecimal amount) {
        Account account = bankingService.deposit(accountNumber, amount);
        return Response.ok(account).build();
    }

    @POST
    @Path("/accounts/{accountNumber}/withdraw")
    @Operation(summary = "Withdraw money", description = "Withdraws money from an account")
    @APIResponse(responseCode = "200", description = "Withdrawal successful",
            content = @Content(schema = @Schema(implementation = Account.class),
                    examples = {@ExampleObject(name = "WithdrawSuccess", summary = "Successful withdrawal",
                            value = "{\"accountNumber\":\"ACC001\",\"balance\":900.00,\"ownerId\":\"OWNER123\"}")}))
    @APIResponse(responseCode = "400", description = "Invalid amount or insufficient balance")
    @APIResponse(responseCode = "404", description = "Account not found")
    public Response withdraw(
            @Parameter(description = "Account number", required = true, example = "ACC001")
            @PathParam("accountNumber") String accountNumber,
            @Valid @RequestBody(description = "Withdrawal amount", required = true,
                    content = @Content(schema = @Schema(implementation = BigDecimal.class),
                            examples = {@ExampleObject(name = "WithdrawAmount", summary = "Amount to withdraw", value = "100.00")}))
            @DecimalMin("0.01") BigDecimal amount) {
        Account account = bankingService.withdraw(accountNumber, amount);
        return Response.ok(account).build();
    }

    @POST
    @Path("/accounts/transfer")
    @Operation(summary = "Transfer money", description = "Transfers money between accounts")
    @APIResponse(responseCode = "200", description = "Transfer successful")
    @APIResponse(responseCode = "400", description = "Invalid amount or insufficient balance")
    @APIResponse(responseCode = "404", description = "Account not found")
    public Response transfer(
            @Valid @RequestBody(description = "Transfer request", required = true,
                    content = @Content(schema = @Schema(implementation = TransferRequest.class),
                            examples = {@ExampleObject(name = "TransferRequest", summary = "Transfer between accounts",
                                    value = "{\"fromAccountId\":\"ACC001\",\"toAccountId\":\"ACC002\",\"amount\":150.00}")}))
            TransferRequest request) {
        bankingService.transfer(request.fromAccountId(), request.toAccountId(), request.amount());
        return Response.ok().build();
    }

    @GET
    @Path("/accounts/{accountNumber}/balance")
    @Operation(summary = "Get balance", description = "Retrieves the current account balance")
    @APIResponse(responseCode = "200", description = "Balance retrieved successfully",
            content = @Content(schema = @Schema(implementation = BigDecimal.class),
                    examples = {@ExampleObject(name = "Balance", summary = "Account balance", value = "1000.00")}))
    @APIResponse(responseCode = "404", description = "Account not found")
    public Response getBalance(
            @Parameter(description = "Account number", required = true, example = "ACC001")
            @PathParam("accountNumber") String accountNumber) {
        BigDecimal balance = bankingService.getBalance(accountNumber);
        return Response.ok(balance).build();
    }

    @GET
    @Path("/accounts/{accountNumber}/transactions")
    @Operation(summary = "Get transactions", description = "Retrieves all transactions for an account")
    @APIResponse(responseCode = "200", description = "Transactions retrieved successfully",
            content = @Content(schema = @Schema(implementation = TransactionResponse.class),
                    examples = {@ExampleObject(name = "TransactionList", summary = "List of transactions",
                            value = "[{\"transactionId\":1,\"accountNumber\":\"ACC001\",\"type\":\"DEPOSIT\",\"amount\":250.00,\"timestamp\":\"2025-11-27T10:30:00\",\"description\":\"Deposit\"},{\"transactionId\":2,\"accountNumber\":\"ACC001\",\"type\":\"WITHDRAW\",\"amount\":100.00,\"timestamp\":\"2025-11-27T11:15:00\",\"description\":\"Withdraw\"}]")}))
    @APIResponse(responseCode = "404", description = "Account not found")
    public Response getTransactions(
            @Parameter(description = "Account number", required = true, example = "ACC001")
            @PathParam("accountNumber") String accountNumber) {
        List<TransactionResponse> transactions = bankingService.getTransactions(accountNumber).stream().map((t) -> new TransactionResponse(t.getTransactionId(), t.getAccountNumber(), t.getType().name(), t.getAmount(), t.getTimestamp(), t.getDescription())).toList();
        return Response.ok(transactions).build();
    }

    @GET
    @Path("/config")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = "Get configuration", description = "Retrieves configuration values from environment variables or Kubernetes/OpenShift cluster")
    @APIResponse(responseCode = "200", description = "Configuration retrieved successfully",
            content = @Content(schema = @Schema(implementation = String.class),
                    examples = {@ExampleObject(name = "ConfigMessage", summary = "Configuration message", value = "Banking Title: Banking Quarkus")}))
    @NonBlocking
    public Response config() {
        String message = "Banking Title: " + bankingConfig.title();
        return Response.ok(message).build();
    }

    @GET
    @Path("/data/read-traditional")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Read file traditionally", 
               description = "Reads entire file into memory, deserializes all objects at once (blocking I/O)")
    @APIResponse(responseCode = "200", description = "File read successfully")
    @APIResponse(responseCode = "500", description = "Read failed")
    public Response readFileTraditional() {
        try {
            long startTime = System.currentTimeMillis();
            String filePath = bankingConfig.dataLoadExample();
            
            java.nio.file.Path path = Paths.get(filePath);
            InputStream inputStream;
            
            if (Files.exists(path)) {
                inputStream = new FileInputStream(path.toFile());
            } else {
                inputStream = getClass().getClassLoader().getResourceAsStream(filePath);
                if (inputStream == null) {
                    return Response.status(Response.Status.NOT_FOUND)
                            .entity("File not found: " + filePath)
                            .build();
                }
            }
            
            List<AccountData> accounts = new java.util.ArrayList<>();
            List<TransactionData> transactions = new java.util.ArrayList<>();
            
            ObjectMapper mapper = new ObjectMapper();
            try (JsonParser parser = mapper.getFactory().createParser(inputStream)) {
                while (parser.nextToken() != null) {
                    if (parser.getCurrentToken() == JsonToken.FIELD_NAME) {
                        String fieldName = parser.currentName();
                        parser.nextToken();
                        
                        if ("accounts".equals(fieldName)) {
                            while (parser.nextToken() != JsonToken.END_ARRAY) {
                                accounts.add(parser.readValueAs(AccountData.class));
                            }
                        } else if ("transactions".equals(fieldName)) {
                            while (parser.nextToken() != JsonToken.END_ARRAY) {
                                transactions.add(parser.readValueAs(TransactionData.class));
                            }
                        }
                    }
                }
            }
            
            DataLoadResponse data = new DataLoadResponse(accounts, transactions);
            long duration = System.currentTimeMillis() - startTime;
            
            return Response.ok(data)
                    .header("X-Read-Time-Ms", duration)
                    .header("X-Accounts-Count", data.accounts().size())
                    .header("X-Transactions-Count", data.transactions().size())
                    .header("X-Read-Mode", "BLOCKING")
                    .build();
        } catch (Exception e) {
            log.error("Error reading file", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Read failed: " + e.getMessage())
                    .build();
        }
    }

    @GET
    @Path("/data/read-reactive")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Read data reactively",
            description = "Streams all accounts then transactions incrementally using streaming parser")
    public Multi<DataItem> readDataReactive() {
        Multi<DataItem> accounts = streamJsonArray("accounts", AccountData.class).map(a -> (DataItem) a);
        Multi<DataItem> transactions = streamJsonArray("transactions", TransactionData.class).map(t -> (DataItem) t);
        
        return Multi.createBy().concatenating().streams(accounts, transactions);
    }

    private <T> Multi<T> streamJsonArray(String fieldName, Class<T> targetClass) {
        String filePath = bankingConfig.dataLoadExample();

        return Multi.createFrom().<T>emitter(emitter -> {
            try {
                java.nio.file.Path path = Paths.get(filePath);
                InputStream inputStream = Files.exists(path)
                        ? new FileInputStream(path.toFile())
                        : getClass().getClassLoader().getResourceAsStream(filePath);

                if (inputStream == null) {
                    emitter.fail(new IOException("File not found: " + filePath));
                    return;
                }

                ObjectMapper mapper = new ObjectMapper();
                try (JsonParser parser = mapper.getFactory().createParser(inputStream)) {
                    while (parser.nextToken() != null) {
                        if (parser.getCurrentToken() == JsonToken.FIELD_NAME && fieldName.equals(parser.currentName())) {
                            parser.nextToken();

                            while (parser.nextToken() != JsonToken.END_ARRAY) {
                                if (emitter.isCancelled()) {
                                    return;
                                }
                                T item = parser.readValueAs(targetClass);
                                emitter.emit(item);
                            }
                            break;
                        }
                    }
                    emitter.complete();
                }
            } catch (Exception e) {
                log.error("Error streaming {}", fieldName, e);
                emitter.fail(e);
            }
        }).runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

}
