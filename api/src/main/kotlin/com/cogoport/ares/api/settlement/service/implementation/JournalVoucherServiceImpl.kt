package com.cogoport.ares.api.settlement.service.implementation

import com.cogoport.ares.api.common.models.ResponseList
import com.cogoport.ares.api.settlement.entity.JournalVoucher
import com.cogoport.ares.api.settlement.mapper.JournalVoucherMapper
import com.cogoport.ares.api.settlement.repository.JournalVoucherRepository
import com.cogoport.ares.api.settlement.service.interfaces.JournalVoucherService
import com.cogoport.ares.model.settlement.JournalVoucherResponse
import com.cogoport.ares.model.settlement.request.JvListRequest
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlin.math.ceil
import kotlin.math.roundToInt

@Singleton
class JournalVoucherServiceImpl : JournalVoucherService {

    @Inject
    lateinit var journalVoucherMapper: JournalVoucherMapper

    @Inject
    lateinit var journalVoucherRepository: JournalVoucherRepository

    override suspend fun getJournalVouchers(jvListRequest: JvListRequest): ResponseList<JournalVoucherResponse> {

        val documentEntity = journalVoucherRepository.getListVouchers(
            jvListRequest.entityCode,
            jvListRequest.startDate,
            jvListRequest.endDate,
            jvListRequest.page,
            jvListRequest.pageLimit,
            jvListRequest.query
        )
        val totalRecords =
            journalVoucherRepository.countDocument(
                jvListRequest.entityCode,
                jvListRequest.startDate,
                jvListRequest.endDate
            )
        val jvList = mutableListOf<JournalVoucherResponse>()
        documentEntity.forEach { doc ->
            jvList.add(journalVoucherMapper.convertToModel((doc)))
        }

        return ResponseList(
            list = jvList,
            totalPages = getTotalPages(totalRecords, jvListRequest.pageLimit),
            totalRecords = totalRecords,
            pageNo = jvListRequest.page
        )
    }
    private fun getTotalPages(totalRows: Long, pageSize: Int): Long {

        return try {
            val totalPageSize = if (pageSize > 0) pageSize else 1
            ceil((totalRows.toFloat() / totalPageSize.toFloat()).toDouble()).roundToInt().toLong()
        } catch (e: Exception) {
            0
        }
    }

    override suspend fun createJournalVouchers(journalVoucher: JournalVoucher): JournalVoucher {
        val newJv =
            JournalVoucher(
                id = journalVoucher.id,
                entityCode = journalVoucher.entityCode,
                entityId = journalVoucher.entityId,
                jvNum = journalVoucher.jvNum,
                type = journalVoucher.type,
                category = journalVoucher.category,
                validityDate = journalVoucher.validityDate,
                amount = journalVoucher.amount,
                currency = journalVoucher.currency,
                status = journalVoucher.status,
                exchangeRate = journalVoucher.exchangeRate,
                tradePartyId = journalVoucher.tradePartyId,
                tradePartnerName = journalVoucher.tradePartnerName,
                createdBy = journalVoucher.createdBy,
                createdAt = journalVoucher.createdAt,
                updatedAt = journalVoucher.updatedAt,
                updatedBy = journalVoucher.updatedBy
            )

        journalVoucherRepository.save(newJv)
        return newJv
    }
}
