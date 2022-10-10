package com.cogoport.ares.api.common.controller

import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.hades.client.HadesClient
import com.cogoport.kuber.client.KuberClient
import com.cogoport.plutus.client.PlutusClient
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.validation.Validated
import jakarta.inject.Inject

@Validated
@Controller("/service-discovery")
class ReachabilityController {

    @Inject
    private lateinit var plutusClient: PlutusClient

    @Inject
    private lateinit var hadesClient: HadesClient

    @Inject
    private lateinit var kuberClient: KuberClient

    @Get("/reachability")
    fun reachable(): HttpResponse<String> {
        return try {
            HttpResponse
                .ok<String>()
                .body("{\"status\":\"reachable\"}")
        } catch (ex: Exception) {
            throw AresException(AresError.ERR_1507, "")
        }
    }

    @Get("/plutus-reachability")
    suspend fun checkPlutusReachability(): HttpResponse<String> {
        return try {
            plutusClient.reachable()
        } catch (ex: Exception) {
            throw AresException(AresError.ERR_1508, "")
        }
    }

    @Get("/hades-reachability")
    suspend fun checkHadesReachability(): HttpResponse<String> {
        return try {
            hadesClient.reachable()
        } catch (ex: Exception) {
            throw AresException(AresError.ERR_1509, "")
        }
    }

    @Get("/kuber-reachability")
    suspend fun checkKuberReachability(): HttpResponse<String> {
        return try {
            kuberClient.reachable()
        } catch (ex: Exception) {
            throw AresException(AresError.ERR_1510, "")
        }
    }

}
