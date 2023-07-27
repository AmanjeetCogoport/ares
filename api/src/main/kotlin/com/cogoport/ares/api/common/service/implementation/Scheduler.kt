package com.cogoport.ares.api.common.service.implementation

import com.cogoport.ares.api.balances.service.implementation.LedgerBalanceServiceImpl
import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.events.AresMessagePublisher
import com.cogoport.ares.api.migration.service.interfaces.PaymentMigrationWrapper
import com.cogoport.ares.api.payment.entity.AresDocument
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.repository.AresDocumentRepository
import com.cogoport.ares.api.payment.repository.PaymentRepository
import com.cogoport.ares.api.payment.repository.UnifiedDBRepo
import com.cogoport.ares.api.payment.service.interfaces.OnAccountService
import com.cogoport.ares.api.payment.service.interfaces.OutStandingService
import com.cogoport.ares.api.settlement.repository.SettlementRepository
import com.cogoport.ares.api.settlement.service.interfaces.SettlementService
import com.cogoport.ares.api.utils.ExcelUtils
import com.cogoport.ares.api.utils.logger
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.request.UpdateSupplierOutstandingRequest
import com.cogoport.ares.model.settlement.PostPaymentToSage
import com.cogoport.brahma.hashids.Hashids
import com.cogoport.brahma.s3.client.S3Client
import io.micronaut.context.annotation.Value
import io.micronaut.scheduling.annotation.Scheduled
import io.sentry.Sentry
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDate.now
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.TimeZone

@Singleton
class Scheduler(
    private var emitter: AresMessagePublisher,
    private var accountUtilizationRepository: AccountUtilizationRepository,
    private var settlementRepository: SettlementRepository,
    private var settlementService: SettlementService,
    private var paymentRepository: PaymentRepository,
    private var outStandingService: OutStandingService,
    private var unifiedDBRepo: UnifiedDBRepo,
    private var ledgerBalanceServiceImpl: LedgerBalanceServiceImpl,
    private var aresMessagePublisher: AresMessagePublisher,
    private var aresDocumentRepository: AresDocumentRepository,
    private var s3Client: S3Client,
    private var onAccountService: OnAccountService,
    private var paymentMigration: PaymentMigrationWrapper
) {
    @Value("\${aws.s3.bucket}")
    private lateinit var s3Bucket: String

    @Value("\${server.base-url}") // application-prod.yml path
    private lateinit var baseUrl: String

    @Scheduled(cron = "0 0 * * *")
    fun updateSupplierOutstandingOnOpenSearch() {
        runBlocking {
            val orgIds = accountUtilizationRepository.getTradePartyOrgIds(AccMode.AP)
            for (orgId in orgIds) {
                try {
                    emitter.emitUpdateSupplierOutstanding(UpdateSupplierOutstandingRequest(orgId = orgId))
                } catch (e: Exception) {
                    logger().error(e.message)
                    Sentry.captureException(e)
                }
            }
        }
    }

    @Scheduled(cron = "0 0 * * *")
    fun updateCustomerOutstandingOnOpenSearch() {
        runBlocking {
            val orgIds = accountUtilizationRepository.getTradePartyOrgIds(AccMode.AR)
            for (orgId in orgIds) {
                try {
                    emitter.emitUpdateCustomerOutstanding(UpdateSupplierOutstandingRequest(orgId = orgId))
                } catch (e: Exception) {
                    logger().error(e.message)
                    Sentry.captureException(e)
                }
            }
        }
    }
    @Scheduled(cron = "30 04 * * *")
    fun uploadPayblesInfo() {
        runBlocking {
            outStandingService.uploadPayblesStats()
        }
    }

    @Scheduled(cron = "0 * * * *")
    fun deleteInvoicesNotPresentInPlutus() {
        runBlocking {
            val ids = unifiedDBRepo.getInvoicesNotPresentInPlutus()
            if (!ids.isNullOrEmpty()) {
                for (id in ids) {
                    emitter.emitDeleteInvoicesNotPresentInPlutus(id)
                }
            }
        }
    }

    @Scheduled(cron = "0 0 * * *", zoneId = "Europe/Paris")
    fun createLedgerBalancesForNetherlands() = runBlocking {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Paris"))
        ledgerBalanceServiceImpl.createLedgerBalances(calendar.time, 201)
    }

    @Scheduled(cron = "0 0 * * *", zoneId = "Asia/Ho_Chi_Minh")
    fun createLedgerBalancesForVietnam() = runBlocking {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"))
        ledgerBalanceServiceImpl.createLedgerBalances(calendar.time, 501)
    }

    @Scheduled(cron = "0 0 * * *", zoneId = "Asia/Kolkata")
    fun createLedgerBalancesForIndia() = runBlocking {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
        ledgerBalanceServiceImpl.createLedgerBalances(calendar.time, 301)
        ledgerBalanceServiceImpl.createLedgerBalances(calendar.time, 101)
    }

    /**
     * Asia/Singapore is UTC+08:00
     **/
    @Scheduled(cron = "0 16 * * *")
    fun createLedgerBalancesForSingapore() = runBlocking {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"))
        ledgerBalanceServiceImpl.createLedgerBalances(calendar.time, 401)
    }

    @Scheduled(cron = "0 18 * * *")
    fun bulkPaymentPostToSage() = runBlocking {
        val yesterday = now().minus(1, ChronoUnit.DAYS)
        logger().info("Scheduler started for AP post payment to sage for date: $yesterday")
        val paymentIds = paymentRepository.getPaymentIdsForApprovedPayments()
        if (!paymentIds.isNullOrEmpty()) {
            paymentIds.forEach {
                aresMessagePublisher.emitPostPaymentToSage(
                    PostPaymentToSage(
                        it,
                        AresConstants.ARES_USER_ID
                    )
                )
            }
        }
    }

    @Scheduled(cron = "0 17 * * *")
    fun bulkMatchingSettlement() = runBlocking {
        val today = now()
        logger().info("Scheduler started for Bulk Matching Settlement On Sage for date: $today")
        val date = Timestamp.valueOf("2023-05-16 00:00:00")
        val settlementsIds = settlementRepository.getSettlementIdForCreatedStatus(date)
        if (!settlementsIds.isNullOrEmpty()) {
            settlementService.bulkMatchingSettlementOnSage(settlementsIds, AresConstants.ARES_USER_ID)
        }
    }

    @Scheduled(cron = "30 19 * * *")
    fun settlementMatchingFailedOnSageEmail() {
        val today = now()
        logger().info("Scheduler has been initiated to send Email notifications for settlement matching failures up to the date: $today")
        val settlementsNotPosted = runBlocking {
            settlementRepository.getAllSettlementsMatchingFailedOnSage()
        }
        if (settlementsNotPosted.isNullOrEmpty()) return
        val excelName = "Failed_Settlements_Matching_On_Sage" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_hhmmss"))
        val file = runBlocking {
            ExcelUtils.writeIntoExcel(settlementsNotPosted as List<Any>, excelName, "Failed Settlements Matching On Sage")
        }
        val url = s3Client.upload(s3Bucket, "$excelName.xlsx", file)
        val aresDocument = AresDocument(
            documentUrl = url.toString(),
            documentName = "failed_settlement_matching",
            documentType = "xlsx",
            uploadedBy = AresConstants.ARES_USER_ID
        )
        val saveUrl = runBlocking {
            aresDocumentRepository.save(aresDocument)
        }
        val visibleUrl = "$baseUrl/payments/download?id=${Hashids.encode(saveUrl.id!!)}"
        runBlocking {
            settlementService.sendEmailSettlementsMatchingFailed(visibleUrl)
        }
    }

    @Scheduled(cron = "0 5 * * *")
    fun sendSagePlatformPaymentReport() = runBlocking {
        val endDate: String = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
        val startDate: String = LocalDate.now().minusDays(1).format(DateTimeFormatter.BASIC_ISO_DATE)

        onAccountService.downloadSagePlatformReport(startDate, endDate)
    }

    @Scheduled(cron = "30 20 * * *")
    fun migrateMTCCVJV() {
        val endDate: String = LocalDate.now().toString()
        val startDate: String = LocalDate.now().minusDays(1).toString()
        logger().info("Scheduler has been initiated to migrate MTCCV JV for the date range : $endDate - $startDate")
        val size = runBlocking {
            paymentMigration.migrateMTCCVJV(startDate, endDate)
        }
        logger().info("Request for mtccv jv migration received, total number of parent jv to migrate is $size")
    }

    @Scheduled(cron = "0 17 * * *")
    suspend fun createLedgerSummaryForAp() = runBlocking {
        outStandingService.createLedgerSummary()
    }
}
