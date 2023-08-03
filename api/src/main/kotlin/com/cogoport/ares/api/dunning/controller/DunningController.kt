package com.cogoport.ares.api.dunning.controller

import com.cogoport.ares.api.dunning.model.request.CreateDunningException
import com.cogoport.ares.api.dunning.model.request.ListDunningCycleReq
import com.cogoport.ares.api.dunning.model.request.ListExceptionReq
import com.cogoport.ares.api.dunning.model.response.CycleWiseExceptionResp
import com.cogoport.ares.api.dunning.model.response.MasterExceptionResp
import com.cogoport.ares.api.dunning.service.interfaces.DunningHelperService
import com.cogoport.ares.api.dunning.service.interfaces.DunningService
import com.cogoport.ares.common.models.Response
import com.cogoport.ares.model.common.ResponseList
import com.cogoport.ares.model.dunning.request.CreateDunningCycleRequest
import com.cogoport.ares.model.dunning.request.CreateUserRequest
import com.cogoport.ares.model.dunning.request.DunningCycleFilters
import com.cogoport.ares.model.dunning.request.DunningScheduleRule
import com.cogoport.ares.model.dunning.request.ListDunningCycleExecutionReq
import com.cogoport.ares.model.dunning.request.ListOrganizationStakeholderRequest
import com.cogoport.ares.model.dunning.request.MonthWiseStatisticsOfAccountUtilizationReuest
import com.cogoport.ares.model.dunning.request.OverallOutstandingAndOnAccountRequest
import com.cogoport.ares.model.dunning.request.SendMailOfAllCommunicationToTradePartyReq
import com.cogoport.ares.model.dunning.request.SyncOrgStakeholderRequest
import com.cogoport.ares.model.dunning.request.UpdateCycleExecutionRequest
import com.cogoport.ares.model.dunning.request.UpdateDunningCycleStatusReq
import com.cogoport.ares.model.dunning.response.CreditControllerResponse
import com.cogoport.ares.model.dunning.response.CustomerOutstandingAndOnAccountResponse
import com.cogoport.ares.model.dunning.response.DunningCardData
import com.cogoport.ares.model.dunning.response.DunningCycleExecutionResponse
import com.cogoport.ares.model.dunning.response.DunningCycleResponse
import com.cogoport.ares.model.dunning.response.MonthWiseStatisticsOfAccountUtilizationResponse
import com.cogoport.ares.model.dunning.response.OverallOutstandingAndOnAccountResponse
import com.cogoport.brahma.hashids.Hashids
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.annotation.QueryValue
import io.micronaut.validation.Validated
import java.util.Date
import java.util.UUID
import javax.validation.Valid

@Validated
@Controller("/dunning")
class DunningController(
    private val dunningService: DunningService,
    private val dunningHelperService: DunningHelperService
) {

    @Post("/cycle")
    suspend fun createDunningCycle(
        @Valid @Body
        createDunningCycleRequest: CreateDunningCycleRequest
    ): String {
        return Response<String>().ok(Hashids.encode(dunningService.createDunningCycle(createDunningCycleRequest)))
    }

    @Post("/sync-organization-stakeholders")
    suspend fun syncOrgStakeholders(
        @Valid @Body
        creditControllerRequest: SyncOrgStakeholderRequest
    ): String {
        return Response<String>().ok(Hashids.encode(dunningService.syncOrgStakeholders(creditControllerRequest)))
    }

    @Get("/customer-outstanding-and-on-account{?request*}")
    suspend fun getCustomersOutstandingAndOnAccount(
        request: DunningCycleFilters
    ): ResponseList<CustomerOutstandingAndOnAccountResponse> {
        return Response<ResponseList<CustomerOutstandingAndOnAccountResponse>>().ok(
            dunningService.getCustomersOutstandingAndOnAccount(request)
        )
    }

    @Get("/list-dunning-cycle-execution{?request*}")
    suspend fun listDunningCycleExecution(
        request: ListDunningCycleExecutionReq
    ): ResponseList<DunningCycleExecutionResponse> {
        return Response<ResponseList<DunningCycleExecutionResponse>>().ok(dunningService.listDunningCycleExecution(request))
    }

    @Get("list-dunning{?request*}")
    suspend fun listDunningCycles(@Valid request: ListDunningCycleReq): ResponseList<DunningCycleResponse> {
        return dunningService.listDunningCycles(request)
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

    @Post("/delete-master-exception")
    suspend fun deleteOrUpdateMasterException(
        @QueryValue("id") id: String,
        @QueryValue("updatedBy") updatedBy: UUID,
        @QueryValue("actionType") actionType: String
    ): Boolean {
        return dunningService.deleteOrUpdateMasterException(id, updatedBy, actionType)
    }

    @Put("/status")
    suspend fun updateStatusDunningCycle(
        @Valid @Body
        updateDunningCycleExecutionStatusReq: UpdateDunningCycleStatusReq
    ): Boolean {
        return dunningService.updateStatusDunningCycle(updateDunningCycleExecutionStatusReq)
    }

    @Delete("/dunning-cycle")
    suspend fun deleteCycle(
        @QueryValue("id") id: String,
        @QueryValue("updatedBy") updatedBy: UUID
    ): Boolean {
        return Response<Boolean>().ok(dunningService.deleteCycle(id, updatedBy))
    }

    @Put
    suspend fun updateCycleExecution(
        @Valid @Body
        request: UpdateCycleExecutionRequest
    ): String {
        return Response<String>().ok(
            Hashids.encode(dunningService.updateCycleExecution(request))
        )
    }

    @Get("/organization-stakeholders{?request*}")
    suspend fun getAllCreditControllersData(
        @Valid
        request: ListOrganizationStakeholderRequest
    ): List<CreditControllerResponse> {
        return Response<List<CreditControllerResponse>>().ok(
            dunningService.listDistinctCreditControllers(request)
        )
    }

    @Get("/list-overall-outstanding-and-on-account-per-trade-party{?request*}")
    suspend fun overallOutstandingAndOnAccountPerTradeParty(
        @Valid
        request: OverallOutstandingAndOnAccountRequest
    ): ResponseList<OverallOutstandingAndOnAccountResponse> {
        return Response<ResponseList<OverallOutstandingAndOnAccountResponse>>().ok(
            dunningService.overallOutstandingAndOnAccountPerTradeParty(request)
        )
    }

    @Get("/month-wise-statistics-of-account-utilization{?request*}")
    suspend fun monthWiseStatisticsOfAccountUtilization(
        @Valid
        request: MonthWiseStatisticsOfAccountUtilizationReuest
    ): List<MonthWiseStatisticsOfAccountUtilizationResponse> {
        return Response<List<MonthWiseStatisticsOfAccountUtilizationResponse>>().ok(
            dunningService.monthWiseStatisticsOfAccountUtilization(request)
        )
    }

    @Get("/severity-level-templates")
    suspend fun listSeverityLevelTemplates(): MutableMap<String, String> {
        return Response<MutableMap<String, String>>().ok(
            dunningHelperService.listSeverityLevelTemplates()
        )
    }

    @Post("/check-schedule-time")
    suspend fun calculateNextScheduleTime(@Body scheduleRule: DunningScheduleRule): Date {
        return dunningHelperService.calculateNextScheduleTime(scheduleRule)
    }

    @Post("/send-mail-of-all-communication-to-trade-party")
    suspend fun sendMailOfAllCommunicationToTradeParty(
        @Valid @Body
        sendMailOfAllCommunicationToTradePartyReq: SendMailOfAllCommunicationToTradePartyReq
    ): Response<String> {
        return Response<String>().ok(
            HttpStatus.OK.name,
            dunningHelperService.sendMailOfAllCommunicationToTradeParty(
                sendMailOfAllCommunicationToTradePartyReq,
                true
            )
        )
    }

    @Post("/create-dunning-relevant-user")
    suspend fun createRelevantUser(@Body request: CreateUserRequest): String? {
        return dunningService.createRelevantUser(request)
    }
    @Get("/card-data")
    suspend fun dunningCardData(@QueryValue("entityCode") entityCode: MutableList<Int>?): DunningCardData {
        return dunningService.dunningCardData(entityCode)
    }
}
