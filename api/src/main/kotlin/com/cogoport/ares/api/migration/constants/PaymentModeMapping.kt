package com.cogoport.ares.api.migration.constants

import com.cogoport.ares.model.payment.PayMode

class PaymentModeMapping {

    companion object {
        private val map = mapOf("BNK" to PayMode.BANK, "CHQ" to PayMode.CHQ, "CSH" to PayMode.CASH, "IMPS" to PayMode.IMPS, "NEFT" to PayMode.NEFT, "RTGS" to PayMode.RTGS)

        fun getPayMode(key: String): PayMode? {
            return map.get(key)
        }
    }
}
