package com.fintech.wallet.api.controller;

import com.fintech.wallet.api.request.InitiateTransferRequest;
import com.fintech.wallet.application.command.InitiateP2PTransferCommand;
import com.fintech.wallet.application.dto.TransferResponseDTO;
import com.fintech.wallet.application.saga.P2PTransferSaga;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
public class TransferController {
    private final P2PTransferSaga p2pTransferSaga;

    @PostMapping("/p2p")
    public ResponseEntity<TransferResponseDTO> initiateP2PTransfer(
            @Valid @RequestBody InitiateTransferRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        log.info("Received P2P transfer request - Source: {}, Destination: {}, Tenant: {}",
                request.getSourceAccountId(), request.getDestinationAccountId(), tenantId);

        InitiateP2PTransferCommand command = new InitiateP2PTransferCommand(
                request.getSourceAccountId(),
                request.getDestinationAccountId(),
                request.getAmount().toPlainString(),
                tenantId,
                request.getIdempotencyKey()
        );

        TransferResponseDTO response = p2pTransferSaga.initiateP2PTransfer(command);
        
        log.info("P2P transfer initiated successfully - Transfer ID: {}", response.getTransferId());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/{transferId}")
    public ResponseEntity<TransferResponseDTO> getTransferStatus(
            @PathVariable String transferId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        log.info("Fetching transfer status - Transfer ID: {}, Tenant: {}", transferId, tenantId);
        // Phase 2 enhancement: Load transfer from repository and return
        // For now, return 501 Not Implemented
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
