package com.redhat.rest;

import com.redhat.model.Account;
import com.redhat.rest.dto.AccountRequest;
import com.redhat.service.BankingService;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Protected Banking API", description = "Account and transaction management operations protected by JWT")
@Slf4j
public class ProtectedBankingRestResource {

    @Inject
    SecurityIdentity securityIdentity;

    @Inject
    protected BankingService bankingService;

    @POST
    @Path("/protected/accounts")
    @Operation(summary = "Create account", description = "Creates a new bank account")
    @APIResponse(responseCode = "201", description = "Account created successfully", content = @Content(schema = @Schema(implementation = Account.class), examples = {@ExampleObject(name = "CreatedAccount", summary = "Example of a created account", value = "{\"accountNumber\":\"ACC001\",\"balance\":1000.00,\"ownerId\":\"OWNER123\"}")}))
    @APIResponse(responseCode = "400", description = "Invalid request")
    @APIResponse(responseCode = "409", description = "Account already exists")
    @RolesAllowed({"role1"})
    public Response createAccount(@Valid @RequestBody(description = "Account creation request", required = true, content = @Content(schema = @Schema(implementation = AccountRequest.class), examples = {@ExampleObject(name = "ValidRequest", summary = "Example of a valid account creation request", value = "{\"accountNumber\":\"ACC001\",\"ownerId\":\"OWNER123\",\"initialBalance\":1000.00}"), @ExampleObject(name = "InvalidRequest", summary = "Example of an invalid request (negative balance)", value = "{\"accountNumber\":\"ACC002\",\"ownerId\":\"OWNER456\",\"initialBalance\":-50.00}")})) AccountRequest request) {
        Account account = bankingService.createAccount(request.accountNumber(), request.ownerId(), request.initialBalance());
        return Response.status(Response.Status.CREATED).entity(account).build();
    }

    @GET
    @Path("/protected/accounts")
    @Operation(summary = "List accounts", description = "Retrieves all bank accounts")
    @APIResponse(responseCode = "200", description = "Accounts retrieved successfully",
            content = @Content(schema = @Schema(implementation = Account.class),
                    examples = {@ExampleObject(name = "AccountList", summary = "List of accounts",
                            value = "[{\"accountNumber\":\"ACC001\",\"balance\":1000.00,\"ownerId\":\"OWNER123\"},{\"accountNumber\":\"ACC002\",\"balance\":500.00,\"ownerId\":\"OWNER456\"}]")}))
    @RolesAllowed({"role2"})
    public Response listAccounts() {
        return Response.status(Response.Status.OK).entity(bankingService.getAllAccounts()).build();
    }

}
