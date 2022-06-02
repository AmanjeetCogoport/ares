package com.cogoport.ares.api.payment.controller

import com.cogoport.ares.model.payment.CollectionRequest
import com.cogoport.ares.model.payment.OverallStatsRequest
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import java.util.Objects
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@MicronautTest
internal class DashboardControllerTest {

    @Inject
    lateinit var dashboardController: DashboardController

    @Test
    fun getOverallStats() = runTest  {
        var overallStatsResponse = dashboardController.getOverallStats(OverallStatsRequest(null, null))
        assert(Objects.nonNull(overallStatsResponse))
    }

    @Test
    fun getCollectionTrend() = runTest   {
        var collectionTrend = dashboardController.getCollectionTrend(CollectionRequest(null, null, 1, 2022))
        assert(Objects.nonNull(collectionTrend))
    }
}