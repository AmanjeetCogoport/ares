package com.cogoport.ares.model.payment.response

data class DueCountResponse(
    val dueCount: Long,
    val overdueCount: Long,
    val proformaCount: Long,
    val thirtyCount: Int,
    val sixtyCount: Int,
    val ninetyCount: Int,
    val ninetyPlus: Int,
)
