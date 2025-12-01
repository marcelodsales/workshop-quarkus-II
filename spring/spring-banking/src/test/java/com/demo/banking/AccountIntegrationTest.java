package com.demo.banking;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AccountIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("banking_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeEach
    void setup() {
        List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
        interceptors.add((request, body, execution) -> {
            request.getHeaders().add("Authorization", "Bearer fake-jwt-token");
            return execution.execute(request, body);
        });
        restTemplate.getRestTemplate().setInterceptors(interceptors);
    }

    @Test
    void shouldCreateAccount() {
        Map<String, Object> request = Map.of(
            "accountNumber", "ACC001",
            "ownerId", "user123",
            "initialBalance", 0.0
        );

        ResponseEntity<Map> response = restTemplate
            .postForEntity("/accounts", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("accountNumber")).isEqualTo("ACC001");
        assertThat(((Number) response.getBody().get("balance")).doubleValue()).isEqualTo(0.0);
    }

    @Test
    void shouldDepositMoney() {
        Map<String, Object> createRequest = Map.of(
            "accountNumber", "ACC002",
            "ownerId", "user456",
            "initialBalance", 0.0
        );
        ResponseEntity<Map> createResponse = restTemplate
            .postForEntity("/accounts", createRequest, Map.class);
        Long accountId = ((Number) createResponse.getBody().get("id")).longValue();

        Map<String, BigDecimal> depositRequest = Map.of("amount", new BigDecimal("100.00"));
        ResponseEntity<Map> response = restTemplate
            .postForEntity("/accounts/" + accountId + "/deposit", depositRequest, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Number) response.getBody().get("balance")).doubleValue()).isEqualTo(100.0);
    }

    @Test
    void shouldWithdrawMoney() {
        Map<String, Object> createRequest = Map.of(
            "accountNumber", "ACC003",
            "ownerId", "user789",
            "initialBalance", 0.0
        );
        ResponseEntity<Map> createResponse = restTemplate
            .postForEntity("/accounts", createRequest, Map.class);
        Long accountId = ((Number) createResponse.getBody().get("id")).longValue();

        restTemplate.postForEntity("/accounts/" + accountId + "/deposit",
            Map.of("amount", new BigDecimal("200.00")), Map.class);

        Map<String, BigDecimal> withdrawRequest = Map.of("amount", new BigDecimal("50.00"));
        ResponseEntity<Map> response = restTemplate
            .postForEntity("/accounts/" + accountId + "/withdraw", withdrawRequest, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Number) response.getBody().get("balance")).doubleValue()).isEqualTo(150.0);
    }

    @Test
    void shouldTransferMoney() {
        Map<String, Object> createRequest1 = Map.of(
            "accountNumber", "ACC004",
            "ownerId", "userA",
            "initialBalance", 0.0
        );
        ResponseEntity<Map> createResponse1 = restTemplate
            .postForEntity("/accounts", createRequest1, Map.class);
        Long fromAccountId = ((Number) createResponse1.getBody().get("id")).longValue();

        Map<String, Object> createRequest2 = Map.of(
            "accountNumber", "ACC005",
            "ownerId", "userB",
            "initialBalance", 0.0
        );
        ResponseEntity<Map> createResponse2 = restTemplate
            .postForEntity("/accounts", createRequest2, Map.class);
        Long toAccountId = ((Number) createResponse2.getBody().get("id")).longValue();

        restTemplate.postForEntity("/accounts/" + fromAccountId + "/deposit",
            Map.of("amount", new BigDecimal("500.00")), Map.class);

        Map<String, Object> transferRequest = Map.of(
            "fromAccountId", fromAccountId,
            "toAccountId", toAccountId,
            "amount", new BigDecimal("150.00")
        );
        ResponseEntity<Void> response = restTemplate
            .postForEntity("/accounts/transfer", transferRequest, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Map> fromBalance = restTemplate
            .getForEntity("/accounts/" + fromAccountId + "/balance", Map.class);
        ResponseEntity<Map> toBalance = restTemplate
            .getForEntity("/accounts/" + toAccountId + "/balance", Map.class);

        assertThat(((Number) fromBalance.getBody().get("balance")).doubleValue()).isEqualTo(350.0);
        assertThat(((Number) toBalance.getBody().get("balance")).doubleValue()).isEqualTo(150.0);
    }

    @Test
    void shouldGetBalance() {
        Map<String, Object> createRequest = Map.of(
            "accountNumber", "ACC006",
            "ownerId", "user999",
            "initialBalance", 0.0
        );
        ResponseEntity<Map> createResponse = restTemplate
            .postForEntity("/accounts", createRequest, Map.class);
        Long accountId = ((Number) createResponse.getBody().get("id")).longValue();

        restTemplate.postForEntity("/accounts/" + accountId + "/deposit",
            Map.of("amount", new BigDecimal("300.00")), Map.class);

        ResponseEntity<Map> response = restTemplate
            .getForEntity("/accounts/" + accountId + "/balance", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Number) response.getBody().get("balance")).doubleValue()).isEqualTo(300.0);
    }

    @Test
    void shouldGetTransactions() {
        Map<String, Object> createRequest = Map.of(
            "accountNumber", "ACC007",
            "ownerId", "user777",
            "initialBalance", 0.0
        );
        ResponseEntity<Map> createResponse = restTemplate
            .postForEntity("/accounts", createRequest, Map.class);
        Long accountId = ((Number) createResponse.getBody().get("id")).longValue();

        restTemplate.postForEntity("/accounts/" + accountId + "/deposit",
            Map.of("amount", new BigDecimal("100.00")), Map.class);
        restTemplate.postForEntity("/accounts/" + accountId + "/withdraw",
            Map.of("amount", new BigDecimal("30.00")), Map.class);

        ResponseEntity<Object[]> response = restTemplate
            .getForEntity("/accounts/" + accountId + "/transactions", Object[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
    }

    @Test
    void shouldRejectWithdrawWhenInsufficientBalance() {
        Map<String, Object> createRequest = Map.of(
            "accountNumber", "ACC008",
            "ownerId", "user111",
            "initialBalance", 0.0
        );
        ResponseEntity<Map> createResponse = restTemplate
            .postForEntity("/accounts", createRequest, Map.class);
        Long accountId = ((Number) createResponse.getBody().get("id")).longValue();

        restTemplate.postForEntity("/accounts/" + accountId + "/deposit",
            Map.of("amount", new BigDecimal("50.00")), Map.class);

        Map<String, BigDecimal> withdrawRequest = Map.of("amount", new BigDecimal("100.00"));
        ResponseEntity<Map> response = restTemplate
            .postForEntity("/accounts/" + accountId + "/withdraw", withdrawRequest, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldRejectDuplicateAccountNumber() {
        Map<String, Object> request = Map.of(
            "accountNumber", "ACC999",
            "ownerId", "userX",
            "initialBalance", 0.0
        );
        restTemplate.postForEntity("/accounts", request, Map.class);

        ResponseEntity<Map> response = restTemplate
            .postForEntity("/accounts", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
}

