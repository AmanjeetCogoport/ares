package com.cogoport.ares.model.settlement

import com.cogoport.ares.model.payment.InvoiceStatus
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Instant

@JsonInclude
@ReflectiveAccess
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@Introspected
data class CheckDocument(
    var id: Long,
    var documentNo: Long,
    var accountType: SettlementType,
    var documentAmount: BigDecimal = 0.toBigDecimal(),
    var tds: BigDecimal?,
    var afterTdsAmount: BigDecimal = 0.toBigDecimal(),
    var balanceAmount: BigDecimal = 0.toBigDecimal(),
    var allocationAmount: BigDecimal = 0.toBigDecimal(),
    var balanceAfterAllocation: BigDecimal = 0.toBigDecimal(),
    var ledgerAmount: BigDecimal,
    var status: InvoiceStatus?,
    var signFlag: Int,
    var settledAmount: BigDecimal = 0.toBigDecimal(),
    var currency: String = "INR",
    var legCurrency: String = "INR",
    var exchangeRate: BigDecimal,
    var transactionDate: Timestamp = Timestamp.from(Instant.now()),
    var settledTds: BigDecimal = 0.toBigDecimal()
)
