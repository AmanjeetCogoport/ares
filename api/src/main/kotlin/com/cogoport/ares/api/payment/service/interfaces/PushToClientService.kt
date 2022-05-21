package com.cogoport.ares.api.payment.service.interfaces

interface PushToClientService {

    suspend fun pushDataToOpenSearch(zone: String?)

}