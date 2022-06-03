package com.cogoport.ares.api.payment.entity

import io.micronaut.data.annotation.MappedEntity

@MappedEntity
data class OrgOutstanding(
    val organizationId: String?,
    val organizationName: String?,
    val currency: String?,
    val openInvoicesCount: Int?,
    val openInvoicesAmount: Float?,
    val paymentsCount: Int?,
    val paymentsAmount: Float?,
    val outstandingAmount: Float?,
    val zoneCode: String?
)
