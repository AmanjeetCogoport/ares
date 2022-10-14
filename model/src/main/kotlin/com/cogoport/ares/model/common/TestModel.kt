package com.cogoport.ares.model.common

import io.micronaut.core.annotation.Introspected

@Introspected
data class TestModel(
    var value: Long? = null
)
