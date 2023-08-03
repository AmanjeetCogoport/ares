package com.cogoport.ares.model.payment.enum

enum class ShipmentDocumentName(val value: String) {
    MBL("bill_of_lading"),
    HBL("house_bill_of_lading"),
    MAWB("airway_bill"),
    HAWB("house_airway_bill"),
    DMBL("draft_bill_of_lading"),
    DHBL("draft_house_bill_of_lading"),
    DMAWB("draft_airway_bill"),
    DHAWB("draft_house_airway_bill"),
}
