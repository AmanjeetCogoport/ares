package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.common.client.AuthClient
import com.cogoport.ares.api.payment.entity.Bpr
import com.cogoport.ares.api.payment.repository.BprRepository
import com.cogoport.ares.api.payment.service.interfaces.BprService
import com.cogoport.ares.model.common.ResponseList
import com.cogoport.ares.model.payment.request.BprRequest
import com.cogoport.ares.model.payment.request.ListBprRequest
import com.cogoport.ares.model.payment.response.BprResponse
import com.cogoport.plutus.model.invoice.SageOrganizationRequest
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.sql.Timestamp
import java.time.LocalDateTime

@Singleton
open class BprServiceImpl : BprService {
    @Inject
    lateinit var bprRepo: BprRepository

    @Inject
    lateinit var cogoClient: AuthClient

    override suspend fun add(request: BprRequest): Long {
        val sageOrganization = cogoClient.getSageOrganization(
            SageOrganizationRequest(
                request.tradePartyDetailSerialId.toString(),
                "importer_exporter"
            )
        )
        val response = bprRepo.save(
            Bpr(
                id = null,
                businessName = request.businessName,
                tradePartyDetailSerialId = request.tradePartyDetailSerialId,
                tradePartyDetailId = request.tradePartyDetailId,
                sageOrgId = sageOrganization.sageOrganizationId,
                createdAt = Timestamp.valueOf(LocalDateTime.now()),
                updatedAt = Timestamp.valueOf(LocalDateTime.now()),
                deletedAt = null
            )
        )
        return response.id!!
    }

    override suspend fun list(request: ListBprRequest): ResponseList<BprResponse?> {
        if (request.q != null) {
            request.q = "%${request.q}%"
        }
        var list = bprRepo.getBpr(request.q, request.page, request.pageLimit)
        val responseList = ResponseList<BprResponse?>()
        responseList.list = list
        responseList.totalRecords = bprRepo.getCount(request.q)
        responseList.totalPages = if (responseList.totalRecords != 0L) (responseList.totalRecords!! / request.pageLimit!!) + 1 else 1
        responseList.pageNo = request.page
        return responseList
    }
}
