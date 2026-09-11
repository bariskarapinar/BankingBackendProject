# Enterprise Digital Wallet and Payment Gateway Platform

## 1. Project Overview

This repository contains a multi-module Java platform for digital wallet accounts, ledger management, peer-to-peer transfers, fraud scoring, reliable event publication, distributed locking, and event-sourced transfer auditing.

The implementation is organized around:

- Domain-Driven Design (DDD)
- Hexagonal architecture (ports and adapters)
- Transactional application services
- Event-driven integration
- The transactional outbox pattern
- P2P transfer saga orchestration
- Multi-tenant request isolation
- Optimistic, pessimistic, and distributed locking
- Rule-based fraud assessment
- Event sourcing foundations and replay
- CQRS-oriented reporting query ports
- PostgreSQL, Redis, Kafka, and Flyway
- Java 21 and Spring Boot 3.3

The repository is structured as a Maven reactor with five business modules:

```text
digital-wallet-common
digital-wallet-domain
digital-wallet-application
digital-wallet-infrastructure
digital-wallet-api
```

The root project is a parent POM. The API module is the executable Spring Boot application.

## 2. Current Implementation Status

### Phase 1: Account and ledger foundation

Implemented:

- Account aggregate
- Currency-aware `Money` value object
- Account identifier value object
- Deposits
- Balance holds and releases
- Ledger entries
- Account repository ports and JPA adapters
- Redis distributed locks
- Outbox persistence
- Kafka event publishing adapter
- Multi-tenant request headers
- OAuth2 resource-server configuration
- Flyway migration `V1__Initial_Schema.sql`
- Domain and application tests

### Phase 2: P2P transfer engine

Implemented:

- Transfer aggregate
- Transfer identifier value object
- Nine-state transfer state machine
- Idempotency key handling
- P2P transfer saga
- Compensation handling
- Rule-based fraud scoring
- Risk score levels
- Six fraud rules
- Resilience4j circuit breaker and retry configuration
- Transfer persistence
- Transfer API
- Flyway migration `V2__Transfer_Schema.sql`

### Phase 3: Event sourcing and CQRS foundations

Implemented:

- Stored event model
- Snapshot model and persistence
- PostgreSQL event-store adapter
- Event version metadata
- Transfer state-change events
- Transfer event replay service
- Tenant-safe replay endpoint
- Transfer summary query port
- Transfer summary query adapter
- Flyway migration `V3__Event_Sourcing.sql`

Still suitable for future enhancement:

- Fully asynchronous projection handlers
- A separately materialized transfer read database
- Account balance projections
- Snapshot creation policy and scheduled snapshot compaction
- Event upcasters for concrete historical event versions
- Production-specific OAuth2 issuer configuration
- A complete account detail read endpoint
- Full integration and end-to-end test automation

## 3. Design Goals

### 3.1 Correctness over convenience

Financial amounts use `BigDecimal` and explicit currencies. Domain objects validate their invariants before state changes. Database constraints duplicate important business rules so invalid records cannot be inserted accidentally.

### 3.2 Explicit boundaries

The domain layer does not depend on Spring, JPA, Redis, Kafka, or HTTP. The application layer depends on ports. Infrastructure implements those ports. The API translates HTTP requests into application commands.

### 3.3 Safe retry behavior

P2P transfers require an idempotency key. Repeating the same request returns the previously persisted transfer instead of creating another transfer.

### 3.4 Tenant isolation

Every API mutation and transfer status/replay request requires an `X-Tenant-ID` header. Tenant identifiers are carried through commands, lock keys, transfer records, and stored events.

### 3.5 Reliable integration

Domain events are written to the outbox as part of the application transaction. The outbox is the durable hand-off point for Kafka publishing and avoids the classic dual-write problem where a database transaction succeeds but message publication fails.

## 4. Architecture

```text
                  +-----------------------------+
                  |        REST API / HTTP       |
                  | controllers, DTOs, security  |
                  +--------------+--------------+
                                 |
                                 v
                  +-----------------------------+
                  |       Application Layer     |
                  | commands, use cases, saga,  |
                  | ports, replay, resilience   |
                  +--------------+--------------+
                                 |
                    depends on interfaces only
                                 |
                  +--------------v--------------+
                  |          Domain Layer        |
                  | aggregates, value objects,  |
                  | rules, state transitions,   |
                  | domain events               |
                  +-----------------------------+

                  +-----------------------------+
                  |     Infrastructure Layer    |
                  | JPA, PostgreSQL, Flyway,    |
                  | Redis, Kafka, adapters       |
                  +-----------------------------+
```

### 4.1 Inbound flow

```text
HTTP request
  -> controller
  -> request validation
  -> command creation
  -> application use case or saga
  -> domain aggregate mutation
  -> repository and outbox ports
  -> infrastructure adapters
  -> HTTP response
```

### 4.2 Transfer flow

```text
POST /api/v1/transfers/p2p
  |
  +-- validate request and tenant header
  |
  +-- check idempotency key
  |
  +-- create Transfer aggregate
  |
  +-- append TransferInitiatedEvent to:
  |     - outbox_events
  |     - stored_events
  |
  +-- load source and destination accounts
  |
  +-- move transfer to PENDING_VALIDATION
  |
  +-- calculate fraud assessment
  |
  +-- if blocked:
  |     - move to FRAUD_REJECTED
  |     - persist state-change and failure events
  |     - return a business error
  |
  +-- acquire source and destination Redis locks
  |
  +-- reload both accounts with database locks
  |
  +-- hold source balance
  |
  +-- move transfer to DEBIT_RESERVED
  |
  +-- deposit destination balance
  |
  +-- move transfer to CREDIT_PROCESSING
  |
  +-- release source hold and withdraw source funds
  |
  +-- move transfer to COMPLETED
  |
  +-- persist transfer, outbox events, and stored events
  |
  +-- release Redis locks in finally blocks
```

## 5. Repository Layout

```text
.
├── pom.xml
├── README.md
├── INDEX.md
├── QUICK_START_GUIDE.md
├── COMPLETION_CHECKLIST.md
├── PHASE_1_ARCHITECTURE.md
├── PHASE_1_DELIVERY_SUMMARY.md
├── PHASE_2_ARCHITECTURE.md
├── PHASE_2_DELIVERY_SUMMARY.md
├── PHASE_2_COMPLETION_CHECKLIST.md
│
├── digital-wallet-common/
│   ├── pom.xml
│   └── src/main/java/com/fintech/wallet/common/
│       ├── exception/
│       │   ├── WalletException.java
│       │   ├── DomainException.java
│       │   ├── ApplicationException.java
│       │   └── InfrastructureException.java
│       └── model/
│           ├── Command.java
│           └── DomainEvent.java
│
├── digital-wallet-domain/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/fintech/wallet/domain/
│       │   ├── account/
│       │   │   ├── Account.java
│       │   │   ├── AccountId.java
│       │   │   └── Money.java
│       │   ├── event/
│       │   │   ├── AccountCreatedEvent.java
│       │   │   ├── BalanceHeldEvent.java
│       │   │   ├── BalanceReleasedEvent.java
│       │   │   ├── TransferInitiatedEvent.java
│       │   │   ├── TransferCompletedEvent.java
│       │   │   ├── TransferFailedEvent.java
│       │   │   ├── TransferStateChangedEvent.java
│       │   │   └── sourcing/
│       │   │       ├── StoredEvent.java
│       │   │       └── Snapshot.java
│       │   ├── fraud/
│       │   │   ├── FraudAssessment.java
│       │   │   ├── FraudRulesEngine.java
│       │   │   └── RiskScore.java
│       │   ├── ledger/
│       │   │   └── LedgerEntry.java
│       │   └── transfer/
│       │       ├── Transfer.java
│       │       ├── TransferId.java
│       │       └── TransferStatus.java
│       └── test/java/
│
├── digital-wallet-application/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/fintech/wallet/application/
│       │   ├── command/
│       │   ├── dto/
│       │   ├── event/
│       │   ├── port/
│       │   ├── resilience/
│       │   ├── saga/
│       │   ├── service/
│       │   └── usecase/
│       └── test/java/
│
├── digital-wallet-infrastructure/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/fintech/wallet/infrastructure/
│       │   ├── adapter/
│       │   ├── config/
│       │   ├── entity/
│       │   ├── event/
│       │   ├── persistence/
│       │   ├── redis/
│       │   └── repository/
│       └── main/resources/
│           ├── application.properties
│           └── db/migration/
│               ├── V1__Initial_Schema.sql
│               ├── V2__Transfer_Schema.sql
│               └── V3__Event_Sourcing.sql
│
└── digital-wallet-api/
    ├── pom.xml
    └── src/
        ├── main/java/com/fintech/wallet/api/
        │   ├── config/
        │   ├── controller/
        │   ├── dto/
        │   ├── exception/
        │   ├── request/
        │   └── DigitalWalletPlatformApplication.java
        └── main/resources/application.yml
```

## 6. Maven Modules and Dependencies

### 6.1 `digital-wallet-common`

Contains dependency-light shared types:

- Base exception hierarchy
- `Command` marker interface
- `DomainEvent` base class

This module should remain independent from Spring and infrastructure libraries.

### 6.2 `digital-wallet-domain`

Contains the business model:

- Account aggregate
- Transfer aggregate
- Money and identifier value objects
- Ledger entry entity
- Fraud rules
- Domain events
- Stored event and snapshot models

The domain module depends on common types and Lombok, but its business rules are framework-independent.

### 6.3 `digital-wallet-application`

Contains application behavior and interfaces:

- Account commands
- Transfer commands
- Account use case
- Transfer saga
- Repository ports
- Locking port
- Event publishing port
- Outbox port
- Event store port
- Query model port
- Replay service
- Resilience configuration and facade

This module defines what infrastructure must provide without depending on concrete databases or brokers.

### 6.4 `digital-wallet-infrastructure`

Contains concrete adapters:

- Spring Data JPA repositories
- JPA persistence entities
- Repository adapters
- Event-store adapter
- Redis locking adapter
- Kafka publisher
- Flyway migrations
- PostgreSQL, Redis, Kafka, and resilience dependencies

### 6.5 `digital-wallet-api`

Contains:

- Spring Boot entry point
- REST controllers
- HTTP DTOs
- Bean Validation
- OAuth2 resource-server security
- Global exception mapping
- HTTP configuration

## 7. Domain Model

## 7.1 `Money`

`Money` is a value object containing:

- A `BigDecimal` amount
- A `java.util.Currency` currency

The value object protects financial calculations from floating-point precision errors and rejects invalid operations such as currency-mismatched addition or subtraction.

Typical invariants:

- Amount must be present.
- Currency must be present.
- Arithmetic must use the same currency.
- Amount precision is preserved.
- Negative or zero amounts are rejected where the business operation requires a positive amount.

The database uses `NUMERIC(19,4)` / `DECIMAL(19,4)` for persisted financial amounts.

## 7.2 `AccountId`

`AccountId` wraps an account UUID and validates that the supplied value is a valid UUID string.

## 7.3 `Account` aggregate

The account aggregate owns:

- Account identifier
- Tenant identifier
- Customer identifier
- Currency
- Current balance
- Held balance
- Version
- Created and updated timestamps
- Pending domain events

Supported business operations include:

- Create account
- Deposit funds
- Hold balance
- Release a hold
- Withdraw funds
- Calculate available balance

Available balance is conceptually:

```text
available balance = balance - hold balance
```

The aggregate emits domain events for account creation, balance holds, and balance releases.

## 7.4 `LedgerEntry`

Ledger entries are intended to be immutable, append-only transaction records. They contain:

- Entry identifier
- Account identifier
- Tenant identifier
- Entry type
- Amount
- Currency
- Reference
- Description
- Creation timestamp

The ledger table has foreign-key and query indexes but no update workflow.

## 7.5 `Transfer` aggregate

The transfer aggregate owns:

- Transfer identifier
- Tenant identifier
- Source account identifier
- Destination account identifier
- Transfer amount
- Idempotency key
- Current transfer status
- Failure reason
- Aggregate version
- Creation and update timestamps
- Pending domain events

Transfers to the same source and destination account are rejected.

### Transfer statuses

| Status | Meaning | Terminal |
|---|---|---:|
| `INITIATED` | Aggregate was created and the request was accepted for validation | No |
| `PENDING_VALIDATION` | Account and fraud validation is in progress | No |
| `DEBIT_RESERVED` | Source funds have been held for the transfer | No |
| `DEBIT_FAILED` | Reserved debit failed | No |
| `CREDIT_PROCESSING` | Destination credit is being applied | No |
| `COMPLETED` | Transfer completed successfully | Yes |
| `FAILED` | Transfer failed without a successful compensation result | Yes |
| `COMPENSATED` | Previously reserved work was compensated | Yes |
| `FRAUD_REJECTED` | Fraud assessment blocked the transfer | Yes |

The aggregate also supports a rehydration constructor for database mapping and a controlled state restoration method for event replay.

## 7.6 Transfer domain events

### `TransferInitiatedEvent`

Contains:

- Transfer identifier
- Tenant identifier
- Source account identifier
- Destination account identifier
- Amount
- Idempotency key

### `TransferStateChangedEvent`

Contains:

- Transfer identifier
- Tenant identifier
- Previous status
- Current status
- Failure reason, when applicable

This event makes the event stream sufficient to reconstruct the transfer status history.

### `TransferCompletedEvent`

Contains:

- Transfer identifier
- Amount

### `TransferFailedEvent`

Contains:

- Transfer identifier
- Failure reason

## 8. Fraud Detection

The fraud subsystem consists of:

- `FraudRulesEngine`
- `FraudEvaluationContext`
- `RiskScore`
- `FraudAssessment`
- `FraudScoringPort`
- `FraudScoringAdapter`

The rules engine uses an additive score, capped at `1.0`.

### Fraud rules

| Rule | Trigger | Score |
|---|---|---:|
| `HIGH_TRANSFER_AMOUNT` | Transfer exceeds the high-value threshold | `0.30` |
| `SUSPICIOUS_TIME` | Transfer occurs during the configured suspicious time window | `0.15` |
| `NEW_DESTINATION_ACCOUNT` | Destination is treated as a new destination | `0.20` |
| `RAPID_SUCCESSION_TRANSFERS` | Recent transfer velocity is suspicious | `0.25` |
| `GEOGRAPHIC_ANOMALY` | Location behavior is anomalous | `0.35` |
| `MICRO_TRANSFER_PATTERN` | Amount is below the micro-transfer threshold | `0.10` |

The current saga supplies some contextual values as defaults. Destination history, velocity history, geographic data, and device data are extension points for a future production integration.

### Risk levels

The `RiskScore` value object classifies the score into:

| Range | Level |
|---|---|
| `0.0` through below `0.3` | `LOW` |
| `0.3` through below `0.7` | `MEDIUM` |
| `0.7` through below `0.9` | `HIGH` |
| `0.9` through `1.0` | `CRITICAL` |

The assessment exposes:

- Triggered rules
- Total risk score
- Risk level
- Blocking decision
- Manual-review decision
- Human-readable summary

## 9. Application Services

## 9.1 `AccountLedgerUseCase`

The account use case coordinates:

1. Input command validation.
2. Account loading or creation.
3. Redis lock acquisition.
4. Domain aggregate mutation.
5. Ledger persistence.
6. Outbox persistence.
7. Lock release.
8. Response mapping.

The use case is transactional and uses the `AccountRepository`, `LedgerRepository`, `DistributedLockPort`, and `OutboxEventRepository` ports.

## 9.2 `P2PTransferSaga`

The saga is the application coordinator for P2P transfers.

Detailed processing sequence:

1. Find an existing transfer by idempotency key.
2. Return the existing transfer if found.
3. Generate a new transfer identifier.
4. Create the transfer aggregate.
5. Persist the initial event.
6. Persist the transfer record.
7. Load source account.
8. Load destination account.
9. Change status to `PENDING_VALIDATION`.
10. Execute fraud assessment.
11. Reject and persist the event stream if fraud blocks the transfer.
12. Build tenant-scoped source and destination lock keys.
13. Acquire locks in deterministic source-then-destination order.
14. Reload both accounts with pessimistic database locks.
15. Hold the source amount.
16. Change status to `DEBIT_RESERVED`.
17. Persist the source account and transfer.
18. Change status to `CREDIT_PROCESSING`.
19. Deposit into the destination account.
20. Persist the destination account and transfer.
21. Release the source hold.
22. Withdraw the source amount.
23. Change status to `COMPLETED`.
24. Persist transfer events to the outbox and event store.
25. Release both distributed locks in `finally` blocks.

### Compensation

If the credit or completion path fails after a source hold:

- The source hold is released.
- The source account is saved.
- The transfer is marked `COMPENSATED`.
- If compensation itself fails, the transfer is marked `FAILED` with a combined failure reason.
- Failure events are persisted.

## 9.3 `TransferEventReplayService`

The replay service:

1. Loads all stored events for a transfer aggregate.
2. Requires an initial `TransferInitiatedEvent`.
3. Creates the transfer aggregate from the initiation payload.
4. Clears constructor-generated transient events.
5. Applies each `TransferStateChangedEvent` in sequence order.
6. Restores status, failure reason, timestamp, and replay version.
7. Validates tenant ownership.
8. Maps the reconstructed aggregate to `TransferResponseDTO`.

Replay is useful for:

- Audit verification
- Debugging transfer state
- Rebuilding a transfer response from the event log
- Validating event-store completeness

## 10. Ports

The application layer defines the following ports:

### Account and ledger ports

- `AccountRepository`
- `LedgerRepository`

These abstract account persistence and immutable ledger storage.

### Transfer ports

- `TransferRepository`
- `TransferSummaryQueryPort`

`TransferRepository` supports:

- Save
- Find by transfer identifier
- Find by idempotency key

`TransferSummaryQueryPort` supports:

- Transfer summary lookup
- Source-account history
- Destination-account history
- Status queries
- Date-range queries
- Total transfer volume
- Transfer counts by status

### Event ports

- `EventPublisherPort`
- `OutboxEventRepository`
- `EventStorePort`

`EventStorePort` supports:

- Append event
- Load aggregate events
- Load events after a sequence number
- Load events by type
- Load events by tenant
- Save snapshot
- Load latest snapshot
- Count all events
- Count aggregate events

### Concurrency port

- `DistributedLockPort`

This port abstracts Redis locking so application services do not depend directly on a Redis client.

### Fraud port

- `FraudScoringPort`

This allows a future external fraud service or machine-learning model to replace or augment the local rules engine.

## 11. Persistence and Database Schema

Flyway migrations are stored in:

```text
digital-wallet-infrastructure/src/main/resources/db/migration/
```

Migrations:

1. `V1__Initial_Schema.sql`
2. `V2__Transfer_Schema.sql`
3. `V3__Event_Sourcing.sql`

Hibernate is configured with `ddl-auto=validate`, so Hibernate validates the schema rather than creating or changing it. Flyway is responsible for schema creation and evolution.

## 11.1 `accounts`

Purpose: current account aggregate state.

| Column | Type | Null | Default | Description |
|---|---|---:|---|---|
| `id` | `VARCHAR(36)` | No | None | Account UUID primary key |
| `tenant_id` | `VARCHAR(255)` | No | None | Tenant boundary |
| `customer_id` | `VARCHAR(255)` | No | None | Owning customer |
| `currency` | `VARCHAR(3)` | No | None | ISO-style currency code |
| `balance` | `NUMERIC(19,4)` | No | None | Current balance |
| `hold_balance` | `NUMERIC(19,4)` | No | `0` | Total held amount |
| `version` | `BIGINT` | No | `1` | Optimistic locking version |
| `created_at` | `TIMESTAMP` | No | Current timestamp | Creation time |
| `updated_at` | `TIMESTAMP` | No | Current timestamp | Last update time |

Indexes:

- `idx_tenant_customer (tenant_id, customer_id)`
- `idx_tenant_id (tenant_id)`

## 11.2 `ledger_entries`

Purpose: immutable account transaction history.

| Column | Type | Null | Description |
|---|---|---:|---|
| `id` | `VARCHAR(36)` | No | Ledger entry UUID |
| `account_id` | `VARCHAR(36)` | No | Referenced account |
| `tenant_id` | `VARCHAR(255)` | No | Tenant boundary |
| `type` | `VARCHAR(20)` | No | Ledger entry type |
| `amount` | `NUMERIC(19,4)` | No | Entry amount |
| `currency` | `VARCHAR(3)` | No | Entry currency |
| `reference` | `VARCHAR(255)` | No | Business reference |
| `description` | `TEXT` | Yes | Human-readable description |
| `created_at` | `TIMESTAMP` | No | Creation time |

Constraints:

- Primary key on `id`
- Foreign key from `account_id` to `accounts(id)`

Indexes:

- `idx_account_id (account_id)`
- `idx_tenant_id_ledger (tenant_id)`
- `idx_created_at (created_at DESC)`

## 11.3 `outbox_events`

Purpose: durable event hand-off for asynchronous publication.

| Column | Type | Null | Default | Description |
|---|---|---:|---|---|
| `id` | `VARCHAR(36)` | No | None | Outbox event identifier |
| `event_type` | `VARCHAR(255)` | No | None | Domain event type |
| `aggregate_id` | `VARCHAR(255)` | No | None | Aggregate identifier |
| `payload` | `TEXT` | No | None | Serialized event payload |
| `published` | `BOOLEAN` | No | `false` | Publication status |
| `published_at` | `TIMESTAMP` | Yes | None | Successful publication time |
| `created_at` | `TIMESTAMP` | No | Current timestamp | Creation time |

Indexes:

- `idx_published (published)`
- `idx_aggregate_id (aggregate_id)`
- `idx_created_at_outbox (created_at DESC)`

The outbox record is created in the same transaction as the aggregate mutation. A publisher process can later select unpublished records, publish them to Kafka, and mark them published.

## 11.4 `transfers`

Purpose: current transfer aggregate state and idempotency lookup.

| Column | Type | Null | Default | Description |
|---|---|---:|---|---|
| `transfer_id` | `VARCHAR(36)` | No | None | Transfer UUID primary key |
| `tenant_id` | `VARCHAR(36)` | No | None | Tenant boundary |
| `source_account_id` | `VARCHAR(36)` | No | None | Debit account |
| `destination_account_id` | `VARCHAR(36)` | No | None | Credit account |
| `amount` | `DECIMAL(19,4)` | No | None | Transfer amount |
| `currency` | `VARCHAR(3)` | No | `USD` | Transfer currency |
| `status` | `VARCHAR(50)` | No | `INITIATED` | Transfer state |
| `idempotency_key` | `VARCHAR(256)` | No | None | Duplicate request key |
| `failure_reason` | `VARCHAR(1000)` | Yes | None | Failure or compensation reason |
| `created_at` | `TIMESTAMP` | No | None | Creation time |
| `updated_at` | `TIMESTAMP` | No | None | Last update time |
| `version` | `BIGINT` | No | `0` | Optimistic locking version |

Constraints:

- Primary key on `transfer_id`
- Unique constraint on `idempotency_key`
- Foreign key from `source_account_id` to `accounts(id)`
- Foreign key from `destination_account_id` to `accounts(id)`
- `amount > 0`
- Status must be one of the nine transfer statuses

Indexes:

- `idx_transfer_source (source_account_id)`
- `idx_transfer_dest (destination_account_id)`
- `idx_transfer_status (status)`
- `idx_transfer_idempotency (idempotency_key)`
- `idx_transfer_tenant (tenant_id)`
- `idx_transfer_created_at (created_at DESC)`

## 11.5 `stored_events`

Purpose: immutable event stream for aggregate audit and replay.

| Column | Type | Null | Description |
|---|---|---:|---|
| `event_id` | `VARCHAR(36)` | No | Event UUID primary key |
| `aggregate_id` | `VARCHAR(36)` | No | Aggregate identifier |
| `aggregate_type` | `VARCHAR(100)` | No | Aggregate type, such as `Transfer` |
| `sequence_number` | `BIGINT` | No | Event position within aggregate stream |
| `event_type` | `VARCHAR(200)` | No | Concrete event class name |
| `event_payload` | `TEXT` | No | Serialized event JSON |
| `event_version` | `INTEGER` | No | Schema version for future upcasting |
| `occurred_at` | `TIMESTAMP` | No | Event timestamp |
| `tenant_id` | `VARCHAR(36)` | No | Tenant boundary |

Constraints:

- Primary key on `event_id`
- Unique `(aggregate_id, sequence_number)`

Indexes:

- `idx_stored_events_type (event_type)`
- `idx_stored_events_tenant (tenant_id)`

The aggregate sequence constraint prevents two events from occupying the same stream position.

## 11.6 `aggregate_snapshots`

Purpose: store serialized aggregate state so long event streams can be loaded efficiently.

| Column | Type | Null | Description |
|---|---|---:|---|
| `snapshot_id` | `VARCHAR(36)` | No | Snapshot UUID primary key |
| `aggregate_id` | `VARCHAR(36)` | No | Aggregate identifier |
| `aggregate_type` | `VARCHAR(100)` | No | Aggregate type |
| `sequence_number` | `BIGINT` | No | Last event included |
| `aggregate_state` | `TEXT` | No | Serialized aggregate state |
| `snapshot_version` | `INTEGER` | No | Snapshot format version |
| `created_at` | `TIMESTAMP` | No | Snapshot creation time |

Index:

- `idx_snapshots_aggregate (aggregate_id, sequence_number DESC)`

The domain `Snapshot` model treats a snapshot as stale when more than 100 events have been appended after its sequence number.

## 12. Complete SQL Migrations

### 12.1 V1: account, ledger, and outbox schema

```sql
CREATE TABLE IF NOT EXISTS accounts (
    id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    customer_id VARCHAR(255) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    balance NUMERIC(19, 4) NOT NULL,
    hold_balance NUMERIC(19, 4) NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tenant_customer ON accounts(tenant_id, customer_id);
CREATE INDEX idx_tenant_id ON accounts(tenant_id);

CREATE TABLE IF NOT EXISTS ledger_entries (
    id VARCHAR(36) PRIMARY KEY,
    account_id VARCHAR(36) NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    type VARCHAR(20) NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    reference VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_account_id FOREIGN KEY (account_id) REFERENCES accounts(id)
);

CREATE INDEX idx_account_id ON ledger_entries(account_id);
CREATE INDEX idx_tenant_id_ledger ON ledger_entries(tenant_id);
CREATE INDEX idx_created_at ON ledger_entries(created_at DESC);

CREATE TABLE IF NOT EXISTS outbox_events (
    id VARCHAR(36) PRIMARY KEY,
    event_type VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    published BOOLEAN NOT NULL DEFAULT false,
    published_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_published ON outbox_events(published);
CREATE INDEX idx_aggregate_id ON outbox_events(aggregate_id);
CREATE INDEX idx_created_at_outbox ON outbox_events(created_at DESC);
```

### 12.2 V2: transfer schema

```sql
CREATE TABLE IF NOT EXISTS transfers (
    transfer_id VARCHAR(36) PRIMARY KEY NOT NULL,
    tenant_id VARCHAR(36) NOT NULL,
    source_account_id VARCHAR(36) NOT NULL,
    destination_account_id VARCHAR(36) NOT NULL,
    amount DECIMAL(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    status VARCHAR(50) NOT NULL DEFAULT 'INITIATED',
    idempotency_key VARCHAR(256) NOT NULL UNIQUE,
    failure_reason VARCHAR(1000),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_transfer_source FOREIGN KEY (source_account_id)
        REFERENCES accounts(id) ON DELETE RESTRICT,
    CONSTRAINT fk_transfer_dest FOREIGN KEY (destination_account_id)
        REFERENCES accounts(id) ON DELETE RESTRICT,
    CONSTRAINT chk_transfer_amount CHECK (amount > 0),
    CONSTRAINT chk_transfer_status CHECK (status IN (
        'INITIATED',
        'PENDING_VALIDATION',
        'DEBIT_RESERVED',
        'DEBIT_FAILED',
        'CREDIT_PROCESSING',
        'COMPLETED',
        'FAILED',
        'COMPENSATED',
        'FRAUD_REJECTED'
    ))
);

CREATE INDEX idx_transfer_source ON transfers(source_account_id);
CREATE INDEX idx_transfer_dest ON transfers(destination_account_id);
CREATE INDEX idx_transfer_status ON transfers(status);
CREATE INDEX idx_transfer_idempotency ON transfers(idempotency_key);
CREATE INDEX idx_transfer_tenant ON transfers(tenant_id);
CREATE INDEX idx_transfer_created_at ON transfers(created_at DESC);
```

### 12.3 V3: event-sourcing schema

```sql
CREATE TABLE IF NOT EXISTS stored_events (
    event_id VARCHAR(36) PRIMARY KEY NOT NULL,
    aggregate_id VARCHAR(36) NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    sequence_number BIGINT NOT NULL,
    event_type VARCHAR(200) NOT NULL,
    event_payload TEXT NOT NULL,
    event_version INTEGER NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    tenant_id VARCHAR(36) NOT NULL,
    CONSTRAINT uq_stored_event_sequence UNIQUE (aggregate_id, sequence_number)
);

CREATE INDEX IF NOT EXISTS idx_stored_events_type ON stored_events(event_type);
CREATE INDEX IF NOT EXISTS idx_stored_events_tenant ON stored_events(tenant_id);

CREATE TABLE IF NOT EXISTS aggregate_snapshots (
    snapshot_id VARCHAR(36) PRIMARY KEY NOT NULL,
    aggregate_id VARCHAR(36) NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    sequence_number BIGINT NOT NULL,
    aggregate_state TEXT NOT NULL,
    snapshot_version INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_snapshots_aggregate
    ON aggregate_snapshots(aggregate_id, sequence_number DESC);
```

## 13. REST API

The application context path is `/wallet`. Therefore, the complete base URL is:

```text
http://localhost:8080/wallet
```

All business endpoints require:

- An authenticated OAuth2 bearer token
- An `X-Tenant-ID` header

## 13.1 Create account

```http
POST /wallet/api/v1/accounts
Content-Type: application/json
Authorization: Bearer <jwt>
X-Tenant-ID: tenant-1
```

Request:

```json
{
  "customerId": "customer-001",
  "currency": "USD",
  "initialBalance": "1000.00"
}
```

Fields:

| Field | Required | Validation |
|---|---:|---|
| `customerId` | Yes | Non-blank |
| `currency` | Yes | Non-blank currency code |
| `initialBalance` | Yes | Positive decimal string |

The request DTO also contains a `tenantId` field for compatibility, but the controller uses the `X-Tenant-ID` header as the tenant source of truth.

Expected status:

```text
201 Created
```

Example:

```bash
curl -X POST 'http://localhost:8080/wallet/api/v1/accounts' \
  -H 'Authorization: Bearer <jwt>' \
  -H 'Content-Type: application/json' \
  -H 'X-Tenant-ID: tenant-1' \
  -d '{
    "customerId": "customer-001",
    "currency": "USD",
    "initialBalance": "1000.00"
  }'
```

## 13.2 Deposit funds

```http
POST /wallet/api/v1/accounts/{accountId}/deposit
Content-Type: application/json
Authorization: Bearer <jwt>
X-Tenant-ID: tenant-1
```

Request:

```json
{
  "amount": "250.00"
}
```

Expected status:

```text
200 OK
```

Example:

```bash
curl -X POST \
  'http://localhost:8080/wallet/api/v1/accounts/7e1c8c4b-9f12-4c8c-a9c1-7b2b8e1f1001/deposit' \
  -H 'Authorization: Bearer <jwt>' \
  -H 'Content-Type: application/json' \
  -H 'X-Tenant-ID: tenant-1' \
  -d '{"amount":"250.00"}'
```

## 13.3 Hold account balance

```http
POST /wallet/api/v1/accounts/{accountId}/hold
Content-Type: application/json
Authorization: Bearer <jwt>
X-Tenant-ID: tenant-1
```

Request:

```json
{
  "amount": "200.00",
  "holdReference": "authorization-123"
}
```

Fields:

| Field | Required | Validation |
|---|---:|---|
| `amount` | Yes | Positive decimal string |
| `holdReference` | Yes | Non-blank |

Expected status:

```text
200 OK
```

## 13.4 Get account

```http
GET /wallet/api/v1/accounts/{accountId}
Authorization: Bearer <jwt>
X-Tenant-ID: tenant-1
```

The route exists, but the current controller returns a placeholder response while the complete account query/read model is being finalized.

## 13.5 Initiate a P2P transfer

```http
POST /wallet/api/v1/transfers/p2p
Content-Type: application/json
Authorization: Bearer <jwt>
X-Tenant-ID: tenant-1
```

Request:

```json
{
  "sourceAccountId": "7e1c8c4b-9f12-4c8c-a9c1-7b2b8e1f1001",
  "destinationAccountId": "c6f9d1b2-6e5d-4ed8-9d7b-2a4f0ce21002",
  "amount": 125.50,
  "idempotencyKey": "transfer-request-2026-000001"
}
```

Fields:

| Field | Required | Validation |
|---|---:|---|
| `sourceAccountId` | Yes | Non-blank UUID |
| `destinationAccountId` | Yes | Non-blank UUID and different from source |
| `amount` | Yes | Positive decimal |
| `idempotencyKey` | Yes | Non-blank and unique for a transfer request |

Expected status:

```text
202 Accepted
```

Example:

```bash
curl -X POST 'http://localhost:8080/wallet/api/v1/transfers/p2p' \
  -H 'Authorization: Bearer <jwt>' \
  -H 'Content-Type: application/json' \
  -H 'X-Tenant-ID: tenant-1' \
  -d '{
    "sourceAccountId": "7e1c8c4b-9f12-4c8c-a9c1-7b2b8e1f1001",
    "destinationAccountId": "c6f9d1b2-6e5d-4ed8-9d7b-2a4f0ce21002",
    "amount": 125.50,
    "idempotencyKey": "transfer-request-2026-000001"
  }'
```

Example response shape:

```json
{
  "transferId": "1fc0dd21-7fcb-4cc7-8987-5f1df1ecf2a1",
  "status": "COMPLETED",
  "sourceAccountId": "7e1c8c4b-9f12-4c8c-a9c1-7b2b8e1f1001",
  "destinationAccountId": "c6f9d1b2-6e5d-4ed8-9d7b-2a4f0ce21002",
  "amount": "125.50",
  "failureReason": null,
  "version": 1
}
```

## 13.6 Get transfer status

```http
GET /wallet/api/v1/transfers/{transferId}
Authorization: Bearer <jwt>
X-Tenant-ID: tenant-1
```

The controller:

1. Loads the transfer by identifier.
2. Returns a not-found application error if it does not exist.
3. Compares the stored tenant with `X-Tenant-ID`.
4. Returns the same not-found behavior for a tenant mismatch.
5. Maps the transfer to `TransferResponseDTO`.

This avoids exposing whether a transfer exists in another tenant.

## 13.7 Replay a transfer from its event stream

```http
GET /wallet/api/v1/transfers/{transferId}/replay
Authorization: Bearer <jwt>
X-Tenant-ID: tenant-1
```

This endpoint reconstructs the transfer from `stored_events` rather than loading its current state from `transfers`.

It is intended for:

- Audit checks
- Replay verification
- Event-store diagnostics
- Comparing event-derived and current-state-derived results

## 14. HTTP Error Contract

Errors are mapped by `GlobalExceptionHandler`.

### Domain errors

Mapped to HTTP `400 Bad Request`.

Typical examples:

- Invalid account identifier
- Invalid amount
- Currency mismatch
- Insufficient available balance
- Invalid aggregate state transition
- Transfer to the same account

### Application errors

Mapped to HTTP `409 Conflict`.

Typical examples:

- Account not found during a business operation
- Idempotency or transfer conflict
- Lock acquisition failure
- Fraud rejection
- Transfer processing failure
- Replay stream failure

### Infrastructure errors

Mapped to HTTP `500 Internal Server Error`.

### Validation errors

Mapped to HTTP `400 Bad Request` with field-level validation details.

Example shape:

```json
{
  "errorCode": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "details": "Check validationErrors for details",
  "timestamp": "2026-09-10T17:00:00Z",
  "path": "/wallet/api/v1/transfers/p2p",
  "status": 400,
  "validationErrors": [
    "amount: Transfer amount must be positive"
  ]
}
```

## 15. Security

The API is configured as an OAuth2 resource server:

- API routes require authentication.
- Account controller methods additionally use `@PreAuthorize("isAuthenticated()")`.
- JWT decoding uses the configured JWK set URI.
- Health endpoint is permitted anonymously.
- Tenant context is supplied with `X-Tenant-ID`.

Default development JWK configuration:

```text
http://localhost:8080/auth/realms/master/protocol/openid-connect/certs
```

For production, configure the JWK set URI through an environment-specific profile or externalized configuration. Do not hard-code development identity-provider URLs in production.

Tenant isolation is applied at several layers:

1. HTTP header is required.
2. Tenant is copied into application commands.
3. Tenant is stored on accounts, transfers, and events.
4. Tenant is included in Redis lock keys.
5. Transfer status and replay endpoints compare tenant ownership.
6. Stored event queries support tenant filtering.

## 16. Concurrency and Locking

The platform uses three complementary mechanisms.

### 16.1 Redis distributed lock

P2P lock keys use:

```text
account-lock:{tenantId}:{accountId}
```

The saga:

- Acquires source first.
- Acquires destination second.
- Uses a ten-second lock timeout.
- Releases destination before source.
- Releases locks in `finally` blocks.

Acquiring account locks in a deterministic order reduces deadlock risk between transfers involving the same accounts.

### 16.2 Pessimistic database lock

After Redis locks are acquired, accounts are reloaded through repository methods that use database locking for the critical balance mutation.

### 16.3 Optimistic versioning

Account and transfer JPA entities have `@Version` fields. A conflicting update produces an optimistic-lock failure instead of silently overwriting a newer state.

## 17. Event-Driven Integration

The platform separates three event concerns:

### Domain events

Created by aggregates to describe business facts.

### Outbox events

Persisted in `outbox_events` in the same transaction as the aggregate change. They provide a durable publication queue.

### Stored events

Persisted in `stored_events` as an immutable aggregate event stream. They provide audit and replay capability.

The transfer saga writes relevant events to both the outbox and event store.

## 18. Kafka Integration

The infrastructure module includes a Kafka event publisher adapter.

Configured defaults:

```properties
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.producer.acks=all
spring.kafka.producer.retries=3
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.StringSerializer
```

Consumer defaults:

```properties
spring.kafka.consumer.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=wallet-platform-consumer
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.auto-offset-reset=earliest
```

The outbox publisher process is the recommended place to provide:

- Retry handling
- Dead-letter routing
- Publication metrics
- Duplicate-safe producer behavior
- Consumer-specific topic routing

## 19. Resilience

Resilience4j configuration contains:

### Circuit breaker

- Failure-rate threshold: 50 percent
- Sliding window size: 100 calls
- Slow-call threshold: five seconds
- Open-state wait duration: 30 seconds
- Half-open permitted calls: five

### Retry

- Maximum attempts: three
- Initial wait duration: 500 milliseconds
- Exponential backoff multiplier: two
- Retryable connection and timeout failures
- Non-retryable argument failures

The `ResiliencePatternFacade` exposes:

- Circuit-breaker execution
- Retry execution
- Combined circuit-breaker and retry execution
- Metrics accessors

## 20. Configuration

### 20.1 API configuration

`digital-wallet-api/src/main/resources/application.yml` contains the Spring Boot application configuration.

Important values:

```yaml
server:
  port: 8080
  servlet:
    context-path: /wallet

spring:
  application:
    name: digital-wallet-platform
  profiles:
    active: prod
```

### 20.2 PostgreSQL

Development defaults:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/wallet_db
    username: postgres
    password: postgres
```

Recommended production changes:

- Use environment variables or a secret manager.
- Use TLS.
- Use a least-privilege database user.
- Configure pool sizing based on workload.
- Enable database backups and point-in-time recovery.

### 20.3 Redis

Development defaults:

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    timeout: 2000
```

Redis is used for distributed account locks. Production Redis should use authentication, TLS where appropriate, persistence policy appropriate to the deployment, and high-availability topology.

### 20.4 Flyway

```yaml
flyway:
  enabled: true
  locations: classpath:db/migration
  baseline-on-migrate: true
```

Migration rules:

- Never edit an already-applied migration in a shared environment.
- Add a new versioned migration for schema changes.
- Test migrations against a clean database and an upgraded database.
- Keep JPA `ddl-auto` set to `validate`.

### 20.5 Logging

Development logging is intentionally verbose:

```yaml
logging:
  level:
    com.fintech.wallet: DEBUG
    org.springframework.web: INFO
    org.springframework.security: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

Production recommendations:

- Reduce SQL and parameter logging.
- Use structured JSON logs.
- Include correlation and tenant identifiers in log context.
- Avoid logging bearer tokens, credentials, or sensitive payment data.

## 21. Local Development Setup

### Prerequisites

- Java 21
- Maven 3.8 or newer
- PostgreSQL 14 or newer
- Redis 7 or newer
- Kafka and ZooKeeper, or a Kafka-compatible local distribution
- Docker, recommended for local dependencies

### Start PostgreSQL

```bash
docker run --name wallet-postgres \
  --detach \
  --publish 5432:5432 \
  --env POSTGRES_DB=wallet_db \
  --env POSTGRES_USER=postgres \
  --env POSTGRES_PASSWORD=postgres \
  postgres:15
```

### Start Redis

```bash
docker run --name wallet-redis \
  --detach \
  --publish 6379:6379 \
  redis:7
```

### Start Kafka

Use an existing local Kafka installation or a project-specific Docker Compose stack. The configured broker is:

```text
localhost:9092
```

### Build all modules

```bash
mvn clean package
```

### Run tests

```bash
mvn test
```

### Run the API module

```bash
mvn spring-boot:run -pl digital-wallet-api
```

### Run only a module

```bash
mvn -pl digital-wallet-domain test
mvn -pl digital-wallet-application test
mvn -pl digital-wallet-infrastructure test
mvn -pl digital-wallet-api test
```

When running a module that depends on sibling modules, use `-am`:

```bash
mvn -pl digital-wallet-api -am test
```

## 22. Example End-to-End Flow

### Step 1: Create source account

```bash
curl -X POST 'http://localhost:8080/wallet/api/v1/accounts' \
  -H 'Authorization: Bearer <jwt>' \
  -H 'Content-Type: application/json' \
  -H 'X-Tenant-ID: tenant-1' \
  -d '{
    "customerId": "customer-source",
    "currency": "USD",
    "initialBalance": "1000.00"
  }'
```

Save the returned account identifier as `SOURCE_ACCOUNT_ID`.

### Step 2: Create destination account

```bash
curl -X POST 'http://localhost:8080/wallet/api/v1/accounts' \
  -H 'Authorization: Bearer <jwt>' \
  -H 'Content-Type: application/json' \
  -H 'X-Tenant-ID: tenant-1' \
  -d '{
    "customerId": "customer-destination",
    "currency": "USD",
    "initialBalance": "0.01"
  }'
```

Save the returned account identifier as `DESTINATION_ACCOUNT_ID`.

### Step 3: Initiate transfer

```bash
curl -X POST 'http://localhost:8080/wallet/api/v1/transfers/p2p' \
  -H 'Authorization: Bearer <jwt>' \
  -H 'Content-Type: application/json' \
  -H 'X-Tenant-ID: tenant-1' \
  -d "{
    \"sourceAccountId\": \"${SOURCE_ACCOUNT_ID}\",
    \"destinationAccountId\": \"${DESTINATION_ACCOUNT_ID}\",
    \"amount\": 100.00,
    \"idempotencyKey\": \"demo-transfer-$(date +%s)\"
  }"
```

### Step 4: Read current transfer state

```bash
curl -X GET \
  'http://localhost:8080/wallet/api/v1/transfers/<TRANSFER_ID>' \
  -H 'Authorization: Bearer <jwt>' \
  -H 'X-Tenant-ID: tenant-1'
```

### Step 5: Replay transfer state from events

```bash
curl -X GET \
  'http://localhost:8080/wallet/api/v1/transfers/<TRANSFER_ID>/replay' \
  -H 'Authorization: Bearer <jwt>' \
  -H 'X-Tenant-ID: tenant-1'
```

## 23. Testing Strategy

### Domain tests

Domain tests should verify:

- Money construction and validation
- Currency-aware arithmetic
- Account creation
- Deposits
- Holds
- Hold release
- Available balance
- Insufficient funds behavior
- Transfer state transitions
- Invalid transfer transitions
- Fraud rule boundaries
- Risk score classification

### Application tests

Application tests should verify:

- Lock acquisition and release
- Account use-case orchestration
- Event and outbox persistence calls
- Idempotency behavior
- Saga compensation
- Fraud rejection
- Replay behavior

### Infrastructure tests

Infrastructure tests should verify:

- JPA mappings
- Flyway migrations
- Repository queries
- Optimistic locking
- Event-store sequence constraints
- Snapshot retrieval
- Redis lock semantics
- Kafka publication behavior

### API tests

API tests should verify:

- Request validation
- Authentication behavior
- Tenant header requirements
- HTTP status mapping
- Error response shape
- Tenant mismatch behavior
- Replay endpoint behavior

## 24. Observability

Current observability integration includes:

- Structured application logs through SLF4J
- Circuit-breaker state and retry logging
- Transfer saga step logging
- Fraud block logging
- Error logging through global exception handling
- Spring Boot Actuator exposure for:
  - Health
  - Info
  - Metrics
  - Prometheus

Configured management endpoints:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

Production monitoring should include:

- Transfer success rate
- Transfer failure rate
- Fraud rejection rate
- Lock acquisition failure rate
- Compensation rate
- Outbox backlog size
- Unpublished event age
- Event replay failures
- Database connection pool utilization
- Redis latency
- Kafka producer failures
- Circuit-breaker state

## 25. Operational Runbook

### Outbox backlog

If unpublished outbox records grow:

1. Check Kafka connectivity.
2. Check publisher process health.
3. Inspect failed event payloads.
4. Check broker permissions and topic availability.
5. Review retry and dead-letter handling.
6. Do not delete unpublished records without an approved recovery procedure.

### Transfer stuck in an intermediate state

1. Inspect `transfers`.
2. Inspect the aggregate's `stored_events`.
3. Check Redis lock ownership and expiration.
4. Check database lock contention.
5. Check application logs for the saga step.
6. Use the replay endpoint to compare event-derived and current-state-derived status.
7. Apply a compensating business action rather than manually editing financial records.

### Fraud false positives

1. Inspect the fraud assessment summary.
2. Identify triggered rules.
3. Confirm source data and thresholds.
4. Review the transfer event stream.
5. Change rule configuration through a reviewed deployment.

### Migration failure

1. Stop application rollout.
2. Inspect Flyway history.
3. Determine whether the migration was applied partially.
4. Restore or repair through a database-approved procedure.
5. Never silently mark a failed financial schema migration as successful.

## 26. Security and Data Handling

Never commit:

- Database passwords
- OAuth client secrets
- JWT signing keys
- Kafka credentials
- Redis passwords
- Production connection strings
- Customer payment data

Use:

- Environment variables
- Secret-manager references
- Kubernetes Secrets
- Vault or cloud secret stores
- Per-environment configuration

Sensitive fields should be redacted from logs. In particular:

- Authorization headers
- Tokens
- Full customer identifiers where unnecessary
- Payment instrument data
- Internal infrastructure credentials

## 27. Performance Considerations

Performance-related design choices include:

- Database indexes for tenant, status, account, idempotency, and timestamps
- Redis locking to coordinate instances
- Pessimistic locking only around critical account mutations
- Optimistic versioning to detect stale writes
- Batch-oriented Hibernate settings
- Append-only ledger and event tables
- Event sequence indexes
- Snapshot schema for long event streams
- Fraud evaluation with a bounded rule set

Performance testing should measure:

- Single-transfer latency
- Concurrent transfers from the same account
- Concurrent transfers across tenants
- Lock contention
- Database transaction duration
- Outbox publication throughput
- Event replay latency
- Snapshot versus full-stream replay

## 28. Extension Points

The architecture is prepared for:

### Advanced fraud

- External ML score provider
- Velocity query port
- Device fingerprint port
- Geographic risk provider
- Customer behavior profile

### External payments

- Wire transfer gateway
- Card processor adapter
- ACH adapter
- Payment webhook adapter
- Settlement reconciliation service

### CQRS projections

- Transfer summary materialized table
- Account balance view
- Customer activity view
- Tenant reporting database
- Kafka-driven projection workers

### Compliance

- KYC provider
- AML screening
- Sanctions screening
- Immutable compliance audit log
- Regulatory reporting exports

### Platform operations

- OpenTelemetry traces
- Prometheus dashboards
- Alertmanager rules
- Kubernetes deployment manifests
- Horizontal Pod Autoscaler
- Managed PostgreSQL and Redis

## 29. Known Limitations and Follow-Up Work

The following items are intentionally visible rather than hidden:

1. The account `GET` endpoint currently returns a placeholder response and should be backed by a dedicated query service.
2. The transfer summary query port and adapter exist, but an HTTP reporting controller is not yet exposed.
3. Snapshot persistence exists, but automatic snapshot creation and snapshot-assisted replay orchestration should be added.
4. Event version metadata exists, but concrete upcaster implementations are still future work.
5. Fraud context currently uses default assumptions for some historical and geographic signals.
6. Production OAuth2 issuer and JWK settings must be externalized.
7. Full integration tests should be run with PostgreSQL, Redis, and Kafka containers.
8. Event sequence allocation should use a concurrency-safe aggregate-stream sequencing strategy under high parallel write load.
9. Production outbox publishing should include explicit retry, dead-letter, and operational replay workflows.
10. Read models should eventually be maintained asynchronously from Kafka events rather than querying the write model directly.

## 30. Recommended Production Checklist

### Application

- [ ] Externalize all secrets.
- [ ] Configure production OAuth2 issuer and JWK URI.
- [ ] Configure CORS and trusted origins.
- [ ] Add request correlation IDs.
- [ ] Add tenant authorization beyond header presence.
- [ ] Add rate limiting.
- [ ] Add idempotency retention and cleanup policy.

### Database

- [ ] Enable TLS.
- [ ] Configure backups and point-in-time recovery.
- [ ] Create least-privilege users.
- [ ] Review indexes with production query plans.
- [ ] Add retention policies for events and outbox records.
- [ ] Verify migration rollback and disaster recovery procedures.

### Messaging

- [ ] Create Kafka topics explicitly.
- [ ] Configure replication and retention.
- [ ] Add dead-letter topics.
- [ ] Make consumers idempotent.
- [ ] Monitor consumer lag.
- [ ] Monitor outbox age and backlog.

### Reliability

- [ ] Test Redis failure behavior.
- [ ] Test PostgreSQL failover.
- [ ] Test Kafka unavailability.
- [ ] Test lock expiration and recovery.
- [ ] Test compensation failures.
- [ ] Test duplicate requests.
- [ ] Test tenant mismatch attempts.

### Observability

- [ ] Add distributed tracing.
- [ ] Add dashboards.
- [ ] Add SLOs and alerts.
- [ ] Add audit event retention monitoring.
- [ ] Add fraud and compensation alerts.

## 31. Technology Stack

| Area | Technology |
|---|---|
| Language | Java 21 |
| Build | Maven |
| Framework | Spring Boot 3.3 |
| Web | Spring MVC |
| Security | Spring Security OAuth2 Resource Server |
| Persistence | Spring Data JPA and Hibernate |
| Database | PostgreSQL |
| Schema migration | Flyway |
| Distributed lock | Redis and Jedis |
| Messaging | Apache Kafka |
| Resilience | Resilience4j |
| Serialization | Jackson |
| Boilerplate reduction | Lombok |
| Mapping support | MapStruct dependency |
| Testing | JUnit 5, Mockito, Spring Boot Test, Testcontainers |

## 32. Related Documentation

- `INDEX.md`: broad project index and roadmap
- `QUICK_START_GUIDE.md`: shorter setup and usage guide
- `PHASE_1_ARCHITECTURE.md`: account and ledger architecture
- `PHASE_1_DELIVERY_SUMMARY.md`: Phase 1 implementation summary
- `PHASE_2_ARCHITECTURE.md`: transfer and fraud architecture
- `PHASE_2_DELIVERY_SUMMARY.md`: Phase 2 implementation summary
- `PHASE_2_COMPLETION_CHECKLIST.md`: Phase 2 checklist
- `COMPLETION_CHECKLIST.md`: overall delivery checklist

## 33. License and Contribution Notes

Before distributing or deploying this repository, add the project's approved license and contribution policy. For financial software, all changes should pass:

1. Domain tests.
2. Application orchestration tests.
3. Persistence and migration tests.
4. API contract tests.
5. Security tests.
6. Concurrency tests.
7. Operational rollback review.

Every change that affects money movement, balance availability, transfer state, event sequencing, idempotency, or tenant isolation should receive an explicit design review.

