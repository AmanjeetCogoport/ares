package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.gateway.OpenSearchClient
import com.cogoport.ares.api.payment.entity.OrgOutstanding
import com.cogoport.ares.api.payment.mapper.OrgOutstandingMapper
import com.cogoport.ares.api.payment.model.OpenSearchListRequest
import com.cogoport.ares.api.payment.model.OpenSearchRequest
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.service.interfaces.OpenSearchService
import com.cogoport.ares.model.payment.CustomerOutstanding
import com.cogoport.ares.model.payment.DueAmount
import com.cogoport.ares.model.payment.InvoiceStats
import com.cogoport.brahma.opensearch.Client
import com.cogoport.brahma.rabbitmq.model.RabbitmqEventLogDocument
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.time.format.DateTimeFormatter
import java.util.UUID

@Singleton
class OpenSearchServiceImpl : OpenSearchService {

    @Inject
    lateinit var accountUtilizationRepository: AccountUtilizationRepository

    @Inject
    lateinit var orgOutstandingConverter: OrgOutstandingMapper

    /** Outstanding Data */
    override suspend fun pushOutstandingData(request: OpenSearchRequest) {
        if (request.orgId.isEmpty()) {
            throw AresException(AresError.ERR_1003, AresConstants.ORG_ID)
        }
        accountUtilizationRepository.generateOrgOutstanding(request.orgId, null, null).also {
            updateOrgOutstanding(null, request.orgName, request.orgId, it)
        }
        accountUtilizationRepository.generateBillOrgOutstanding(request.orgId, request.zone, null).also {
            updateOrgOutstanding(request.zone, request.orgName, request.orgId, it)
        }
    }

    /**
     * Push List of Organization outstanding data to open search.
     * @param: openSearchListRequest
     */
    override suspend fun pushOutstandingListData(openSearchListRequest: OpenSearchListRequest) {
        for (organization in openSearchListRequest.openSearchList) {
            pushOutstandingData(
                OpenSearchRequest(
                    zone = organization.zone,
                    date = AresConstants.CURR_DATE.toString()
                        .format(DateTimeFormatter.ofPattern(AresConstants.YEAR_DATE_FORMAT)),
                    quarter = AresConstants.CURR_QUARTER,
                    year = AresConstants.CURR_YEAR,
                    orgId = organization.orgId,
                    orgName = organization.orgName,
                    accMode = null
                )
            )
        }
    }

    private fun updateOrgOutstanding(zone: String?, orgName: String?, orgId: String?, data: List<OrgOutstanding>) {
        if (data.isEmpty()) return
        val dataModel = data.map { orgOutstandingConverter.convertToModel(it) }
        val invoicesDues = dataModel.groupBy { it.currency }.map {
            DueAmount(
                it.key,
                it.value.sumOf { it.openInvoicesAmount.toString().toBigDecimal() },
                it.value.sumOf { it.openInvoicesCount!! }
            )
        }.toMutableList()
        val paymentsDues = dataModel.groupBy { it.currency }.map {
            DueAmount(
                it.key,
                it.value.sumOf { it.paymentsAmount.toString().toBigDecimal() },
                it.value.sumOf { it.paymentsCount!! }
            )
        }.toMutableList()
        val outstandingDues = dataModel.groupBy { it.currency }.map {
            DueAmount(
                it.key,
                it.value.sumOf { it.outstandingAmount.toString().toBigDecimal() },
                it.value.sumOf { it.openInvoicesCount!! }
            )
        }.toMutableList()
        val invoicesCount = dataModel.sumOf { it.openInvoicesCount!! }
        val paymentsCount = dataModel.sumOf { it.paymentsCount!! }
        val invoicesLedAmount = dataModel.sumOf { it.openInvoicesLedAmount!! }
        val paymentsLedAmount = dataModel.sumOf { it.paymentsLedAmount!! }
        val outstandingLedAmount = dataModel.sumOf { it.outstandingLedAmount!! }
        validateDueAmount(invoicesDues)
        validateDueAmount(paymentsDues)
        validateDueAmount(outstandingDues)
        val orgOutstanding = CustomerOutstanding(
            orgId,
            orgName,
            zone,
            InvoiceStats(invoicesCount, invoicesLedAmount, invoicesDues.sortedBy { it.currency }),
            InvoiceStats(paymentsCount, paymentsLedAmount, paymentsDues.sortedBy { it.currency }),
            InvoiceStats(invoicesCount, outstandingLedAmount, outstandingDues.sortedBy { it.currency }),
            null
        )
        val docId = if (zone != null) "${orgId}_$zone" else "${orgId}_ALL"
        OpenSearchClient().updateDocument(AresConstants.SALES_OUTSTANDING_INDEX, docId, orgOutstanding)
    }

    fun validateDueAmount(data: MutableList<DueAmount>) {
        data.forEach { if (it.amount == 0.toBigDecimal()) it.amount = 0.0.toBigDecimal() }
        listOf("INR", "USD").forEach { curr ->
            if (curr !in data.groupBy { it.currency }) {
                data.add(DueAmount(curr, 0.0.toBigDecimal(), 0))
            }
        }
    }

    override suspend fun pushEventLogsToOpenSearch(rabbitmqEventLogDocument: RabbitmqEventLogDocument) {
        Client.addDocument(com.cogoport.brahma.rabbitmq.model.Constants.EVENT_LOG_INDEX, UUID.randomUUID().toString(), rabbitmqEventLogDocument, true)
    }
}
