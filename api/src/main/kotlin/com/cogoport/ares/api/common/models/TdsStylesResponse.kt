package com.cogoport.ares.api.common.models

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.util.UUID

data class TdsStylesResponse(
    @JsonProperty("id")
    var id: UUID,
    @JsonProperty("tds_deduction_style")
    var tdsDeductionStyle: String?,
    @JsonProperty("tds_deduction_type")
    var tdsDeductionType: String?,
    @JsonProperty("tds_deduction_rate")
    var tdsDeductionRate: BigDecimal?,
)
