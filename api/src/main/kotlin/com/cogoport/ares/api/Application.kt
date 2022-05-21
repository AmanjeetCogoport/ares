package com.cogoport.ares.api

import com.cogoport.brahma.opensearch.Client
import com.cogoport.brahma.opensearch.Configuration
import io.micronaut.runtime.Micronaut.build
fun main(args: Array<String>) {
    build()
        .args(*args)
        .packages("com.cogoport.ares")
        .start()
    val configuration = Configuration(
        host = "search.books.dev.cogoport.io",
        user = "books_search_dev",
        pass = "F20guhTISeCQ93nRaD@"
    )
    Client.configure(configuration)
}
