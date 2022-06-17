package com.cogoport.ares.api.exception

class AresException(var error: AresError, var context: String) : RuntimeException()
