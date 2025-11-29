package com.redhat.repository;

import com.redhat.model.Account;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

@Repository
public interface AccountRepository extends CrudRepository<Account, String> {
    @Query("SELECT COUNT(a) FROM Account a WHERE a.accountNumber = :id")
    Long countByAccountNumber(String id);
}
