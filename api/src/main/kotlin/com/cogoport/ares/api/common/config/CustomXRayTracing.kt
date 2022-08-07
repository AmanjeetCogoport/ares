package com.cogoport.ares.api.common.config

import brave.Tracing
import brave.context.slf4j.MDCScopeDecorator
import brave.propagation.ThreadLocalCurrentTraceContext
import brave.propagation.aws.AWSPropagation
import brave.sampler.Sampler
import io.micronaut.context.annotation.Value
import zipkin2.reporter.brave.AsyncZipkinSpanHandler
import zipkin2.reporter.xray_udp.XRayUDPReporter

class CustomXRayTracing {
    fun getCustomXRayTracing(@Value("\${micronaut.application.name}") serviceName: String): Tracing? {
        return Tracing.newBuilder().localServiceName(serviceName)
            .propagationFactory(AWSPropagation.FACTORY)
            .currentTraceContext(
                ThreadLocalCurrentTraceContext.newBuilder().addScopeDecorator(MDCScopeDecorator.get()).build()
            )
            .addSpanHandler(AsyncZipkinSpanHandler.create(XRayUDPReporter.create("localhost:2000")))
            .traceId128Bit(true) // X-Ray requires 128-bit trace IDs
            .sampler(Sampler.ALWAYS_SAMPLE) // Configure as desired
            .build()
    }
}
