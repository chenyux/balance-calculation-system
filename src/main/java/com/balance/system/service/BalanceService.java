package com.balance.system.service;

import com.balance.system.entity.Account;
import com.balance.system.entity.Transaction;
import com.balance.system.repository.AccountRepository;
import com.balance.system.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BalanceService {
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    /**
     * 处理交易并更新余额（实时处理，带事务和重试）
     * @param transaction 交易信息
     * @return 处理结果
     */
    @Transactional // 数据库事务，保证原子性
    @Retryable( // 重试机制：异常时重试3次，间隔1秒
            value = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
    public String processTransaction(Transaction transaction) {
        // 1. 校验交易是否已存在（避免重复处理）
        if (transactionRepository.existsById(transaction.getTransactionId())) {
            return "Transaction already processed: " + transaction.getTransactionId();
        }

        String sourceAccountNo = transaction.getSourceAccount();
        String targetAccountNo = transaction.getTargetAccount();
        BigDecimal txAmount = transaction.getAmount().abs(); // 统一取绝对值，保证转出/转入一致性

        // 2. 锁定源账户和目标账户（悲观锁，防止并发冲突）
        Account sourceAccount = accountRepository.findWithLockByAccountNumber(sourceAccountNo)
                .orElseThrow(() -> new RuntimeException("Source account not found: " + sourceAccountNo));
        Account targetAccount = accountRepository.findWithLockByAccountNumber(targetAccountNo)
                .orElseThrow(() -> new RuntimeException("Target account not found: " + targetAccountNo));

        // 3. 校验源账户状态和余额
        if (!"ACTIVE".equals(sourceAccount.getStatus())) {
            transaction.setStatus("FAILED");
            transactionRepository.save(transaction);
            return "Source account is inactive: " + sourceAccountNo;
        }
        if (sourceAccount.getBalance().compareTo(txAmount) < 0) {
            transaction.setStatus("FAILED");
            transactionRepository.save(transaction);
            return "Insufficient balance for source account: " + sourceAccountNo;
        }

        // 4. 更新账户余额
        sourceAccount.setBalance(sourceAccount.getBalance().subtract(txAmount));
        targetAccount.setBalance(targetAccount.getBalance().add(txAmount));
        LocalDateTime now = LocalDateTime.now();
        sourceAccount.setLastUpdatedTime(now);
        targetAccount.setLastUpdatedTime(now);

        // 5. 保存账户和交易记录
        accountRepository.save(sourceAccount);
        accountRepository.save(targetAccount);
        transaction.setStatus("SUCCESS");
        transaction.setTimestamp(now);
        transactionRepository.save(transaction);

        // 6. 清除缓存（后续查询加载最新余额）
        evictAccountBalance(sourceAccountNo);
        evictAccountBalance(targetAccountNo);

        return "Transaction processed successfully: " + transaction.getTransactionId();
    }

    /**
     * 查询账户余额（优先从Redis缓存获取）
     * @param accountNumber 账户号
     * @return 账户余额
     */
    @Cacheable(value = "accountBalance", key = "#accountNumber")
    public BigDecimal getAccountBalance(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found: " + accountNumber));
        if (!"ACTIVE".equals(account.getStatus())) {
            throw new RuntimeException("Account is inactive: " + accountNumber);
        }
        return account.getBalance();
    }

    /**
     * 清除指定账户的余额缓存
     * @param accountNumber 账户号
     */
    @CacheEvict(value = "accountBalance", key = "#accountNumber")
    public void evictAccountBalance(String accountNumber) {
        // 仅用于清除缓存，无业务逻辑
    }
}