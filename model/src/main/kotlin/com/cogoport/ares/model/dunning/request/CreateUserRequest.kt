package com.cogoport.ares.model.dunning.request

import io.micronaut.core.annotation.Introspected

@Introspected
data class CreateUserRequest(
    val userToken: String,
    val mobileCountryCode: String?,
    val mobileNumber: String?,
    val email: String,
    val name: String,
    val workScopes: MutableList<String>?
)
