package com.demo.banking;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class AccountIntegrationTest {

    @Test
    void shouldCreateAccount() {
        Map<String, Object> request = Map.of(
            "accountNumber", "ACC001",
            "ownerId", "user123",
            "initialBalance", 0.0
        );

        given()
            .contentType("application/json")
            .body(request)
        .when()
            .post("/accounts")
        .then()
            .statusCode(201)
            .body("accountNumber", equalTo("ACC001"))
            .body("balance", equalTo(0.0f));
    }

    @Test
    void shouldDepositMoney() {
        Map<String, Object> createRequest = Map.of(
            "accountNumber", "ACC002",
            "ownerId", "user456",
            "initialBalance", 0.0
        );
        
        Integer accountId = given()
            .contentType("application/json")
            .body(createRequest)
        .when()
            .post("/accounts")
        .then()
            .statusCode(201)
            .extract().path("id");

        Map<String, BigDecimal> depositRequest = Map.of("amount", new BigDecimal("100.00"));
        
        given()
            .contentType("application/json")
            .body(depositRequest)
        .when()
            .post("/accounts/" + accountId + "/deposit")
        .then()
            .statusCode(200)
            .body("balance", equalTo(100.0f));
    }

    @Test
    void shouldWithdrawMoney() {
        Map<String, Object> createRequest = Map.of(
            "accountNumber", "ACC003",
            "ownerId", "user789",
            "initialBalance", 0.0
        );
        
        Integer accountId = given()
            .contentType("application/json")
            .body(createRequest)
        .when()
            .post("/accounts")
        .then()
            .statusCode(201)
            .extract().path("id");

        given()
            .contentType("application/json")
            .body(Map.of("amount", new BigDecimal("200.00")))
        .when()
            .post("/accounts/" + accountId + "/deposit")
        .then()
            .statusCode(200);

        Map<String, BigDecimal> withdrawRequest = Map.of("amount", new BigDecimal("50.00"));
        
        given()
            .contentType("application/json")
            .body(withdrawRequest)
        .when()
            .post("/accounts/" + accountId + "/withdraw")
        .then()
            .statusCode(200)
            .body("balance", equalTo(150.0f));
    }

    @Test
    void shouldTransferMoney() {
        Map<String, Object> createRequest1 = Map.of(
            "accountNumber", "ACC004",
            "ownerId", "userA",
            "initialBalance", 0.0
        );
        
        Integer fromAccountId = given()
            .contentType("application/json")
            .body(createRequest1)
        .when()
            .post("/accounts")
        .then()
            .statusCode(201)
            .extract().path("id");

        Map<String, Object> createRequest2 = Map.of(
            "accountNumber", "ACC005",
            "ownerId", "userB",
            "initialBalance", 0.0
        );
        
        Integer toAccountId = given()
            .contentType("application/json")
            .body(createRequest2)
        .when()
            .post("/accounts")
        .then()
            .statusCode(201)
            .extract().path("id");

        given()
            .contentType("application/json")
            .body(Map.of("amount", new BigDecimal("500.00")))
        .when()
            .post("/accounts/" + fromAccountId + "/deposit")
        .then()
            .statusCode(200);

        Map<String, Object> transferRequest = Map.of(
            "fromAccountId", fromAccountId,
            "toAccountId", toAccountId,
            "amount", new BigDecimal("150.00")
        );
        
        given()
            .contentType("application/json")
            .body(transferRequest)
        .when()
            .post("/accounts/transfer")
        .then()
            .statusCode(200);

        given()
        .when()
            .get("/accounts/" + fromAccountId + "/balance")
        .then()
            .statusCode(200)
            .body("balance", equalTo(350.0f));

        given()
        .when()
            .get("/accounts/" + toAccountId + "/balance")
        .then()
            .statusCode(200)
            .body("balance", equalTo(150.0f));
    }

    @Test
    void shouldGetBalance() {
        Map<String, Object> createRequest = Map.of(
            "accountNumber", "ACC006",
            "ownerId", "user999",
            "initialBalance", 0.0
        );
        
        Integer accountId = given()
            .contentType("application/json")
            .body(createRequest)
        .when()
            .post("/accounts")
        .then()
            .statusCode(201)
            .extract().path("id");

        given()
            .contentType("application/json")
            .body(Map.of("amount", new BigDecimal("300.00")))
        .when()
            .post("/accounts/" + accountId + "/deposit")
        .then()
            .statusCode(200);

        given()
        .when()
            .get("/accounts/" + accountId + "/balance")
        .then()
            .statusCode(200)
            .body("balance", equalTo(300.0f));
    }

    @Test
    void shouldGetTransactions() {
        Map<String, Object> createRequest = Map.of(
            "accountNumber", "ACC007",
            "ownerId", "user777",
            "initialBalance", 0.0
        );
        
        Integer accountId = given()
            .contentType("application/json")
            .body(createRequest)
        .when()
            .post("/accounts")
        .then()
            .statusCode(201)
            .extract().path("id");

        given()
            .contentType("application/json")
            .body(Map.of("amount", new BigDecimal("100.00")))
        .when()
            .post("/accounts/" + accountId + "/deposit")
        .then()
            .statusCode(200);

        given()
            .contentType("application/json")
            .body(Map.of("amount", new BigDecimal("30.00")))
        .when()
            .post("/accounts/" + accountId + "/withdraw")
        .then()
            .statusCode(200);

        given()
        .when()
            .get("/accounts/" + accountId + "/transactions")
        .then()
            .statusCode(200)
            .body("size()", equalTo(2));
    }

    @Test
    void shouldRejectWithdrawWhenInsufficientBalance() {
        Map<String, Object> createRequest = Map.of(
            "accountNumber", "ACC008",
            "ownerId", "user111",
            "initialBalance", 0.0
        );
        
        Integer accountId = given()
            .contentType("application/json")
            .body(createRequest)
        .when()
            .post("/accounts")
        .then()
            .statusCode(201)
            .extract().path("id");

        given()
            .contentType("application/json")
            .body(Map.of("amount", new BigDecimal("50.00")))
        .when()
            .post("/accounts/" + accountId + "/deposit")
        .then()
            .statusCode(200);

        Map<String, BigDecimal> withdrawRequest = Map.of("amount", new BigDecimal("100.00"));
        
        given()
            .contentType("application/json")
            .body(withdrawRequest)
        .when()
            .post("/accounts/" + accountId + "/withdraw")
        .then()
            .statusCode(400);
    }

    @Test
    void shouldRejectDuplicateAccountNumber() {
        Map<String, Object> request = Map.of(
            "accountNumber", "ACC999",
            "ownerId", "userX",
            "initialBalance", 0.0
        );
        
        given()
            .contentType("application/json")
            .body(request)
        .when()
            .post("/accounts")
        .then()
            .statusCode(201);

        given()
            .contentType("application/json")
            .body(request)
        .when()
            .post("/accounts")
        .then()
            .statusCode(409);
    }
}
