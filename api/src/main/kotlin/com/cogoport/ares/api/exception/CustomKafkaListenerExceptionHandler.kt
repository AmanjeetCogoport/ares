package com.cogoport.ares.api.exception

import com.cogoport.ares.api.utils.logger
import io.micronaut.configuration.kafka.exceptions.DefaultKafkaListenerExceptionHandler
import io.micronaut.configuration.kafka.exceptions.KafkaListenerException
import io.micronaut.configuration.kafka.exceptions.KafkaListenerExceptionHandler
import io.micronaut.context.annotation.Replaces
import io.micronaut.context.env.Environment
import io.micronaut.core.annotation.NonNull
import io.sentry.Sentry
import jakarta.inject.Singleton
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.errors.SerializationException
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.util.regex.Pattern

@Replaces(DefaultKafkaListenerExceptionHandler::class)
@Singleton
public class CustomKafkaListenerExceptionHandler(private var environment: Environment) : KafkaListenerExceptionHandler {

    private val logger = logger()

    private val LOG = LoggerFactory.getLogger(KafkaListenerExceptionHandler::class.java)
    private val SERIALIZATION_EXCEPTION_MESSAGE_PATTERN =
        Pattern.compile(".+ for partition (.+)-(\\d+) at offset (\\d+)\\..+")

    private var skipRecordOnDeserializationFailure = true

    override fun handle(exception: KafkaListenerException) {
        val cause = exception.cause
        val consumerBean = exception.kafkaListener

        sendToSentry(exception)

        if (cause is SerializationException) {
            LOG.error("Kafka consumer [{}] failed to deserialize value: {}", consumerBean, cause.message, cause)
            if (skipRecordOnDeserializationFailure) {
                val kafkaConsumer = exception.kafkaConsumer
                seekPastDeserializationError(cause, consumerBean, kafkaConsumer)
            }
        } else {
            if (LOG.isErrorEnabled) {
                val consumerRecord = exception.consumerRecord
                if (consumerRecord.isPresent) {
                    LOG.error(
                        "Error processing record [{}] for Kafka consumer [{}] produced error: {}",
                        consumerRecord,
                        consumerBean,
                        cause!!.message,
                        cause
                    )
                } else {
                    LOG.error("Kafka consumer [{}] produced error: {}", consumerBean, cause!!.message, cause)
                }
            }
        }
    }

    private fun sendToSentry(exception: KafkaListenerException) {
        logger.error(exception.toString())

        exception?.let {
            val request = exception.consumerRecord.get().value()
            Sentry.withScope {
                it.setTag("traceId", MDC.get("traceId") ?: "")
                it.setTag("spanId", MDC.get("spanId") ?: "")
                it.setTag("Kafka_Topic", exception.consumerRecord.get().topic() ?: "")
                it.setTag("Cause", exception.cause.toString() ?: "")
                it.setTag("environmentName", "Kafka Listner")
                it.setContexts("Data", request)
                Sentry.captureException(exception)
            }
        }
    }

    /**
     * Sets whether the seek past records that are not deserializable.
     * @param skipRecordOnDeserializationFailure True if records that are not deserializable should be skipped.
     */
    fun setSkipRecordOnDeserializationFailure(skipRecordOnDeserializationFailure: Boolean) {
        this.skipRecordOnDeserializationFailure = skipRecordOnDeserializationFailure
    }

    /**
     * Seeks past a serialization exception if an error occurs.
     * @param cause The cause
     * @param consumerBean The consumer bean
     * @param kafkaConsumer The kafka consumer
     */
    protected fun seekPastDeserializationError(
        @NonNull cause: SerializationException,
        @NonNull consumerBean: Any?,
        @NonNull kafkaConsumer: Consumer<*, *>
    ) {
        try {
            val message = cause.message
            val matcher = SERIALIZATION_EXCEPTION_MESSAGE_PATTERN.matcher(message)
            if (matcher.find()) {
                val topic = matcher.group(1)
                val partition = Integer.valueOf(matcher.group(2))
                val offset = Integer.valueOf(matcher.group(3))
                val tp = TopicPartition(topic, partition)
                LOG.debug(
                    "Seeking past unserializable consumer record for partition {}-{} and offset {}",
                    topic,
                    partition,
                    offset
                )
                kafkaConsumer.seek(tp, (offset + 1).toLong())
            }
        } catch (e: Throwable) {
            LOG.error("Kafka consumer [{}] failed to seek past unserializable value: {}", consumerBean, e.message, e)
        }
    }
}
