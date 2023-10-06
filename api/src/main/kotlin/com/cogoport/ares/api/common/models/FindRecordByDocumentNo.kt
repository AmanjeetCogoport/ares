package com.cogoport.ares.api.common.models

import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountType
import com.fasterxml.jackson.annotation.JsonAutoDetect
import io.micronaut.core.annotation.Introspected

@Introspected
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class FindRecordByDocumentNo(
    var documentNo: Long,
    var documentValue: String? = null,
    var accType: AccountType? = null,
    var accMode: AccMode? = null
)
