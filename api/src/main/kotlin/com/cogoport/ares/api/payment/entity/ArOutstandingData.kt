package com.cogoport.ares.api.payment.entity

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.util.UUID

@MappedEntity
data class ArOutstandingData(
    @JsonProperty("organization_id")
    val organizationId: UUID,
    @JsonProperty("entity_code")
    val entityCode: Int,
    @JsonProperty("led_currency")
    val ledCurrency: String,
    @JsonProperty("invoice_not_due_amount")
    val invoiceNotDueAmount: BigDecimal,
    @JsonProperty("invoice_thirty_amount")
    val invoiceThirtyAmount: BigDecimal,
    @JsonProperty("invoice_sixty_amount")
    val invoiceSixtyAmount: BigDecimal,
    @JsonProperty("invoice_ninety_amount")
    val invoiceNinetyAmount: BigDecimal,
    @JsonProperty("invoice_one_eighty_amount")
    val invoiceOneEightyAmount: BigDecimal,
    @JsonProperty("invoice_three_sixty_five_amount")
    val invoiceThreeSixtyFiveAmount: BigDecimal,
    @JsonProperty("invoice_three_sixty_five_plus_amount")
    val invoiceThreeSixtyFivePlusAmount: BigDecimal,
    @JsonProperty("invoice_not_due_count")
    val invoiceNotDueCount: Long,
    @JsonProperty("invoice_thirty_count")
    val invoiceThirtyCount: Long,
    @JsonProperty("invoice_sixty_count")
    val invoiceSixtyCount: Long,
    @JsonProperty("invoice_ninety_count")
    val invoiceNinetyCount: Long,
    @JsonProperty("invoice_one_eighty_count")
    val invoiceOneEightyCount: Long,
    @JsonProperty("invoice_three_sixty_five_count")
    val invoiceThreeSixtyFiveCount: Long,
    @JsonProperty("invoice_three_sixty_five_plus_count")
    val invoiceThreeSixtyFivePlusCount: Long,
    @JsonProperty("on_account_not_due_amount")
    val onAccountNotDueAmount: BigDecimal,
    @JsonProperty("on_account_thirty_amount")
    val onAccountThirtyAmount: BigDecimal,
    @JsonProperty("on_account_sixty_amount")
    val onAccountSixtyAmount: BigDecimal,
    @JsonProperty("on_account_ninety_amount")
    val onAccountNinetyAmount: BigDecimal,
    @JsonProperty("on_account_one_eighty_amount")
    val onAccountOneEightyAmount: BigDecimal,
    @JsonProperty("on_account_three_sixty_five_amount")
    val onAccountThreeSixtyFiveAmount: BigDecimal,
    @JsonProperty("on_account_three_sixty_five_plus_amount")
    val onAccountThreeSixtyFivePlusAmount: BigDecimal,
    @JsonProperty("on_account_not_due_count")
    val onAccountNotDueCount: Long,
    @JsonProperty("on_account_thirty_count")
    val onAccountThirtyCount: Long,
    @JsonProperty("on_account_sixty_count")
    val onAccountSixtyCount: Long,
    @JsonProperty("on_account_ninety_count")
    val onAccountNinetyCount: Long,
    @JsonProperty("on_account_one_eighty_amount")
    val onAccountOneEightyCount: Long,
    @JsonProperty("on_account_three_sixty_five_count")
    val onAccountThreeSixtyFiveCount: Long,
    @JsonProperty("on_account_three_sixty_five_plus_count")
    val onAccountThreeSixtyFivePlusCount: Long,
    @JsonProperty("not_due_outstanding")
    val notDueOutstanding: BigDecimal,
    @JsonProperty("thirty_outstanding")
    val thirtyOutstanding: BigDecimal,
    @JsonProperty("sixty_outstanding")
    val sixtyOutstanding: BigDecimal,
    @JsonProperty("ninety_outstanding")
    val ninetyOutstanding: BigDecimal,
    @JsonProperty("one_eighty_outstanding")
    val oneEightyOutstanding: BigDecimal,
    @JsonProperty("three_sixty_five_outstanding")
    val threeSixtyFiveOutstanding: BigDecimal,
    @JsonProperty("three_sixty_five_plus_outstanding")
    val threeSixtyFivePlusOutstanding: BigDecimal,
    @JsonProperty("total_open_invoice_amount")
    val totalOpenInvoiceAmount: BigDecimal,
    @JsonProperty("total_open_credit_note_amount")
    val totalOpenCreditNoteAmount: BigDecimal,
    @JsonProperty("total_open_on_account_amount")
    val totalOpenOnAccountAmount: BigDecimal,
    @JsonProperty("total_outstanding")
    val totalOutstanding: BigDecimal,
    @JsonProperty("credit_note_not_due_amount")
    val creditNoteNotDueAmount: BigDecimal,
    @JsonProperty("credit_note_thirty_amount")
    val creditNoteThirtyAmount: BigDecimal,
    @JsonProperty("credit_note_sixty_amount")
    val creditNoteSixtyAmount: BigDecimal,
    @JsonProperty("credit_note_ninety_amount")
    val creditNoteNinetyAmount: BigDecimal,
    @JsonProperty("credit_note_one_eighty_amount")
    val creditNoteOneEightyAmount: BigDecimal,
    @JsonProperty("credit_note_three_sixty_five_amount")
    val creditNoteThreeSixtyFiveAmount: BigDecimal,
    @JsonProperty("credit_note_three_sixty_five_plus_amount")
    val creditNoteThreeSixtyFivePlusAmount: BigDecimal,
    @JsonProperty("credit_note_not_due_count")
    val creditNoteNotDueCount: Long,
    @JsonProperty("credit_note_thirty_count")
    val creditNoteThirtyCount: Long,
    @JsonProperty("credit_note_sixty_count")
    val creditNoteSixtyCount: Long,
    @JsonProperty("credit_note_ninety_count")
    val creditNoteNinetyCount: Long,
    @JsonProperty("credit_note_one_eighty_count")
    val creditNoteOneEightyCount: Long,
    @JsonProperty("credit_note_three_sixty_five_count")
    val creditNoteThreeSixtyFiveCount: Long,
    @JsonProperty("credit_note_three_sixty_five_plus_count")
    val creditNoteThreeSixtyFivePlusCount: Long
)
