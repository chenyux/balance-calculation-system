package com.balance.system.integration;

import com.balance.system.entity.Account;
import com.balance.system.entity.Transaction;
import com.balance.system.repository.AccountRepository;
import com.balance.system.repository.TransactionRepository;
import com.balance.system.service.BalanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RedisContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class BalanceServiceIntegrationTest {
    // 启动PostgreSQL容器（自动注入连接配置）
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("test_balance_db")
            .withUsername("test")
            .withPassword("test");

    // 启动Redis容器（自动注入连接配置）
    @Container
    @ServiceConnection
    static RedisContainer<?> redis = new RedisContainer<>("redis:7");

    @Autowired
    private BalanceService balanceService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    // 每个测试方法执行前初始化数据
    @BeforeEach
    void setUp() {
        // 清空数据
        transactionRepository.deleteAll();
        accountRepository.deleteAll();

        // 创建测试账户
        Account acc1 = new Account("ACC001", new BigDecimal("1000.00"), "ACTIVE", LocalDateTime.now());
        Account acc2 = new Account("ACC002", new BigDecimal("500.00"), "ACTIVE", LocalDateTime.now());
        accountRepository.save(acc1);
        accountRepository.save(acc2);
    }

    // 测试完整交易流程
    @Test
    void processTransaction_IntegrationSuccess() {
        // 1. 构造交易
        Transaction transaction = new Transaction();
        transaction.setTransactionId("TX_INTEG_001");
        transaction.setSourceAccount("ACC001");
        transaction.setTargetAccount("ACC002");
        transaction.setAmount(new BigDecimal("200.00"));
        transaction.setDescription("Integration test transfer");

        // 2. 处理交易
        String result = balanceService.processTransaction(transaction);

        // 3. 验证交易结果
        assertTrue(result.contains("successfully"));
        assertEquals("SUCCESS", transactionRepository.findById("TX_INTEG_001").get().getStatus());

        // 4. 验证账户余额
        assertEquals(new BigDecimal("800.00"), balanceService.getAccountBalance("ACC001"));
        assertEquals(new BigDecimal("700.00"), balanceService.getAccountBalance("ACC002"));

        // 5. 验证缓存（再次查询，确认缓存生效）
        assertEquals(new BigDecimal("800.00"), balanceService.getAccountBalance("ACC001"));
    }

    // 测试余额不足场景
    @Test
    void processTransaction_IntegrationInsufficientBalance() {
        // 1. 构造大额交易
        Transaction transaction = new Transaction();
        transaction.setTransactionId("TX_INTEG_002");
        transaction.setSourceAccount("ACC001");
        transaction.setTargetAccount("ACC002");
        transaction.setAmount(new BigDecimal("2000.00"));

        // 2. 处理交易
        String result = balanceService.processTransaction(transaction);

        // 3. 验证结果
        assertTrue(result.contains("Insufficient balance"));
        assertEquals("FAILED", transactionRepository.findById("TX_INTEG_002").get().getStatus());

        // 4. 验证余额未变化
        assertEquals(new BigDecimal("1000.00"), balanceService.getAccountBalance("ACC001"));
        assertEquals(new BigDecimal("500.00"), balanceService.getAccountBalance("ACC002"));
    }
}