package com.cogoport.ares.api.common.config

import com.cogoport.ares.api.utils.logger
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Value
import io.micronaut.discovery.DiscoveryClient
import io.micronaut.discovery.ServiceInstance
import jakarta.inject.Inject
import org.reactivestreams.Publisher
import software.amazon.awssdk.services.servicediscovery.ServiceDiscoveryAsyncClient
import software.amazon.awssdk.services.servicediscovery.model.DiscoverInstancesRequest
import software.amazon.awssdk.services.servicediscovery.model.ListServicesResponse
import java.net.URI
import kotlin.random.Random

@Factory
class CogoAWSServiceDiscovery : DiscoveryClient {
    private val log = logger()

    @Inject
    private lateinit var serviceDiscoveryAsyncClient: ServiceDiscoveryAsyncClient

    @Value("\${environment}")
    private lateinit var environment: String

    @Inject
    private lateinit var services: Services

    @Value("\${cogoport.internal_url}")
    private lateinit var bfUrl: String

    @Value("\${port_mapping.common}")
    private lateinit var bfPort: String

    /**
     * The description.
     */
    override fun getDescription(): String {
        return "Aws Service Discovery Client"
    }

    /**
     * Gets a list of instances registered with Route53 given a service name.
     *
     * @param serviceId The service name
     * @return list of serviceInstances usable by MN.
     */
    override fun getInstances(serviceId: String?): Publisher<List<ServiceInstance>?> {
        if (environment == "local") {
            return Publisher { subscriber ->
                subscriber.onNext(getServiceForLocalEnvironment(serviceId))
                subscriber.onComplete()
            }
        } else if (environment == "staging") {
            val serviceAddress = getServiceForStaging(serviceId)
            if (serviceAddress != null) {
                return Publisher { subscriber ->
                    subscriber.onNext(serviceAddress)
                    subscriber.onComplete()
                }
            }
        }
        return resolveService(serviceId)
    }

    private fun resolveService(serviceId: String?): Publisher<List<ServiceInstance>?> {
        if (serviceId == BUSINESS_RAILS_SERVICE_ID) {
            val url = "$bfUrl:$bfPort"
            val list = listOf<ServiceInstance>(ServiceInstance.of(serviceId, URI(url)))
            return Publisher { subscriber ->
                subscriber.onNext(list)
                subscriber.onComplete()
            }
        } else {
            return Publisher { subscriber ->
                val discoverInstancesResult = serviceDiscoveryAsyncClient.discoverInstances(buildRequest(serviceId))
                discoverInstancesResult.whenComplete { t, u ->
                    if (u == null) {
                        val allInstances = t.instances()

                        val list = allInstances?.map {
                            val host = it.attributes()[AWS_INSTANCE_IPV_4_ATTRIBUTE] ?: "localhost"
                            val port = it.attributes()[AWS_INSTANCE_PORT_ATTRIBUTE]?.toInt() ?: 8080
                            log.info("Given {}, found {}", serviceId, "$host:$port")
                            ServiceInstance.of(serviceId, host, port)
                        } ?: emptyList()

                        subscriber.onNext(list)
                        subscriber.onComplete()
                    } else {
                        subscriber.onNext(emptyList())
                        subscriber.onComplete()
                    }
                }
            }
        }
    }
    private fun getServiceForLocalEnvironment(serviceId: String?): List<ServiceInstance>? {
        val serviceMap = services.service!!.firstOrNull { it?.id == serviceId && it?.enabledLocally!! }

        if (serviceMap != null)
            return listOf<ServiceInstance>(ServiceInstance.of(serviceId, URI(serviceMap.url!!)))

        return listOf<ServiceInstance>(ServiceInstance.of(serviceId, URI(services.staging!!)))
    }


    private fun buildRequest(serviceName: String?): DiscoverInstancesRequest {
        return DiscoverInstancesRequest.builder().namespaceName(services.namespace).serviceName(serviceName).build()
    }

    /**
     * Gets a list of service IDs from AWS for a given namespace.
     *
     * @return publisher list of the service IDs in string format
     */
    override fun getServiceIds(): Publisher<List<String?>?> {
        return Publisher {
            emptyList<String>()
        }
    }

    /**
     * Close down AWS Client on shutdown.
     */
    override fun close() {
        serviceDiscoveryAsyncClient.close()
    }

    private fun convertServiceIds(listServicesResult: ListServicesResponse): List<String?>? {
        val services = listServicesResult.services()
        val serviceIds: MutableList<String?> = ArrayList(services.size)
        for (service in services) {
            serviceIds.add(service.id())
        }
        return serviceIds
    }


    private fun getServiceForStaging(serviceId: String?): List<ServiceInstance>? {
        val serviceMap = services.service!!.firstOrNull { it?.id == serviceId && it?.enabledLocally!! }
        if (serviceMap == null && serviceId == BUSINESS_RAILS_SERVICE_ID) {
            var url = "$bfUrl:$bfPort"
            return listOf<ServiceInstance>(ServiceInstance.of(serviceId, URI(url)))
        }
        if (serviceMap != null)
            return listOf<ServiceInstance>(ServiceInstance.of(serviceId, URI(serviceMap.url!!)))

        return null
    }

    companion object {
        private const val AWS_INSTANCE_IPV_4_ATTRIBUTE = "AWS_INSTANCE_IPV4"
        private const val AWS_INSTANCE_PORT_ATTRIBUTE = "AWS_INSTANCE_PORT"
        private const val BUSINESS_RAILS_SERVICE_ID = "bf-rails"
        private val RAND = Random(System.currentTimeMillis())
    }
}
