package com.cogoport.ares.api.events

import com.cogoport.ares.api.migration.model.JournalVoucherRecord
import com.cogoport.ares.api.migration.model.PayLocUpdateRequest
import com.cogoport.ares.api.migration.model.PaymentRecord
import com.cogoport.ares.api.migration.model.SettlementRecord
import com.cogoport.ares.api.migration.service.interfaces.PaymentMigration
import io.micronaut.configuration.kafka.annotation.KafkaListener
import io.micronaut.configuration.kafka.annotation.OffsetReset
import io.micronaut.configuration.kafka.annotation.OffsetStrategy
import io.micronaut.configuration.kafka.annotation.Topic
import io.micronaut.context.annotation.Property
import jakarta.inject.Inject
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.consumer.ConsumerConfig

@KafkaListener(
    offsetReset = OffsetReset.EARLIEST, pollTimeout = "10000ms",
    offsetStrategy = OffsetStrategy.SYNC_PER_RECORD, threads = 12, heartbeatInterval = "1000ms",
    properties = [
        Property(name = ConsumerConfig.MAX_POLL_RECORDS_CONFIG, value = "10"),
        Property(name = ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, value = "20000"),
        Property(name = ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG, value = "org.apache.kafka.clients.consumer.RoundRobinAssignor")
    ]
)
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
    fun updateUtilizationAmount(payLocUpdateRequest: PayLocUpdateRequest) = runBlocking {
        paymentMigration.updatePayment(payLocUpdateRequest)
    }
}
