package com.cogoport.ares.exception

class AresException(var error: AresError, var context: String) : RuntimeException()
