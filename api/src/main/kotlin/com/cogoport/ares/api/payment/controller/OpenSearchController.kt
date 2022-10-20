package com.cogoport.ares.api.payment.controller

import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.api.payment.model.OpenSearchListRequest
import com.cogoport.ares.api.payment.model.PushAccountUtilizationRequest
import com.cogoport.ares.api.payment.service.interfaces.OnAccountService
import com.cogoport.ares.api.payment.service.interfaces.OpenSearchService
import com.cogoport.ares.common.models.Response
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.validation.Validated
import jakarta.inject.Inject
import javax.validation.Valid

@Validated
@Controller("/opensearch")
class OpenSearchController {

    @Inject lateinit var onAccountService: OnAccountService

    @Inject
    lateinit var pushToClientService: OpenSearchService

    @Post("/account-utilization")
    suspend fun getAccountUtilizationRequest(@Valid @Body request: PushAccountUtilizationRequest): List<AccountUtilization> {
        return Response<List<AccountUtilization>>().ok(onAccountService.getDataAccUtilization(request))
    }

    @Post("/customer-outstanding")
    suspend fun addToOpenSearch(@Valid @Body request: OpenSearchListRequest) {
        return pushToClientService.pushOutstandingListData(request)
    }
}
