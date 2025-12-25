package com.balance.system.service;

import com.balance.system.entity.Account;
import com.balance.system.entity.Transaction;
import com.balance.system.repository.AccountRepository;
import com.balance.system.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BalanceServiceTest {
    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private BalanceService balanceService;

    // 测试正常交易处理
    @Test
    void processTransaction_Success() {
        // 1. 构造测试数据
        String txId = "TX_TEST_001";
        String sourceAcc = "ACC001";
        String targetAcc = "ACC002";
        BigDecimal txAmount = new BigDecimal("100.00");

        Transaction transaction = new Transaction();
        transaction.setTransactionId(txId);
        transaction.setSourceAccount(sourceAcc);
        transaction.setTargetAccount(targetAcc);
        transaction.setAmount(txAmount);

        Account sourceAccount = new Account(sourceAcc, new BigDecimal("500.00"), "ACTIVE", LocalDateTime.now());
        Account targetAccount = new Account(targetAcc, new BigDecimal("200.00"), "ACTIVE", LocalDateTime.now());

        // 2. Mock依赖方法
        when(transactionRepository.existsById(txId)).thenReturn(false);
        when(accountRepository.findWithLockByAccountNumber(sourceAcc)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findWithLockByAccountNumber(targetAcc)).thenReturn(Optional.of(targetAccount));

        // 3. 执行测试方法
        String result = balanceService.processTransaction(transaction);

        // 4. 验证结果
        assertTrue(result.contains("successfully"));
        assertEquals(new BigDecimal("400.00"), sourceAccount.getBalance());
        assertEquals(new BigDecimal("300.00"), targetAccount.getBalance());
        assertEquals("SUCCESS", transaction.getStatus());
        assertNotNull(transaction.getTimestamp());

        // 5. 验证方法调用
        verify(transactionRepository, times(1)).existsById(txId);
        verify(accountRepository, times(1)).save(sourceAccount);
        verify(accountRepository, times(1)).save(targetAccount);
        verify(transactionRepository, times(1)).save(transaction);
        verify(balanceService, times(1)).evictAccountBalance(sourceAcc);
        verify(balanceService, times(1)).evictAccountBalance(targetAcc);
    }

    // 测试源账户余额不足
    @Test
    void processTransaction_InsufficientBalance() {
        // 1. 构造测试数据
        String txId = "TX_TEST_002";
        String sourceAcc = "ACC001";
        String targetAcc = "ACC002";
        BigDecimal txAmount = new BigDecimal("1000.00");

        Transaction transaction = new Transaction();
        transaction.setTransactionId(txId);
        transaction.setSourceAccount(sourceAcc);
        transaction.setTargetAccount(targetAcc);
        transaction.setAmount(txAmount);

        Account sourceAccount = new Account(sourceAcc, new BigDecimal("500.00"), "ACTIVE", LocalDateTime.now());
        Account targetAccount = new Account(targetAcc, new BigDecimal("200.00"), "ACTIVE", LocalDateTime.now());

        // 2. Mock依赖方法
        when(transactionRepository.existsById(txId)).thenReturn(false);
        when(accountRepository.findWithLockByAccountNumber(sourceAcc)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findWithLockByAccountNumber(targetAcc)).thenReturn(Optional.of(targetAccount));

        // 3. 执行测试方法
        String result = balanceService.processTransaction(transaction);

        // 4. 验证结果
        assertTrue(result.contains("Insufficient balance"));
        assertEquals("FAILED", transaction.getStatus());
        assertEquals(new BigDecimal("500.00"), sourceAccount.getBalance()); // 余额未变化
        assertEquals(new BigDecimal("200.00"), targetAccount.getBalance()); // 余额未变化

        // 5. 验证方法调用
        verify(transactionRepository, times(1)).save(transaction);
        verify(accountRepository, never()).save(sourceAccount);
        verify(accountRepository, never()).save(targetAccount);
    }

    // 测试重复交易
    @Test
    void processTransaction_DuplicateTx() {
        // 1. 构造测试数据
        String txId = "TX_TEST_003";
        Transaction transaction = new Transaction();
        transaction.setTransactionId(txId);

        // 2. Mock依赖方法
        when(transactionRepository.existsById(txId)).thenReturn(true);

        // 3. 执行测试方法
        String result = balanceService.processTransaction(transaction);

        // 4. 验证结果
        assertTrue(result.contains("already processed"));

        // 5. 验证方法调用
        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(accountRepository, never()).findWithLockByAccountNumber(anyString());
    }

    // 测试账户查询
    @Test
    void getAccountBalance_Success() {
        // 1. 构造测试数据
        String accountNo = "ACC001";
        BigDecimal balance = new BigDecimal("500.00");
        Account account = new Account(accountNo, balance, "ACTIVE", LocalDateTime.now());

        // 2. Mock依赖方法
        when(accountRepository.findByAccountNumber(accountNo)).thenReturn(Optional.of(account));

        // 3. 执行测试方法
        BigDecimal result = balanceService.getAccountBalance(accountNo);

        // 4. 验证结果
        assertEquals(balance, result);
    }

    // 测试查询不存在的账户
    @Test
    void getAccountBalance_AccountNotFound() {
        // 1. 构造测试数据
        String accountNo = "ACC999";

        // 2. Mock依赖方法
        when(accountRepository.findByAccountNumber(accountNo)).thenReturn(Optional.empty());

        // 3. 执行测试方法并验证异常
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            balanceService.getAccountBalance(accountNo);
        });
        assertTrue(exception.getMessage().contains("not found"));
    }
}