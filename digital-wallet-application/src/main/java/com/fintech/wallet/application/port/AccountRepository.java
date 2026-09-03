package com.fintech.wallet.application.port;

import com.fintech.wallet.domain.account.Account;
import com.fintech.wallet.domain.account.AccountId;
import java.util.Optional;

public interface AccountRepository {
    Account save(Account account);
    Optional<Account> findById(AccountId accountId);
    Optional<Account> findByIdWithLock(AccountId accountId);
}
