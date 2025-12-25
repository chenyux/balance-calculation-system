package com.balance.system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    // 唯一交易ID（主键）
    @Id
    private String transactionId;
    
    // 源账户号
    @Column(name = "source_account", nullable = false)
    private String sourceAccount;
    
    // 目标账户号
    @Column(name = "target_account", nullable = false)
    private String targetAccount;
    
    // 交易金额（正数为转入，负数为转出）
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;
    
    // 交易时间戳
    @Column(name = "transaction_time", nullable = false)
    private LocalDateTime timestamp;
    
    // 交易状态（SUCCESS/FAILED/RETRYING）
    @Column(nullable = false)
    private String status;
    
    // 交易描述
    private String description;
}