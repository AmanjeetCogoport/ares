package com.cogoport.ares.api.payment.model.response

import com.cogoport.ares.api.payment.entity.BfCustomerProfitabilityResp
import com.cogoport.ares.api.payment.entity.BfShipmentProfitabilityResp
import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal

@Introspected
data class ShipmentProfitResp(
    var shipmentList: List<BfShipmentProfitabilityResp>? = null,
    var customerList: List<BfCustomerProfitabilityResp>? = null,
    var averageShipmentProfit: BigDecimal? = 0.toBigDecimal(),
    var averageCustomerProfit: BigDecimal? = 0.toBigDecimal(),
    var pageIndex: Int? = 1,
    var pageSize: Int? = 10,
    var totalRecord: Long? = 0,
    var currency: String? = "INR"
)
