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
    ERR_1003("ERR_1003", "Mandatory field missing:", HttpStatus.BAD_REQUEST),
    ERR_1004("ERR_1004", "Invalid Quarter : ", HttpStatus.BAD_REQUEST),
    ERR_1005("ERR_1005", "Data not found", HttpStatus.NO_CONTENT),
    ERR_1006("ERR_1006", "Invalid Year : ", HttpStatus.BAD_REQUEST),
    ERR_1007("ERR_1007", "Record already deleted", HttpStatus.BAD_REQUEST),
    ERR_1010("ERR_1008", "Payment is already approved", HttpStatus.CONFLICT),
    ERR_1009("ERR_1009", "Invalid ", HttpStatus.BAD_REQUEST),
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
    ERR_1505("ERR_1503", "Exchange Rate not found for : ", HttpStatus.BAD_REQUEST),
    ERR_1506("ERR_1506", "Cross Trade Party AP/AR Settlement is not allowed.", HttpStatus.BAD_REQUEST),
    ERR_1507("ERR_1507", "Ares Service Not Reachable.", HttpStatus.SERVICE_UNAVAILABLE),
    ERR_1508("ERR_1508", "Plutus Service Not Reachable.", HttpStatus.SERVICE_UNAVAILABLE),
    ERR_1509("ERR_1509", "Hades Service Not Reachable.", HttpStatus.SERVICE_UNAVAILABLE),
    ERR_1510("ERR_1510", "Kuber Service Not Reachable.", HttpStatus.SERVICE_UNAVAILABLE),
    ERR_1511("ERR_1511", "Invalid File format: ", HttpStatus.BAD_REQUEST),
    ERR_1512("ERR_1512", "Settlement is not possible as some bills are present in payrun", HttpStatus.NOT_ACCEPTABLE),
    ERR_1513("ERR_1513", "Already Present", HttpStatus.BAD_REQUEST),
    ERR_1514("ERR_1514", "Sage organization not found", HttpStatus.BAD_REQUEST),
    ERR_1515("ERR_1515", "Settled Through UTR. You can not delete it.", HttpStatus.NOT_ACCEPTABLE),
    ERR_1516("ERR_1516", "JV is not been utilized", HttpStatus.BAD_REQUEST),
    ERR_1517("ERR_1517", "No GL Codes exists for this JV category: ", HttpStatus.BAD_REQUEST),
    ERR_1518("ERR_1518", "JV already posted", HttpStatus.BAD_REQUEST),
    ERR_1519("ERR_1519", "No Jv is present", HttpStatus.BAD_REQUEST),
    ERR_1520("ERR_1520", "JV is already", HttpStatus.NOT_ACCEPTABLE),
    ERR_1521("ERR_1521", "Suspense Account is not available for Account Payable", HttpStatus.BAD_REQUEST),
    ERR_1522("ERR_1522", "Cannot Post without a customer", HttpStatus.BAD_REQUEST),
    ERR_1523("ERR_1523", "Payment already posted", HttpStatus.BAD_REQUEST),
    ERR_1524("ERR_1524", "Payment is not approved", HttpStatus.BAD_REQUEST),
    ERR_1525("ERR_1525", "Payment entry already exist on sage", HttpStatus.NOT_ACCEPTABLE),
    ERR_1526("ERR_1526", "Unauthorised", HttpStatus.UNAUTHORIZED),
    ERR_1527("ERR_1527", "Credit Amount is not equal to Debit Amount", HttpStatus.NOT_ACCEPTABLE),
    ERR_1529("ERR_1529", "No value exists for this Acc Mode: ", HttpStatus.BAD_REQUEST),
    ERR_1530("ERR_1530", "No Organization is present with this Trade Party Id ", HttpStatus.BAD_REQUEST),
    ERR_1531("ERR_1531", "Documents must be posted on Sage", HttpStatus.BAD_REQUEST),
    ERR_1532("ERR_1532", "Posting failed because of following org issue:", HttpStatus.BAD_REQUEST),
    ERR_1533("ERR_1533", "Date format must be dd/MM/yyyy", HttpStatus.BAD_REQUEST),
    ERR_1534("ERR_1534", "Invalid Date Format : ", HttpStatus.BAD_REQUEST),
    ERR_1535("ERR_1535", "Payment must be posted", HttpStatus.BAD_REQUEST),
    ERR_1536("ERR_1536", "Payment is not posted from sage", HttpStatus.BAD_REQUEST),
    ERR_1537("ERR_1537", "UTR Number already exit with same UTR number:", HttpStatus.BAD_REQUEST),
    ERR_1538("ERR_1538", "You have selected Invalid Bank.", HttpStatus.BAD_REQUEST),
    ERR_1539("ERR_1539", "Payment not found.", HttpStatus.BAD_REQUEST),
    ERR_1540("ERR_1540", "You can not delete it.", HttpStatus.NOT_ACCEPTABLE),
    ERR_1541("ERR_1541", "Credit Controller Data not present.", HttpStatus.BAD_REQUEST),
    ERR_1542("ERR_1542", "Wrong Ageing Bucket Selected.", HttpStatus.BAD_REQUEST),
    ERR_1543("ERR_1543", "Wrong Service Type Selected.", HttpStatus.BAD_REQUEST),
    ERR_1544("ERR_1544", "Settlement has already been post to sage. You have to delete matching on sage.", HttpStatus.FORBIDDEN),
    ERR_1545("ERR_1545", "Dunning Cycle Execution Not Found for your request.", HttpStatus.BAD_REQUEST),
    ERR_1546("ERR_1546", "No payment with this utr exists.", HttpStatus.NOT_FOUND),
    ERR_1547("ERR_1547", "You can not post this settlement.", HttpStatus.FORBIDDEN),
    ERR_1548("ERR_1548", "Dunning Cycles have scheduled executions", HttpStatus.BAD_REQUEST),
    ERR_1549("ERR_1549", "Wrong Time string format, Please Send in this (HH:MM) format.", HttpStatus.BAD_REQUEST),
    ERR_1551("ERR_1551", "Please choose dunning schedule time after 1 Hour from now.", HttpStatus.BAD_REQUEST),
    ERR_1552("ERR_1552", "You can not delete this settlement as tds document has already been posted to sage", HttpStatus.FORBIDDEN);

    fun getMessage(param: String): String {
        if (param.isNotEmpty()) {
            return "$message $param"
        }
        return message
    }

    override fun toString(): String {
        return "$code: $message"
    }
}
