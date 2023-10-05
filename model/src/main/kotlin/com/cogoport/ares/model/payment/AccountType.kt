package com.cogoport.ares.model.payment

enum class AccountType(val dbValue: String) {
    SINV("SINV"),
    PINV("PINV"),
    SCN("SCN"),
    SDN("SDN"),
    PCN("PCN"),
    PDN("PDN"),
    REC("REC"),
    OPDIV("OPDIV"),
    MISC("MISC"),
    BANK("BANK"),
    CONTR("CONTR"),
    INTER("INTER"),
    MTC("MTC"),
    MTCCV("MTCCV"),
    WOFF("WOFF"),
    ROFF("ROFF"),
    EXCH("EXCH"),
    JVNOS("JVNOS"),
    OUTST("OUTST"),
    PAY("PAY"),
    SREIMB("SREIMB"),
    SREIMBCN("SREIMBCN"),
    PREIMB("PREIMB"),
    ICJV("ICJV"),
    GENSM("GENSM"),
    ZSMEP("ZSMEP"),
    PAYMT("PAYMT"),
    FAIS3("FAIS3"),
    SPDIR("SPDIR"),
    GEN("GEN"),
    NEWPR("NEWPR"),
    PAYRL("PAYRL"),
    STOCK("STOCK"),
    WIPCO("WIPCO"),
    RECP("RECP"),
    MSCOP("MSCOP"),
    FAS12("FAS12"),
    RECJL("RECJL"),
    CLOS1("CLOS1"),
    ZDN("ZDN"),
    UNRIN("UNRIN"),
    MFIRC("MFIRC"),
    PREPY("PREPY"),
    FIXDP("FIXDP"),
    FAIR3("FAIR3"),
    DBTRC("DBTRC"),
    WIPCS("WIPCS"),
    MFIOP("MFIOP"),
    CSTRV("CSTRV"),
    RECPT("RECPT"),
    CRRCV("CRRCV"),
    CLOSE("CLOSE"),
    ZSMFR("ZSMFR"),
    SAINV("SAINV"),
    RPI("RPI"),
    EXPNS("EXPNS"),
    MFIIS("MFIIS"),
    FAR12("FAR12"),
    CURVR("CURVR"),
    GENAJ("GENAJ"),
    ZSDN("ZSDN"),
    CSDIR("CSDIR"),
    STMNT("STMNT"),
    FAAR2("FAAR2"),
    EXP("EXP"),
    VTDS("VTDS"),
    CTDS("CTDS"),
    CSINV("CSINV"),
    ZSINV("ZSINV"),
    SPINV("SPINV"),
    SPMEM("SPMEM"),
    CSMEM("CSMEM"),
    ZSMEM("ZSMEM"),
    RECJV("RECJV"),
    PAYJV("PAYJV"),
    CLOSING("CLOSING");

    open operator fun contains(value: String?): Boolean {
        for (c in AccountType.values()) {
            if (c.equals(value)) {
                return true
            }
        }
        return false
    }
}

enum class PaymentCode {
    PAY, REC, CTDS, VTDS, CPRE, APRE
}

enum class Operator {
    DIVIDE, MULTIPLY, SUBTRACT, ADD
}
