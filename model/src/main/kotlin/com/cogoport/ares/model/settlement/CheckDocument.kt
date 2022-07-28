package com.cogoport.ares.model.settlement

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Instant

@JsonInclude
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@Introspected
data class CheckDocument(
    var id: Long,
    var documentNo: Long,
    var documentValue: String,
    var accountType: SettlementType,
    var documentAmount: BigDecimal = 0.toBigDecimal(),
    var tds: BigDecimal?,
    var afterTdsAmount: BigDecimal = 0.toBigDecimal(),
    var balanceAmount: BigDecimal = 0.toBigDecimal(),
    var allocationAmount: BigDecimal = 0.toBigDecimal(),
    var currentBalance: BigDecimal?,
    var balanceAfterAllocation: BigDecimal = 0.toBigDecimal(),
    var ledgerAmount: BigDecimal,
    var status: String?,
    var settledAllocation: BigDecimal = 0.toBigDecimal(),
    var currency: String = "INR",
    var ledCurrency: String = "INR",
    var exchangeRate: BigDecimal,
    var transactionDate: Timestamp = Timestamp.from(Instant.now()),
    var settledTds: BigDecimal = 0.toBigDecimal(),
    var settledAmount: BigDecimal?
)
