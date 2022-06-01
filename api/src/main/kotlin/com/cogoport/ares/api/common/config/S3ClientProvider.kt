package com.cogoport.ares.common.config

import com.cogoport.brahma.s3.S3ClientBuilder
import com.cogoport.brahma.s3.auth.AWSCredentials
import com.cogoport.brahma.s3.auth.AWSCredentialsProvider
import com.cogoport.brahma.s3.client.S3Client
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory

@Factory
class S3ClientProvider {
//    @Bean
//    fun s3Client(): S3Client {
//        return S3ClientBuilder.Builder().default()
//    }

    @Bean
    fun s3Client(): S3Client {
        return S3ClientBuilder.Builder().withRegion("ap-south-1").withCredentials(
            object : AWSCredentialsProvider {
                override fun refresh() {}
                override fun getCredentials(): AWSCredentials {
                    return object : AWSCredentials {
                        override fun getAWSAccessKeyId() = "AKIAYWPSNT6MR2WQRM5P" // AWS Access Key ID
                        override fun getAWSSecretKey() = "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" // AWS Secret Key
                    }
                }
            }
        ).build()
    }
}
