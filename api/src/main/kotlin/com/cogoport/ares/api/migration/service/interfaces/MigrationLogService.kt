package com.cogoport.ares.api.migration.service.interfaces

import java.math.BigDecimal

interface MigrationLogService {
    suspend fun saveMigrationLogs(
        paymentId: Long?,
        accUtilId: Long?,
        paymentNum: String?,
        currency: String?,
        currencyAmount: BigDecimal?,
        ledgerAmount: BigDecimal?,
        bankPayAmount: BigDecimal?,
        accountUtilCurrAmount: BigDecimal?,
        accountUtilLedAmount: BigDecimal?,
        errorMessage: String?
    )
    suspend fun saveMigrationLogs(paymentId: Long?, accUtilId: Long?, ex: String?, paymentNum: String?)

}
