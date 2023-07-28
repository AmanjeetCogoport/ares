package com.cogoport.ares.api.utils

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.model.PaymentStatus
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.Operator
import java.math.BigDecimal
import java.math.RoundingMode
import java.security.MessageDigest
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
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
            if (accType == AccountType.SINV || accType == AccountType.SCN || accType == AccountType.SDN ||
                accType == AccountType.PCN || accType == AccountType.PINV || accType == AccountType.PDN ||
                accType == AccountType.PREIMB || accType == AccountType.SREIMB || accType == AccountType.EXP || accType == AccountType.SREIMBCN
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

        /**
         * Get Financial in the format yyyy. If Financial year is 2022-2023, method will return 2223
         * @return String
         */
        fun getFinancialYear(): String {
            var fiscalYear: Int
            val currentYear = LocalDate.now().year
            val currentMonth = LocalDate.now().month
            fiscalYear = if (currentMonth <= Month.MARCH) {
                (currentYear)
            } else {
                (currentYear) + 1
            }
            return (fiscalYear - 1).toString().substring(2, 4) + (fiscalYear).toString().substring(2, 4)
        }

        fun getDateFromString(stringDate: String): String {
            try {
                val formattingStringdate = stringDate.replace('-', '/')
                val strDate = SimpleDateFormat(AresConstants.YEAR_DATE_FORMAT).format(SimpleDateFormat("yyyy/MM/dd").parse(formattingStringdate))
                return strDate
            } catch (ex: IllegalArgumentException) {
                ex.printStackTrace() // log write
                throw AresException(AresError.ERR_1003, "") // Invalid date format
            }
        }

        fun getPaymentStatus(accountUtilization: AccountUtilization): Pair<PaymentStatus, BigDecimal> {
            val balanceAmount = (
                accountUtilization.amountCurr.setScale(4, RoundingMode.HALF_UP).minus(accountUtilization.payCurr.setScale(4, RoundingMode.HALF_UP))
                ).setScale(4, RoundingMode.HALF_UP)
            if (balanceAmount.compareTo(BigDecimal.ZERO) == 0 || balanceAmount.abs().compareTo(BigDecimal("0.1")) <= 0) {
                return Pair(PaymentStatus.PAID, balanceAmount)
            } else if (balanceAmount.compareTo(BigDecimal.ZERO) > 0 && accountUtilization.payCurr.setScale(4, RoundingMode.HALF_UP).compareTo(0.toBigDecimal()) != 0) {
                return Pair(PaymentStatus.PARTIAL_PAID, balanceAmount)
            } else if (accountUtilization.payCurr.setScale(4, RoundingMode.HALF_UP).compareTo(BigDecimal.ZERO) == 0)
                return Pair(PaymentStatus.UNPAID, balanceAmount)
            return Pair(PaymentStatus.PAID, balanceAmount)
        }

        fun localDateTimeToTimeStamp(date: LocalDateTime): Timestamp {
            return try {
                Timestamp.valueOf(
                    DateTimeFormatter
                        .ofPattern("yyyy-MM-dd HH:mm:ss")
                        .withZone(ZoneOffset.UTC)
                        .format(date)
                )
            } catch (e: Exception) {
                Timestamp.valueOf(
                    DateTimeFormatter
                        .ofPattern("yyyy-MM-dd HH:mm:ss")
                        .withZone(ZoneOffset.UTC)
                        .format(Instant.now())
                )
            }
        }

        /**
         * Converts Date into LocalDateTime
         */
        fun dateIntoLocalDateTime(date: Date): LocalDateTime {
            return LocalDateTime.ofInstant(
                date.toInstant(),
                ZoneId.systemDefault()
            )
        }

        /**
         * Converts LocalDateTime into Date
         */
        fun localDateTimeIntoDate(ldt: LocalDateTime): Date {
            return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant())
        }

        fun getEncodedToken(objectId: String): String {
            val currentTime = ZonedDateTime.now()
            val timestampString = currentTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            val inputString = objectId + timestampString

            val messageDigest = MessageDigest.getInstance("SHA-256")
            val digestBytes = messageDigest.digest(inputString.toByteArray())

            val hexString = StringBuilder()
            for (byte in digestBytes) {
                val hex = String.format(Locale("en", "IN"), "%02x", byte.toInt() and 0xFF)
                hexString.append(hex)
            }
            return hexString.toString()
        }
    }
}
