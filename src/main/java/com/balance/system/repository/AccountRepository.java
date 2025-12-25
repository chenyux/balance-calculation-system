package com.balance.system.repository;

import com.balance.system.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, String> {
    // 悲观锁查询（FOR UPDATE），保证并发更新安全
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.accountNumber = :accountNumber")
    Optional<Account> findWithLockByAccountNumber(@Param("accountNumber") String accountNumber);

    // 普通查询（用于缓存查询，无锁）
    Optional<Account> findByAccountNumber(String accountNumber);
}