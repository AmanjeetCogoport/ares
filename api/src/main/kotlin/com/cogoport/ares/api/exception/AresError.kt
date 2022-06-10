package com.cogoport.ares.api.exception

import io.micronaut.http.HttpStatus

enum class AresError(
    var code: String,
    var message: String,
    var httpStatus: HttpStatus
) {
    ERR_1001("ERR_1001", "Something went wrong. Please contact admin", HttpStatus.SERVICE_UNAVAILABLE),
    ERR_1002("ERR_1002", "Not found", HttpStatus.NOT_FOUND),
    ERR_1003("ERR_1003", "Mandatory field missing : ", HttpStatus.BAD_REQUEST),
    ERR_1201("ERR_12101", "Document number already exists", HttpStatus.BAD_REQUEST),
    ERR_1004("ERR_1004", "Invalid Quarter : ", HttpStatus.BAD_REQUEST),
    ERR_1005("ERR_1005", "Data not found", HttpStatus.NO_CONTENT),
    ERR_1203("ERR_1203", "Invalid date format", HttpStatus.BAD_REQUEST),
    ERR_1202("ERR_1202", "Invalid account type for invoice", HttpStatus.BAD_REQUEST),
    ERR_1007("ERR_1007", "Record already deleted", HttpStatus.BAD_REQUEST),
    ERR_1006("ERR_1006", "Invalid Year : ", HttpStatus.BAD_REQUEST),
    ERR_1204("ERR_1204", "Document with final status cannot be modified/deleted : ", HttpStatus.FORBIDDEN);

    fun getMessage(param: String): String {
        if (param.isNotEmpty()) {
            message += param
        }
        return message
    }

    override fun toString(): String {
        return "$code: $message"
    }
}
