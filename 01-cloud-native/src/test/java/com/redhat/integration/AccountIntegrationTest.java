package com.redhat.integration;

import com.redhat.model.Account;
import com.redhat.service.BankingService;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;
import static io.restassured.RestAssured.*;
import static org.assertj.core.api.Assertions.*;


import java.math.BigDecimal;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AccountIntegrationTest {

    @Inject
    BankingService bankingService;

    @Test
    @Order(1)
    public void createAccount() {
        Account account = bankingService.createAccount("1001", "123.456-78", BigDecimal.TEN);
        assertNotNull(account);
        assertEquals("1001", account.getAccountNumber());
        assertEquals(BigDecimal.TEN, account.getBalance());
    }

    @ParameterizedTest(name = "AccountNumber: {0}, OwnerId: {1}, InitalBalance: {2}")
    @Order(2)
    @CsvSource({
            "1002,321.546-87,1.0",
            "1003,132.654-78,10.0"
    })
    public void createAccounts(String accountNumber, String ownerId, BigDecimal initalBalance) {
        Account account = bankingService.createAccount(accountNumber, ownerId, initalBalance);
        assertNotNull(account);
        assertEquals(accountNumber, account.getAccountNumber());
        assertEquals(ownerId, account.getOwnerId());
        assertEquals(initalBalance, account.getBalance());
    }

    @ParameterizedTest(name = "AccountNumber: {0}, OwnerId: {1}, InitalBalance: {2}")
    @Order(3)
    @CsvFileSource(resources = "/accounts.csv",numLinesToSkip = 1)
    public void createAccountsFromExternalCSV(String accountNumber, String ownerId, BigDecimal initalBalance) {
        Account account = bankingService.createAccount(accountNumber, ownerId, initalBalance);
        assertNotNull(account);
        assertEquals(accountNumber, account.getAccountNumber());
        assertEquals(ownerId, account.getOwnerId());
        assertEquals(initalBalance, account.getBalance());
    }

    @Test
    @Order(4)
    public void createAccountWithRestEndpoint(){

        JsonObject accountJson = Json.createObjectBuilder()
                .add("accountNumber", "999.999-99")
                .add("ownerId", "027-863-70")
                .add("initialBalance", "5000.0")
                .build();

        Account returnedAccount = given().
                contentType(ContentType.JSON)
                .body(accountJson.toString())
                .when().post("/api/accounts")
                .then()
                .statusCode(Response.Status.CREATED.getStatusCode())
                .extract().body().as(Account.class);

        assertThat(returnedAccount.getAccountNumber()).isEqualTo("999.999-99");
        assertThat(returnedAccount.getOwnerId()).isEqualTo("027-863-70");
        assertThat(returnedAccount.getBalance()).isEqualTo("5000.0");
    }

}
