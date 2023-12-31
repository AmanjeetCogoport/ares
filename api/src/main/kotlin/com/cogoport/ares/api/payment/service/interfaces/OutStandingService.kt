package com.cogoport.ares.api.payment.service.interfaces

import com.cogoport.ares.api.payment.entity.EntityLevelStats
import com.cogoport.ares.api.payment.entity.EntityWiseOutstandingBucket
import com.cogoport.ares.api.payment.model.CustomerOutstandingPaymentRequest
import com.cogoport.ares.api.payment.model.CustomerOutstandingPaymentResponse
import com.cogoport.ares.api.payment.model.requests.OutstandingVisualizationRequest
import com.cogoport.ares.api.payment.model.response.TopServiceProviders
import com.cogoport.ares.model.common.ResponseList
import com.cogoport.ares.model.common.TradePartyOutstandingReq
import com.cogoport.ares.model.common.TradePartyOutstandingRes
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.CustomerOutstanding
import com.cogoport.ares.model.payment.ListInvoiceResponse
import com.cogoport.ares.model.payment.OutstandingList
import com.cogoport.ares.model.payment.SupplierOutstandingList
import com.cogoport.ares.model.payment.request.AccPayablesOfOrgReq
import com.cogoport.ares.model.payment.request.BulkUploadRequest
import com.cogoport.ares.model.payment.request.CustomerMonthlyPaymentRequest
import com.cogoport.ares.model.payment.request.CustomerOutstandingRequest
import com.cogoport.ares.model.payment.request.InvoiceListRequest
import com.cogoport.ares.model.payment.request.OutstandingListRequest
import com.cogoport.ares.model.payment.request.SupplierOutstandingRequest
import com.cogoport.ares.model.payment.request.SupplierOutstandingRequestV2
import com.cogoport.ares.model.payment.request.UpdateAccountTaggingRequest
import com.cogoport.ares.model.payment.response.AccPayablesOfOrgRes
import com.cogoport.ares.model.payment.response.CustomerMonthlyPayment
import com.cogoport.ares.model.payment.response.CustomerOutstandingDocumentResponse
import com.cogoport.ares.model.payment.response.CustomerOutstandingDocumentResponseV2
import com.cogoport.ares.model.payment.response.PayblesInfoRes
import com.cogoport.ares.model.payment.response.SupplierOutstandingDocument
import com.cogoport.ares.model.payment.response.SupplierOutstandingDocumentV2
import java.math.BigDecimal
import java.util.UUID

interface OutStandingService {

    suspend fun getInvoiceList(request: InvoiceListRequest): ListInvoiceResponse?

    suspend fun getOutstandingList(request: OutstandingListRequest): OutstandingList?

    suspend fun getCustomerOutstanding(orgId: String): MutableList<CustomerOutstanding?>

    suspend fun getCurrOutstanding(invoiceIds: List<Long>): Long

    suspend fun getCustomersOutstandingInINR(orgIds: List<String>): MutableMap<String, BigDecimal?>

    suspend fun getSupplierOutstandingList(request: OutstandingListRequest): SupplierOutstandingList

    suspend fun createSupplierDetails(request: SupplierOutstandingDocument)
    suspend fun updateSupplierDetails(id: String, flag: Boolean, document: SupplierOutstandingDocument?)

    suspend fun listSupplierDetails(request: SupplierOutstandingRequest): ResponseList<SupplierOutstandingDocument?>

    suspend fun createCustomerDetails(request: CustomerOutstandingDocumentResponse)

    suspend fun updateCustomerDetails(id: String, flag: Boolean, document: CustomerOutstandingDocumentResponse?)

    suspend fun listCustomerDetails(request: CustomerOutstandingRequest): ResponseList<CustomerOutstandingDocumentResponse?>

    suspend fun getCustomerOutstandingPaymentDetails(request: CustomerOutstandingPaymentRequest): ResponseList<CustomerOutstandingPaymentResponse?>

    suspend fun getPayablesInfo(entity: Int?): PayblesInfoRes

    suspend fun uploadPayblesStats()
    suspend fun getTopTenServiceProviders(request: SupplierOutstandingRequestV2): TopServiceProviders

    suspend fun getPayableOfOrganization(request: AccPayablesOfOrgReq): List<AccPayablesOfOrgRes>

    suspend fun getCustomerMonthlyPayment(request: CustomerMonthlyPaymentRequest): CustomerMonthlyPayment

    suspend fun getTradePartyOutstanding(request: TradePartyOutstandingReq): List<TradePartyOutstandingRes>?

    suspend fun createLedgerSummary()

//    suspend fun getOverallCustomerOutstanding(entityCodes: String?): HashMap<String, EntityWiseOutstandingBucket>

    suspend fun createSupplierDetailsV2()

    suspend fun listSupplierDetailsV2(request: SupplierOutstandingRequestV2): ResponseList<SupplierOutstandingDocumentV2?>

    suspend fun getEntityLevelStats(entityCode: Int): List<EntityLevelStats>

    suspend fun createRecordInBulk(request: BulkUploadRequest?): String?

    suspend fun getDistinctOrgIds(accMode: AccMode?): List<UUID>?

    suspend fun getOpenInvoices(organizationId: UUID): String

    suspend fun createCustomerDetailsV2(request: CustomerOutstandingDocumentResponseV2)
    suspend fun getOutstandingDataBifurcation(request: OutstandingVisualizationRequest): Any

    suspend fun updateCustomerDetailsV2(orgId: UUID, entityCode: Int?)
    suspend fun getCustomerData()

    suspend fun listCustomerDetailsV2(req: CustomerOutstandingRequest): ResponseList<CustomerOutstandingDocumentResponseV2?>

    suspend fun updateAccountTaggings(req: UpdateAccountTaggingRequest): Boolean

    suspend fun getOverAllCustomerOutstandingV2(entityCodes: String?): HashMap<String, EntityWiseOutstandingBucket>
}
