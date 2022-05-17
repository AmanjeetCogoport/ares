package com.cogoport.ares.utils.exception

import com.cogoport.ares.utils.code.AresError

class AresException(var error: AresError, var context: String) : RuntimeException()
