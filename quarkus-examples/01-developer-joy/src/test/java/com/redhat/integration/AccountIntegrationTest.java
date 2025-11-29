package com.redhat.integration;

import com.redhat.model.Account;
import com.redhat.rest.dto.TransactionResponse;
import com.redhat.service.BankingService;
import com.redhat.service.BankingServiceBaseEnhanced;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
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

    @Test
    @Order(5)
    public void listAccounts() {
        Account[] accounts = given()
                .when().get("/api/accounts")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(ContentType.JSON)
                .extract().body().as(Account[].class);

        assertThat(accounts).isNotEmpty();
        assertThat(accounts).hasSize(7);
    }

    @Test
    @Order(6)
    public void depositAmount() {
        given()
                .contentType(ContentType.JSON)
                .body("250.50")
                .when().post("/api/accounts/1001/deposit")
                .then()
                .statusCode(Response.Status.OK.getStatusCode());

        BigDecimal balance = bankingService.getBalance("1001");
        assertThat(balance).isEqualByComparingTo("260.50");
    }

    @Test
    @Order(7)
    public void withdrawAmount() {
        given()
                .contentType(ContentType.JSON)
                .body("60.50")
                .when().post("/api/accounts/1001/withdraw")
                .then()
                .statusCode(Response.Status.OK.getStatusCode());

        BigDecimal balance = bankingService.getBalance("1001");
        assertThat(balance).isEqualByComparingTo("200.00");
    }

    @Test
    @Order(8)
    public void transferBetweenAccounts() {
        JsonObject transferJson = Json.createObjectBuilder()
                .add("fromAccountId", "1001")
                .add("toAccountId", "1002")
                .add("amount", "50.00")
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(transferJson.toString())
                .when().post("/api/accounts/transfer")
                .then()
                .statusCode(Response.Status.OK.getStatusCode());

        assertThat(bankingService.getBalance("1001")).isEqualByComparingTo("150.00");
        assertThat(bankingService.getBalance("1002")).isEqualByComparingTo("51.00");
    }

    @Test
    @Order(9)
    public void getAccountBalance() {
        BigDecimal balance = given()
                .when().get("/api/accounts/1001/balance")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(ContentType.JSON)
                .extract().body().as(BigDecimal.class);

        assertThat(balance).isEqualByComparingTo("150.00");
    }

    @Test
    @Order(10)
    public void getAccountTransactions() {
        TransactionResponse[] transactions = given()
                .when().get("/api/accounts/1001/transactions")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(ContentType.JSON)
                .extract().body().as(TransactionResponse[].class);

        assertThat(transactions).isNotEmpty();
        assertThat(transactions).hasSize(3);
        assertThat(transactions[0].accountNumber()).isEqualTo("1001");
    }

    @Test
    @Order(11)
    public void createDuplicateAccountShouldReturn409() {
        JsonObject accountJson = Json.createObjectBuilder()
                .add("accountNumber", "1001")
                .add("ownerId", "123.456-78")
                .add("initialBalance", "100.0")
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(accountJson.toString())
                .when().post("/api/accounts")
                .then()
                .statusCode(Response.Status.CONFLICT.getStatusCode());
    }

    @Test
    @Order(12)
    public void getBalanceForNonExistentAccountShouldReturn404() {
        given()
                .when().get("/api/accounts/INVALID/balance")
                .then()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    @Order(13)
    public void withdrawMoreThanBalanceShouldReturn400() {
        given()
                .contentType(ContentType.JSON)
                .body("10000.00")
                .when().post("/api/accounts/1001/withdraw")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    @Order(14)
    public void invalidJsonShouldReturn400() {
        given()
                .contentType(ContentType.JSON)
                .body("{invalid json}")
                .when().post("/api/accounts")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

}
