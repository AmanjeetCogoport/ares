package com.cogoport.ares.api.dunning.service.interfaces

interface EmailService {
    suspend fun sendEmailForIrnGeneration(invoiceId: Long)
}
