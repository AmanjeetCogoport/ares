package com.cogoport.ares.api.utils

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.Operator
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.Timestamp
import kotlin.math.ceil
import kotlin.math.roundToInt

class Utilities {
    companion object Utils {
        fun getTimeStampFromString(stringDate: String): Timestamp {

            try {
                val year = stringDate.substring(0, 4)
                val month = stringDate.substring(5, 7)
                val day = stringDate.substring(8, 10)

                val finalDate = "$year-$month-$day 00:00:00"
                return Timestamp.valueOf(finalDate)
            } catch (ex: IllegalArgumentException) {
                ex.printStackTrace() // log write
                throw AresException(AresError.ERR_1003, "date") // Invalid date format
            }
        }

        fun isInvoiceAccountType(accType: AccountType): Boolean {
            if (accType == AccountType.SINV || accType == AccountType.SCN || accType == AccountType.SDN || accType == AccountType.PCN ||
                accType == AccountType.PINV || accType == AccountType.PDN
            ) {
                return true
            }
            return false
        }

        fun isPayAccountType(accType: AccountType): Boolean {
            if (accType == AccountType.REC || accType == AccountType.PAY) {
                return true
            }
            return false
        }

        fun binaryOperation(operandOne: BigDecimal, operandTwo: BigDecimal, operation: Operator): BigDecimal {
            return when (operation) {
                Operator.DIVIDE -> { operandOne.divide(operandTwo, AresConstants.DECIMAL_NUMBER_UPTO, RoundingMode.HALF_DOWN) }
                Operator.MULTIPLY -> { operandOne.multiply(operandTwo).setScale(AresConstants.DECIMAL_NUMBER_UPTO, RoundingMode.HALF_DOWN) }
                Operator.ADD -> { operandOne.add(operandTwo).setScale(AresConstants.DECIMAL_NUMBER_UPTO, RoundingMode.HALF_DOWN) }
                Operator.SUBTRACT -> { operandOne.subtract(operandTwo).setScale(AresConstants.DECIMAL_NUMBER_UPTO, RoundingMode.HALF_DOWN) }
                else -> { throw Exception("Operation do not match") }
            }
        }

        fun decimalRound(amount: BigDecimal, digits: Int = AresConstants.ROUND_DECIMAL_TO, roundingMode: RoundingMode = RoundingMode.HALF_DOWN): BigDecimal {
            return amount.setScale(digits, roundingMode)
        }

        /**
         * Get Total pages from page size and total records
         * @param totalRows
         * @param pageSize
         * @return totalPages
         */
        fun getTotalPages(totalRows: Long, pageSize: Int): Long {

            return try {
                val totalPageSize = if (pageSize > 0) pageSize else 1
                ceil((totalRows.toFloat() / totalPageSize.toFloat()).toDouble()).roundToInt().toLong()
            } catch (e: Exception) {
                0
            }
        }
    }
}
