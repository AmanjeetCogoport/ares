package com.cogoport.ares.api.common.models

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@Introspected
@MappedEntity
data class ARLedgerJobDetailsResponse(
    @JsonProperty("Transaction Date")
    val transactionDate: String?,
    @JsonProperty("Document Type")
    val documentType: String?,
    @JsonProperty("Document Number")
    val documentNumber: String,
    @JsonProperty("Original Transaction Currency")
    val currency: String?,
    @JsonProperty("Original Transaction Amount")
    val amount: String?,
    @JsonProperty("Ledger Debit")
    val debit: BigDecimal,
    @JsonProperty("Ledger Credit")
    val credit: BigDecimal,
    @JsonProperty("Transaction Ref Number")
    val transactionRefNumber: String?,
    @JsonProperty("shipmentDocumentNumber")
    var shipmentDocumentNumber: String?,
    @JsonProperty("houseDocumentNumber")
    var houseDocumentNumber: String?

)
