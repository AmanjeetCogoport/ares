package com.cogoport.ares.model.payment

enum class AccMode {
    AR, AP, OTHER, CSD, PDA, EMD, SUSS, SUSA, RE, PREF, EMP, RI, PC, VTDS,
}

enum class ServiceType {
    FCL_FREIGHT,
    LCL_FREIGHT,
    AIR_FREIGHT,
    FTL_FREIGHT,
    LTL_FREIGHT,
    HAULAGE_FREIGHT,
    FCL_CUSTOMS,
    AIR_CUSTOMS,
    LCL_CUSTOMS,
    NA,
    TRAILER_FREIGHT,
    STORE_ORDER,
    ADDITIONAL_CHARGE,
    FCL_CFS,
    ORIGIN_SERVICES,
    DESTINATION_SERVICES,
    FCL_CUSTOMS_FREIGHT,
    LCL_CUSTOMS_FREIGHT,
    AIR_CUSTOMS_FREIGHT,
    FCL_FREIGHT_LOCAL,
    DOMESTIC_AIR_FREIGHT,
    INSURANCE,
    EXPENSE,
    AIR_FREIGHT_LOCAL,
    RAIL_DOMESTIC_FREIGHT,
    PREMIUM_SERVICES,
    SUBSCRIPTION,
    SUBSCRIPTION_ADDON,
    TRUCKING
}

enum class ZoneCode {
    NORTH, SOUTH, EAST, WEST, VIETNAM
}

enum class AllCurrencyTypes {
    GBP, EUR, USD, INR;
}

enum class PaymentInvoiceMappingType {
    TDS, INVOICE, BILL
}

enum class DocStatus(val value: String) {
    PAID("Paid"), UNPAID("Unpaid"), PARTIAL_PAID("Partially Paid"), KNOCKED_OFF("Knocked Off"), UTILIZED("Utilized"), UNUTILIZED("Unutilized"), PARTIAL_UTILIZED("Partially Utilized")
}

enum class InvoiceType(val value: String) {
    SINV("Sales Invoice"),
    SCN("Sales Credit Note"),
    SDN("Sales Debit Note"),
    REC("Sales Payment"),
    PINV("Purchase Invoice"),
    PCN("Purchase Credit Note"),
    PDN("Purchase Debit Note"),
    PAY("Purchase Payment"),
    WOFF("Write Off Voucher"),
    ROFF("Round Off Voucher"),
    EXCH("Exchange Voucher"),
    OUTST("Outstanding Voucher"),
    JVNOS("Nostro Voucher"),
    SREIMB("Reimbursement Sales Invoice"),
    SREIMBCN("Reimbursement Sales Credit Note"),
    PREIMB("Reimbursement Purchase Invoice"),
    ICJV("Inter Company Journal Voucher"),
    DBTRC("Faltas pagamento"),
    STMNT("Relevés de factures"),
    RECPT("Payment Receipt"),
    CSTRV("Révision coût standard"),
    NEWPR("New Period"),
    SPDIR("Achats - Factures directes"),
    CLOSE("Fecho"),
    STOCK("Movimentos Stock"),
    GENAJ("Pagamento"),
    PREPY("Acomptes"),
    FAIS3("OD Inmo simulación IAS"),
    FAAR2("OD Imob anal."),
    MFIRC("Interface Produção - Recep."),
    CURVR("Desvio de conversão"),
    INTER("Miscellaneous entry"),
    FAS12("OD Imno simul. social & ana"),
    PAYRL("OD de nómina"),
    GENSM("Simulação Geral"),
    FAIR3("OD Imob IAS"),
    WIPCS("Em Curso"),
    MFIIS("Interface Produção"),
    BANK("Bank Entry"),
    MSCOP("Opérations Diverses"),
    RECJL("Pagos"),
    MFIOP("Interface Prod Oper."),
    FAR12("OD Imob. social & anal."),
    EXPNS("Notas de Despesa"),
    CSDIR("Прямые продажи (сф)"),
    CLOS1("Closing Year End / Legal"),
    GEN("Opérations diverses"),
    MISC("Miscellaneous entry"),
    OPDIV("Miscellaneous entry"),
    CONTR("CONTRA"),
    CRRCV("N/Créd. a receber"),
    CSINV("Customer Invoice"),
    ZSINV("Customer Invoice"),
    SPINV("Purchase Invoice"),
    SPMEM("Purchase Credit Note"),
    CSMEM("Customer Credit Note"),
    ZSMEM("Customer Credit Note"),
    RECJV("AR Payment Jv"),
    PAYJV("AP Payment Jv"),
    CTDS("Customer TDS"),
    VTDS("Vendor TDS");
}

/**
 * Used to identify the input of request which updates account utilization data from DB on Open Search
 */
enum class DocumentSearchType {
    NUMBER,
    VALUE
}

/**
 * Using for distinguishing on account and tds entry
 */
enum class DocType {
    PAYMENT,
    RECEIPT,
    TDS
}
