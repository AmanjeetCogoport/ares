package com.cogoport.ares.api.payment.service.interfaces

interface GetInformation {
    suspend fun getCurrOutstanding(req: List<Long>): Long
}
