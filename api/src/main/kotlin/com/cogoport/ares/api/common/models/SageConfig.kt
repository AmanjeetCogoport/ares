package com.cogoport.ares.api.common.models

import io.micronaut.context.annotation.ConfigurationProperties

@ConfigurationProperties("sage")
class SageConfig {
    var soapUrl: String? = null
    var user: String? = null
    var password: String? = null
    var queryClientUrl: String? = null
    var queryClientPassword: String? = null
    var loginUrl: String? = null
    var username: String? = null
    var userpassword: String? = null
}
