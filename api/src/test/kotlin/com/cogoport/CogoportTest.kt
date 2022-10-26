package com.cogoport
import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions

@MicronautTest
class CogoportTest {

    @Inject
    lateinit var application: EmbeddedApplication<*>

    // @Test
    fun testItWorks() {
        Assertions.assertTrue(application.isRunning)
    }
}