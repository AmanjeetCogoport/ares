package com.cogoport.ares.api.dunning.controller

import com.cogoport.ares.api.dunning.model.request.CreateDunningException
import com.cogoport.ares.api.dunning.model.request.ListExceptionReq
import com.cogoport.ares.api.dunning.model.response.CycleWiseExceptionResp
import com.cogoport.ares.api.dunning.model.response.MasterExceptionResp
import com.cogoport.ares.api.dunning.service.interfaces.DunningService
import com.cogoport.ares.common.models.Response
import com.cogoport.ares.model.common.ResponseList
import com.cogoport.ares.model.dunning.request.CreateDunningCycleRequest
import com.cogoport.ares.model.dunning.request.CreditControllerRequest
import com.cogoport.ares.model.dunning.request.DunningCycleFilters
import com.cogoport.ares.model.dunning.request.ListDunningCycleExecutionReq
import com.cogoport.ares.model.dunning.request.UpdateCreditControllerRequest
import com.cogoport.ares.model.dunning.response.CustomerOutstandingAndOnAccountResponse
import com.cogoport.ares.model.dunning.response.DunningCycleExecutionResponse
import com.cogoport.brahma.hashids.Hashids
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.annotation.QueryValue
import io.micronaut.validation.Validated
import java.util.UUID
import javax.validation.Valid

@Validated
@Controller("/dunning")
class DunningController(
    private val dunningService: DunningService
) {

    @Post("/cycle")
    suspend fun createDunningCycle(
        @Valid @Body
        createDunningCycleRequest: CreateDunningCycleRequest
    ): String {
        return Response<String>().ok(Hashids.encode(dunningService.createDunningCycle(createDunningCycleRequest)))
    }

    @Post("/credit-controller")
    suspend fun createCreditController(
        @Valid @Body
        creditControllerRequest: CreditControllerRequest
    ): String {
        return Response<String>().ok(Hashids.encode(dunningService.createCreditController(creditControllerRequest)))
    }

    @Put("/credit-controller")
    suspend fun updateCreditController(
        @Valid @Body
        updateCreditControllerRequest: UpdateCreditControllerRequest
    ): String {
        return Response<String>().ok(Hashids.encode(dunningService.updateCreditController(updateCreditControllerRequest)))
    }

    @Get("/customer-outstanding-and-on-account{?request*}")
    suspend fun getCustomersOutstandingAndOnAccount(
        request: DunningCycleFilters
    ): List<CustomerOutstandingAndOnAccountResponse> {
        return Response<List<CustomerOutstandingAndOnAccountResponse>>().ok(
            dunningService.getCustomersOutstandingAndOnAccount(request)
        )
    }

    @Get("/dunning-cycle-execution{?request*}")
    suspend fun listDunningCycleExecution(
        request: ListDunningCycleExecutionReq
    ): ResponseList<DunningCycleExecutionResponse> {
        return Response<ResponseList<DunningCycleExecutionResponse>>().ok(dunningService.listDunningCycleExecution(request))
    }

    @Get("/master-exceptions{?request*}")
    suspend fun listMasterException(@Valid request: ListExceptionReq): ResponseList<MasterExceptionResp> {
        return dunningService.listMasterException(request)
    }

    @Post("/create-exceptions")
    suspend fun createException(@Valid @Body request: CreateDunningException): MutableList<String> {
        return dunningService.createDunningException(request)
    }

    @Get("/cycle-exception{?request*}")
    suspend fun getCycleWiseExceptions(@Valid request: ListExceptionReq): ResponseList<CycleWiseExceptionResp> {
        return dunningService.getCycleWiseExceptions(request)
    }

//    @Get("list-dunning{?request*}")
//    suspend fun listDunningCycles(@Valid request: ListDunningCycleReq): ResponseList<ListDunningCycleResp> {
//        return dunningService.listDunningCycles(request)
//    }

    @Post("delete-master-exception")
    suspend fun deleteOrUpdateMasterException(
        @QueryValue("id") id: String,
        @QueryValue("updatedBy") updatedBy: UUID,
        @QueryValue("actionType") actionType: String
    ): Boolean {
        return dunningService.deleteOrUpdateMasterException(id, updatedBy, actionType)
    }
}
