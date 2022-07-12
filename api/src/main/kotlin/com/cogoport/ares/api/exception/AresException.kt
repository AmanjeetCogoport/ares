package com.cogoport.ares.api.exception

class AresException(val error: AresError, val context: String) : RuntimeException()
