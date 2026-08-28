package com.fintech.wallet.api.controller;

import com.fintech.wallet.api.dto.CreateAccountRequestDTO;
import com.fintech.wallet.api.dto.DepositFundsRequestDTO;
import com.fintech.wallet.api.dto.HoldBalanceRequestDTO;
import com.fintech.wallet.application.command.CreateAccountCommand;
import com.fintech.wallet.application.command.DepositFundsCommand;
import com.fintech.wallet.application.command.HoldBalanceCommand;
import com.fintech.wallet.application.dto.AccountResponseDTO;
import com.fintech.wallet.application.usecase.AccountLedgerUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AccountController {
    private final AccountLedgerUseCase accountLedgerUseCase;

    @PostMapping
    public ResponseEntity<AccountResponseDTO> createAccount(
            @Valid @RequestBody CreateAccountRequestDTO request,
            @RequestHeader(value = "X-Tenant-ID", required = true) String tenantId) {

        log.info("Create account request - Tenant: {}, Customer: {}", tenantId, request.getCustomerId());

        CreateAccountCommand command = new CreateAccountCommand(
                tenantId,
                request.getCustomerId(),
                request.getCurrency(),
                request.getInitialBalance()
        );

        AccountResponseDTO response = accountLedgerUseCase.createAccount(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{accountId}/deposit")
    public ResponseEntity<AccountResponseDTO> depositFunds(
            @PathVariable String accountId,
            @Valid @RequestBody DepositFundsRequestDTO request,
            @RequestHeader(value = "X-Tenant-ID", required = true) String tenantId) {

        log.info("Deposit funds request - Account: {}, Tenant: {}, Amount: {}", accountId, tenantId, request.getAmount());

        DepositFundsCommand command = new DepositFundsCommand(accountId, request.getAmount(), tenantId);
        AccountResponseDTO response = accountLedgerUseCase.depositFunds(command);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{accountId}/hold")
    public ResponseEntity<AccountResponseDTO> holdBalance(
            @PathVariable String accountId,
            @Valid @RequestBody HoldBalanceRequestDTO request,
            @RequestHeader(value = "X-Tenant-ID", required = true) String tenantId) {

        log.info("Hold balance request - Account: {}, Tenant: {}, Amount: {}", accountId, tenantId, request.getAmount());

        HoldBalanceCommand command = new HoldBalanceCommand(accountId, request.getAmount(), request.getHoldReference(), tenantId);
        AccountResponseDTO response = accountLedgerUseCase.holdBalance(command);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<String> getAccount(
            @PathVariable String accountId,
            @RequestHeader(value = "X-Tenant-ID", required = true) String tenantId) {

        log.info("Get account request - Account: {}, Tenant: {}", accountId, tenantId);
        return ResponseEntity.ok("Account details endpoint - Phase 2");
    }
}
