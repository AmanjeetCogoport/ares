package com.cogoport.ares.api.balances.entity

import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.DateUpdated
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Instant
import java.util.Date
import java.util.UUID

@MappedEntity("ledger_balances")
data class LedgerBalance(
    @field:Id @GeneratedValue var id: Long?,
    var tradePartyDetailId: UUID,
    var balanceAmount: BigDecimal,
    var balanceDate: Date,
    var ledgerCurrency: String,
    var entityId: UUID,
    var entityCode: Int,
    @DateCreated
    var createdAt: Timestamp? = Timestamp.from(Instant.now()),
    @DateUpdated
    var updatedAt: Timestamp? = Timestamp.from(Instant.now()),
)
