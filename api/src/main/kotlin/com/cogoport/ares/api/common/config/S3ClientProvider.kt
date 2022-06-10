package com.cogoport.ares.common.config

import com.cogoport.brahma.s3.S3ClientBuilder
import com.cogoport.brahma.s3.auth.AWSCredentials
import com.cogoport.brahma.s3.auth.AWSCredentialsProvider
import com.cogoport.brahma.s3.client.S3Client
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory

@Factory
class S3ClientProvider {
    @Bean
    fun s3Client(): S3Client {
        return S3ClientBuilder.Builder().default()
    }
}
