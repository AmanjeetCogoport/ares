package com.cogoport

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepo
import com.cogoport.ares.api.utils.logger
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import com.cogoport.brahma.opensearch.Client as OpenSearchClient

@ExtendWith(MockitoExtension::class)
@OptIn(ExperimentalCoroutinesApi::class)
@MicronautTest(transactional = false)
class SettlementTest () {
    @Inject
    @field:Client("/payments")
    lateinit var client: HttpClient

    @Inject
    lateinit var accountUtilizationRepo: AccountUtilizationRepo

    @Inject
    lateinit var helper: SettlementHelper


    @BeforeEach
    fun setUp() = runTest {
        accountUtilizationRepo.deleteAll()
//        OpenSearchClient.deleteIndex(indexName = AresConstants.ACCOUNT_UTILIZATION_INDEX)
    }

    @AfterEach
    fun tearDown() = runTest {
        accountUtilizationRepo.deleteAll()
//        OpenSearchClient.deleteIndex(indexName = AresConstants.ACCOUNT_UTILIZATION_INDEX)
    }

    @Test
    fun settle() = runTest {
//        val endpoint = "/settlement/"
        val id = helper.saveAccountUtilizations()
        logger().info("saved dfocument id: $id")
    }
}