package com.balance.system.mock;

import com.balance.system.entity.Account;
import com.balance.system.entity.Transaction;
import com.balance.system.repository.AccountRepository;
import com.balance.system.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import net.datafaker.Faker;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 模拟数据生成器，项目启动时自动生成测试账户和交易
 * 生产环境可通过配置禁用（如：@Profile("test")）
 */
@Component
@RequiredArgsConstructor
public class MockDataGenerator implements CommandLineRunner {
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    private final Faker faker = new Faker(Locale.US);
    private static final int ACCOUNT_COUNT = 1000; // 测试账户数量
    private static final int TRANSACTION_COUNT = 10000; // 测试交易数量

    @Override
    public void run(String... args) throws Exception {
        // 清空原有数据
        clearOldData();

        // 生成测试账户
        generateMockAccounts();

        // 生成测试交易
        generateMockTransactions();

        System.out.println("Mock data generation completed!");
    }

    /**
     * 清空原有账户和交易数据
     */
    private void clearOldData() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        System.out.println("Cleared old mock data.");
    }

    /**
     * 生成模拟账户
     */
    private void generateMockAccounts() {
        List<Account> accounts = new ArrayList<>();
        for (int i = 0; i < ACCOUNT_COUNT; i++) {
            String accountNumber = "ACC" + String.format("%06d", i);
            BigDecimal balance = new BigDecimal(faker.number().randomDouble(2, 100, 100000));
            Account account = new Account(
                    accountNumber,
                    balance,
                    "ACTIVE",
                    LocalDateTime.now().minusDays(faker.number().numberBetween(1, 30))
            );
            accounts.add(account);
        }
        accountRepository.saveAll(accounts);
        System.out.printf("Generated %d mock accounts.%n", ACCOUNT_COUNT);
    }

    /**
     * 生成模拟交易
     */
    private void generateMockTransactions() {
        List<Transaction> transactions = new ArrayList<>();
        for (int i = 0; i < TRANSACTION_COUNT; i++) {
            String txId = "TX_MOCK_" + String.format("%08d", i);
            // 随机生成源账户和目标账户（避免相同）
            String sourceAccount = "ACC" + String.format("%06d", faker.number().numberBetween(0, ACCOUNT_COUNT - 1));
            String targetAccount = "ACC" + String.format("%06d", faker.number().numberBetween(0, ACCOUNT_COUNT - 1));
            while (sourceAccount.equals(targetAccount)) {
                targetAccount = "ACC" + String.format("%06d", faker.number().numberBetween(0, ACCOUNT_COUNT - 1));
            }
            // 随机交易金额
            BigDecimal amount = new BigDecimal(faker.number().randomDouble(2, 1, 1000));
            // 随机交易时间（近1小时内）
            LocalDateTime txTime = LocalDateTime.now().minusMinutes(faker.number().numberBetween(0, 60));
            // 构造交易
            Transaction transaction = new Transaction(
                    txId,
                    sourceAccount,
                    targetAccount,
                    amount,
                    txTime,
                    "SUCCESS",
                    "Mock transaction: " + faker.lorem().sentence(3)
            );
            transactions.add(transaction);
        }
        transactionRepository.saveAll(transactions);
        System.out.printf("Generated %d mock transactions.%n", TRANSACTION_COUNT);
    }
}