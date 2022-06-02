package com.cogoport.ares.model.payment

enum class ServiceType {
    FCL_FREIGHT, LCL_FREIGHT, AIR_FREIGHT, FTL_FREIGHT, LTL_FREIGHT, HAULAGE_FREIGHT, FCL_CUSTOMS, AIR_CUSTOMS, LCL_CUSTOMS
}

enum class ZoneCode {
    NORTH, SOUTH, EAST, WEST
}

enum class AccType {
    SINV, PINV, SCN, SDN, PCN, PDN, REC, PAY;
}

enum class AllCurrencyTypes {
    GBP, EUR, USD, INR;
}
