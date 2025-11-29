package com.redhat.unit;

import com.redhat.model.Account;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;

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
