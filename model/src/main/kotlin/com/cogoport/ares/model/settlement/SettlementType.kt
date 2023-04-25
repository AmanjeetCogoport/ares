package com.cogoport.ares.model.settlement

enum class SettlementType(val dbValue: String) {
    SINV("SINV"),
    PINV("PINV"),
    SCN("SCN"),
    SDN("SDN"),
    PCN("PCN"),
    PDN("PDN"),
    REC("REC"),
    PAY("PAY"),
    SECH("SECH"),
    PECH("PECH"),
    VTDS("VTDS"),
    CTDS("CTDS"),
    NOSTRO("NOSTRO"),
    WOFF("WOFF"),
    ROFF("ROFF"),
    EXCH("EXCH"),
    JVNOS("JVNOS"),
    OUTST("OUTST"),
    SREIMB("SREIMB"),
    PREIMB("PREIMB"),
    ICJV("ICJV"),
    EXP("EXP"),
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
    MISC("MISC"),
    BANK("BANK"),
    CONTR("CONTR"),
    MTC("MTC"),
    INTER("INTER"),
    OPDIV("OPDIV"),
    MTCCV("MTCCV");

    open operator fun contains(value: String?): Boolean {
        for (c in SettlementType.values()) {
            if (c.equals(value)) {
                return true
            }
        }
        return false
    }
}
