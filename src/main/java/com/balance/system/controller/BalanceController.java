package com.balance.system.controller;

import com.balance.system.entity.Transaction;
import com.balance.system.service.BalanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/balance")
@RequiredArgsConstructor
public class BalanceController {
    private final BalanceService balanceService;

    /**
     * 接收交易请求并处理
     */
    @PostMapping("/transaction")
    public ResponseEntity<String> handleTransaction(@RequestBody Transaction transaction) {
        try {
            String result = balanceService.processTransaction(transaction);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Transaction failed: " + e.getMessage());
        }
    }

    /**
     * 查询指定账户的余额
     */
    @GetMapping("/{accountNumber}")
    public ResponseEntity<BigDecimal> getAccountBalance(@PathVariable String accountNumber) {
        try {
            BigDecimal balance = balanceService.getAccountBalance(accountNumber);
            return ResponseEntity.ok(balance);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}