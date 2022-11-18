package com.cogoport.ares.api.migration.constants

import com.cogoport.ares.model.settlement.SettlementType

class SettlementTypeMigration {
    companion object {
        val settlementTypeMapping = mapOf(
            "SPINV" to "PINV",
            "SPMEM" to "PCN",
            "ZSINV" to "SINV",
            "NOSTR" to "NOSTRO",
            "PAY" to "PAY",
            "REC" to "REC",
            "OPDIV" to "JVNOS"
        )
        fun getSettlementType(settlementType: String): SettlementType {
            return SettlementType.valueOf(settlementTypeMapping[settlementType]!!)
        }
    }
}
