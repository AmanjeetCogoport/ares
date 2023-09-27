package com.cogoport.ares.api.common.models

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity

@Introspected
@MappedEntity
data class EmailBankDetails(
    var bankName: String? = null,
    var accountNumber: String? = null,
    var beneficiaryName: String? = null,
    var ifscCode: String? = null,
    var swiftCode: String? = null,
)
