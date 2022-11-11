package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.common.client.AuthClient
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.payment.entity.DefaultedBusinessPartners
import com.cogoport.ares.api.payment.repository.DefaultedBusinessPartnersRepository
import com.cogoport.ares.api.payment.service.interfaces.DefaultedBusinessPartnersService
import com.cogoport.ares.model.common.ResponseList
import com.cogoport.ares.model.payment.request.DefaultedBusinessPartnerRequest
import com.cogoport.ares.model.payment.request.ListDefaultedBusinessPartnersRequest
import com.cogoport.ares.model.payment.response.DefaultedBusinessPartnersResponse
import com.cogoport.plutus.model.invoice.SageOrganizationRequest
import com.cogoport.plutus.model.invoice.SageOrganizationResponse
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.sql.Timestamp
import java.time.LocalDateTime

@Singleton
open class DefaultedBusinessPartnersServiceImpl : DefaultedBusinessPartnersService {
    @Inject
    lateinit var bprRepo: DefaultedBusinessPartnersRepository

    @Inject
    lateinit var cogoClient: AuthClient

    override suspend fun add(request: DefaultedBusinessPartnerRequest): Long {

        if (bprRepo.checkIfTradePartyDetailSerialIdExists(request.tradePartyDetailSerialId)) {
            throw AresException(AresError.ERR_1513, "")
        }

        val sageOrganization: SageOrganizationResponse
        try {
            sageOrganization = cogoClient.getSageOrganization(
                SageOrganizationRequest(
                    request.tradePartyDetailSerialId.toString(),
                    "importer_exporter"
                )
            )
        } catch (e: Exception) {
            throw AresException(AresError.ERR_1514, "")
        }

        val response = bprRepo.save(
            DefaultedBusinessPartners(
                id = null,
                businessName = request.businessName,
                tradePartyDetailSerialId = request.tradePartyDetailSerialId,
                tradePartyDetailId = request.tradePartyDetailId,
                sageOrgId = sageOrganization.sageOrganizationId!!,
                createdAt = Timestamp.valueOf(LocalDateTime.now()),
                updatedAt = Timestamp.valueOf(LocalDateTime.now()),
                deletedAt = null
            )
        )
        return response.id!!
    }

    override suspend fun delete(id: Long): Long {
        return bprRepo.delete(id)
    }

    override suspend fun list(request: ListDefaultedBusinessPartnersRequest): ResponseList<DefaultedBusinessPartnersResponse?> {
        if (request.q != null) {
            request.q = "%${request.q}%"
        }
        var list = bprRepo.getDefaultedBusinessPartners(request.q, request.page, request.pageLimit)
        val responseList = ResponseList<DefaultedBusinessPartnersResponse?>()
        responseList.list = list
        responseList.totalRecords = bprRepo.getCount(request.q)
        responseList.totalPages = if (responseList.totalRecords != 0L) (responseList.totalRecords!! / request.pageLimit!!) + 1 else 1
        responseList.pageNo = request.page
        return responseList
    }
}
