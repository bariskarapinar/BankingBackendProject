package com.fintech.wallet.application.port;

import com.fintech.wallet.domain.ledger.LedgerEntry;
import java.util.List;

public interface LedgerRepository {
    LedgerEntry save(LedgerEntry entry);
    List<LedgerEntry> findByAccountId(String accountId);
}
