package com.cogoport.ares.api.migration.model

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal

@Introspected
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class InvoiceDetails(
    @JsonProperty("invoice_type")
    val invoiceType: String?,
    @JsonProperty("ledger_total")
    val ledgerTotal: BigDecimal?,
    @JsonProperty("invoice_number")
    val invoiceNumber: String?,
    @JsonProperty("entity_code_num")
    val entityCodeNum: String?,
    @JsonProperty("sage_organization_id")
    val sageOrganizationId: String?,
    @JsonProperty("currency_amount_paid")
    val currencyAmountPaid: BigDecimal?,
    @JsonProperty("ledger_amount_paid")
    val ledgerAmountPaid: BigDecimal?,
    @JsonProperty("created_at")
    val createdAt: String?,
    @JsonProperty("updated_at")
    val updatedAt: String?
)
