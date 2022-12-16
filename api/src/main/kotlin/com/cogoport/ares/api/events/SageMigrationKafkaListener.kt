package com.cogoport.ares.api.events

import com.cogoport.ares.api.migration.model.JournalVoucherRecord
import com.cogoport.ares.api.migration.model.PaymentRecord
import com.cogoport.ares.api.migration.model.SettlementRecord
import com.cogoport.ares.api.migration.service.interfaces.PaymentMigration
import io.micronaut.configuration.kafka.annotation.KafkaListener
import io.micronaut.configuration.kafka.annotation.OffsetReset
import io.micronaut.configuration.kafka.annotation.OffsetStrategy
import io.micronaut.configuration.kafka.annotation.Topic
import jakarta.inject.Inject
import kotlinx.coroutines.runBlocking

@KafkaListener(offsetReset = OffsetReset.LATEST, pollTimeout = "15000ms", offsetStrategy = OffsetStrategy.SYNC_PER_RECORD, threads = 2)
class SageMigrationKafkaListener {
    @Inject
    lateinit var paymentMigration: PaymentMigration

    @Topic("sage-payment-migration")
    fun migrateSagePayments(paymentRecord: PaymentRecord) = runBlocking {
        paymentMigration.migratePayment(paymentRecord)
    }
    @Topic("sage-jv-migration")
    fun migrateJournalVoucher(journalVoucherRecord: JournalVoucherRecord) = runBlocking {
        paymentMigration.migarteJournalVoucher(journalVoucherRecord)
    }
    @Topic("settlement-migration")
    fun migrateSettlements(settlementRecord: SettlementRecord) = runBlocking {
        paymentMigration.migrateSettlements(settlementRecord)
    }

    @Topic("update-utilization-amount")
    fun updateUtilizationAmount(paymentRecord: PaymentRecord) = runBlocking {
        paymentMigration.updatePayment(paymentRecord)
    }
}
