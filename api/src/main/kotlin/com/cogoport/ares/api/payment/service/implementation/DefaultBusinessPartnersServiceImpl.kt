package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.common.client.AuthClient
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.payment.entity.DefaultBusinessPartners
import com.cogoport.ares.api.payment.repository.DefaultBusinessPartnersRepository
import com.cogoport.ares.api.payment.service.interfaces.DefaultBusinessPartnersService
import com.cogoport.ares.model.common.ResponseList
import com.cogoport.ares.model.payment.request.BprRequest
import com.cogoport.ares.model.payment.request.ListBprRequest
import com.cogoport.ares.model.payment.response.DefaultBusinessPartnersResponse
import com.cogoport.plutus.model.invoice.SageOrganizationRequest
import com.cogoport.plutus.model.invoice.SageOrganizationResponse
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.sql.Timestamp
import java.time.LocalDateTime

@Singleton
open class DefaultBusinessPartnersServiceImpl : DefaultBusinessPartnersService {
    @Inject
    lateinit var bprRepo: DefaultBusinessPartnersRepository

    @Inject
    lateinit var cogoClient: AuthClient

    override suspend fun add(request: BprRequest): Long {
        val sageOrganization: SageOrganizationResponse
        try {
            sageOrganization = cogoClient.getSageOrganization(
                SageOrganizationRequest(
                    request.tradePartyDetailSerialId.toString(),
                    "importer_exporter"
                )
            )
        } catch (e: Exception) {
            throw AresException(AresError.ERR_1002, "")
        }

        if (bprRepo.findTradePartyDetailSerialId(request.tradePartyDetailSerialId.toString()) == request.tradePartyDetailSerialId.toString()) {
            throw AresException(AresError.ERR_1512, "")
        }

        val response = bprRepo.save(
            DefaultBusinessPartners(
                id = null,
                businessName = request.businessName,
                tradePartyDetailSerialId = request.tradePartyDetailSerialId.toString(),
                tradePartyDetailId = request.tradePartyDetailId,
                sageOrgId = sageOrganization.sageOrganizationId,
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

    override suspend fun list(request: ListBprRequest): ResponseList<DefaultBusinessPartnersResponse?> {
        if (request.q != null) {
            request.q = "%${request.q}%"
        }
        var list = bprRepo.getDefaultBusinessPartners(request.q, request.page, request.pageLimit)
        val responseList = ResponseList<DefaultBusinessPartnersResponse?>()
        responseList.list = list
        responseList.totalRecords = bprRepo.getCount(request.q)
        responseList.totalPages = if (responseList.totalRecords != 0L) (responseList.totalRecords!! / request.pageLimit!!) + 1 else 1
        responseList.pageNo = request.page
        return responseList
    }
}
