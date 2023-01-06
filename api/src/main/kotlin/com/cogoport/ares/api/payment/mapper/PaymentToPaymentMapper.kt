package com.cogoport.ares.api.payment.mapper

import com.cogoport.ares.api.payment.entity.Payment
import com.cogoport.ares.api.payment.entity.SuspenseAccount
import com.cogoport.ares.model.payment.response.PaymentResponse
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.ReportingPolicy

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface PaymentToPaymentMapper {
    @Mapping(source = "entityType", target = "entityCode")
    @Mapping(source = "remarks", target = "narration")
    @Mapping(source = "utr", target = "transRefNumber")
    @Mapping(source = "bankAccountNumber", target = "cogoAccountNo")
    fun convertToEntity(payment: com.cogoport.ares.model.payment.Payment): Payment

    @Mapping(source = "entityCode", target = "entityType")
    @Mapping(source = "narration", target = "remarks")
    @Mapping(source = "transRefNumber", target = "utr")
    @Mapping(source = "cogoAccountNo", target = "bankAccountNumber")
    fun convertToModel(payment: Payment): com.cogoport.ares.model.payment.Payment

    @Mapping(source = "entityType", target = "entityCode")
    @Mapping(source = "remarks", target = "narration")
    @Mapping(source = "utr", target = "transRefNumber")
    @Mapping(source = "bankAccountNumber", target = "cogoAccountNo")
    @Mapping(source = "ledCurrency", target = "ledgerCurrency")
    @Mapping(source = "ledAmount", target = "ledgerAmount")
    @Mapping(source = "payMode", target = "paymentMode")
    @Mapping(source = "tradePartyDocument", target = "tradePartyDocumentUrl")
    fun convertPaymentToSuspenseEntity(payment: com.cogoport.ares.model.payment.Payment): SuspenseAccount

    @Mapping(source = "entityCode", target = "entityType")
    @Mapping(source = "narration", target = "remarks")
    @Mapping(source = "transRefNumber", target = "utr")
    @Mapping(source = "cogoAccountNo", target = "bankAccountNumber")
    @Mapping(target = "organizationName", constant = "Suspense Account")
    @Mapping(target = "isPosted", constant = "false")
    @Mapping(target = "isSuspense", constant = "true")
    @Mapping(target = "accMode", constant = "AR")
    @Mapping(target = "ledCurrency", source = "ledgerCurrency")
    @Mapping(target = "ledAmount", source = "ledgerAmount")
    @Mapping(target = "payMode", source = "paymentMode")
    @Mapping(source = "tradePartyDocumentUrl", target = "tradePartyDocument")
    fun convertSuspenseEntityToPaymentResponse(suspenseEntities: SuspenseAccount): PaymentResponse

    fun convertSuspenseEntityListToPaymentResponse(suspenseEntities: List<SuspenseAccount>): List<PaymentResponse>
}
