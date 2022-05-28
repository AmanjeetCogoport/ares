package com.cogoport.ares.api

import io.micronaut.runtime.Micronaut.build
fun main(args: Array<String>) {
    build()
        .args(*args)
        .packages("com.cogoport.ares")
        .start()
}
