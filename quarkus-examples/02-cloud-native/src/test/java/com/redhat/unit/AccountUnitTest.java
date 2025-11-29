package com.redhat.unit;

import com.redhat.model.Account;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AccountUnitTest {
    @Test
    void shouldCreateAccount(){
        Account account = Account.builder()
                .accountNumber("abc123")
                .balance(BigDecimal.TEN)
                .ownerId("123.456-78")
                .build();
        assertNotNull(account);
        assertEquals("abc123", account.getAccountNumber());
        assertEquals(BigDecimal.TEN, account.getBalance());
    }
}
