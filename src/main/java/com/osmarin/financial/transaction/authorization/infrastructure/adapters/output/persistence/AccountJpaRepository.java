package com.osmarin.financial.transaction.authorization.infrastructure.adapters.output.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface AccountJpaRepository
        extends JpaRepository<AccountEntity, UUID> {

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select account from AccountEntity account where account.id = :id")
    java.util.Optional<AccountEntity> findByIdForUpdate(@Param("id") UUID id);

    @Modifying
    @Query(
            value = """
                    INSERT INTO accounts (
                        id,
                        owner_id,
                        balance,
                        currency,
                        status,
                        opened_at
                    )
                    VALUES (
                        :id,
                        :ownerId,
                        :balance,
                        :currency,
                        :status,
                        :openedAt
                    )
                    ON CONFLICT (id) DO NOTHING
                    """,
            nativeQuery = true
    )
    int insertIfAbsent(
            @Param("id") UUID id,
            @Param("ownerId") UUID ownerId,
            @Param("balance") BigDecimal balance,
            @Param("currency") String currency,
            @Param("status") String status,
            @Param("openedAt") Instant openedAt
    );
}
