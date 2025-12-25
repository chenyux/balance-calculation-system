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
@Table(name = "accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Account {
    // 账户号（主键）
    @Id
    private String accountNumber;
    
    // 账户余额（精度18位，小数2位）
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal balance;
    
    // 账户状态（ACTIVE/INACTIVE）
    @Column(nullable = false)
    private String status;
    
    // 最后更新时间
    @Column(name = "last_updated_time")
    private LocalDateTime lastUpdatedTime;
}