package com.cogoport.ares.common.models

import com.fasterxml.jackson.annotation.JsonProperty

class Response<T> {
    @JsonProperty("message")
    var msg: String? = null

    @JsonProperty("data")
    var data: T? = null

    // TO Return Response Object in data with message
    fun ok(msg: String, data: T): Response<T> {
        this.msg = msg
        this.data = data
        return this
    }
    // TO Return Object only
    fun ok(data: T): T {
        return data
    }
}
