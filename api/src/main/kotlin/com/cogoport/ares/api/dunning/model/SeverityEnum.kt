package com.cogoport.ares.api.dunning.model

enum class SeverityEnum(val severity: String) {
    LOW(
        "<div style='text-align:left;' >We understand that" +
            " you must be pre-occupied and some of the below invoices" +
            " are overdue have probably escaped your notice.<br/></div>"
    ),
    MEDIUM(
        "<div style='text-align:left;' >It pains us to inform you that we are observing substantial Overdue" +
            " in outstanding payment of invoices generated against your firm for services taken through Cogoport.<br/><br/>" +
            " We understand that a firm with your reputation and clientele will strive to maintain highest standards of business" +
            " ethics and will look into this matter with utmost urgency. We hereby request you to clear the outstanding payment at" +
            " the earliest and would like to remind you that as per the agreement, all overdue invoices will attract a penalty interest" +
            " of <b>18% per annum</b>.<br/></div>"
    ),
    HIGH(
        "<div style='text-align:left;' > This communication is sent to you without prejudice.<br/><br/>" +
            " We have observed substantial Overdue in outstanding payment of invoices generated against your firm for services" +
            " taken through Cogoport and have tried reaching out to your good self on multiple occasions to resolve the same but to" +
            " no avail.</br><br/> As communicated earlier, you are hereby requested to clear the outstanding payment at the earliest to" +
            " avoid penal action against the same, please note, as per the agreement, your overdue invoices have started accruing a penalty" +
            " interest of <b>18% per annum</b>.<br/><br/>We once again request you to let us know of any discrepancy in the amount by reaching" +
            " out to our Receivables Manager, Mr Utkarsh Tiwari, on the below details:<br/> Email: <a href='mailto:utkarsh.tiwari@cogoport.com'>utkarsh.tiwari@cogoport.com</a>" +
            "<br/>Please beware, we shall be forced to initiate legal proceedings to recover our dues should the same remain unpaid any further.</div>"
    )
}
