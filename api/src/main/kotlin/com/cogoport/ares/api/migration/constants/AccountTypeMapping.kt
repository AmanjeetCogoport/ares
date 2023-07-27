package com.cogoport.ares.api.migration.constants

class AccountTypeMapping {
    companion object {
        private val sageAccountTypeMapping = mapOf(
            "SPINV" to "PINV",
            "ZSINV" to "SINV",
            "VTDS" to "VTDS"
        )

        fun getAccountType(sageAccountType: String): String {
            return sageAccountTypeMapping[sageAccountType] ?: sageAccountType
        }
    }
}
