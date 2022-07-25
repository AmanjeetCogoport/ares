package com.cogoport.ares.api.exception

import io.micronaut.http.HttpStatus

enum class AresError(
    val code: String,
    val message: String,
    val httpStatus: HttpStatus
) {
    ERR_1000("ERR_1000", "Service Unavailable. Please contact admin", HttpStatus.SERVICE_UNAVAILABLE),
    ERR_1008("ERR_1008", "Resource not found", HttpStatus.NOT_FOUND),
    ERR_1001("ERR_1001", "Something went wrong. Please contact admin", HttpStatus.SERVICE_UNAVAILABLE),
    ERR_1002("ERR_1002", "Not found", HttpStatus.NOT_FOUND),
    ERR_1003("ERR_1003", "Mandatory field missing : ", HttpStatus.BAD_REQUEST),
    ERR_1004("ERR_1004", "Invalid Quarter : ", HttpStatus.BAD_REQUEST),
    ERR_1005("ERR_1005", "Data not found", HttpStatus.NO_CONTENT),
    ERR_1006("ERR_1006", "Invalid Year : ", HttpStatus.BAD_REQUEST),
    ERR_1007("ERR_1007", "Record already deleted", HttpStatus.BAD_REQUEST),
    ERR_1201("ERR_12101", "Document number already exists", HttpStatus.BAD_REQUEST),
    ERR_1203("ERR_1203", "Invalid date format", HttpStatus.BAD_REQUEST),
    ERR_1202("ERR_1202", "Invalid account type for invoice", HttpStatus.BAD_REQUEST),
    ERR_1204("ERR_1204", "Document with final status cannot be modified/deleted : ", HttpStatus.FORBIDDEN),
    ERR_1205("ERR_1205", "No sign found for this account type", HttpStatus.BAD_REQUEST),
    ERR_1206("ERR_1206", "Bank information not found", HttpStatus.NOT_FOUND),
    ERR_1207("ERR_1207", "Organization Serial does not exist", HttpStatus.BAD_REQUEST),
    ERR_1501("ERR_1501", "No credit document found", HttpStatus.BAD_REQUEST),
    ERR_1502("ERR_1502", "No debit document found", HttpStatus.BAD_REQUEST),
    ERR_1503("ERR_1503", "Document does not exist : ", HttpStatus.BAD_REQUEST),
    ERR_1504("ERR_1504", "Document Overpaid", HttpStatus.NOT_MODIFIED),
    ERR_1505("ERR_1505", "New Tds cannot be greater than previous value", HttpStatus.NOT_IMPLEMENTED);

    fun getMessage(param: String): String {
        if (param.isNotEmpty()) {
            return message + param
        }
        return message
    }

    override fun toString(): String {
        return "$code: $message"
    }
}
