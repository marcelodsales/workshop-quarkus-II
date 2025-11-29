package com.redhat.repository;

import com.redhat.model.Transaction;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Param;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends CrudRepository<Transaction, Long> {
    @Query("SELECT t FROM Transaction t WHERE t.accountNumber = :accountNumber")
    List<Transaction> findByAccountNumber(@Param("accountNumber") String accountNumber);
}
