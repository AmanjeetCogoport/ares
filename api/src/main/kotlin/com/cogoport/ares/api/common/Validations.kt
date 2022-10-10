package com.cogoport.ares.api.common

import com.cogoport.ares.model.payment.AllCurrencyTypes

class Validations {
    companion object {
        fun checkForNumeral(value: String): Boolean {
            var pattern = Regex("[+-]?[0-9]+(\\.[0-9]+)?([Ee][+-]?[0-9]+)?")
            return pattern.matches(value.replace(",", ""))
        }

        fun checkForCurrency(value: String): Boolean {
            return AllCurrencyTypes.values().any { it.name == value }
        }

        fun checkForCustomerName(value: String): Boolean {
            var pattern = Regex("^[\\p{L} .'-]+$")
            return pattern.matches(value)
        }

        fun validateUTR(value: String): Boolean {
            var pattern = Regex("[a-zA-Z0-9]*")
            return pattern.matches(value)
        }

        fun checkLocalDate(value: String): Boolean {
            var pattern = Regex("^(20[0-9]{2})-(1[0-2]|0[1-9])-(3[01]|[12][0-9]|0[1-9])$")
            return pattern.matches(value)
        }
    }
}
