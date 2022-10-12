package com.cogoport.ares.api.common.service.interfaces

import java.io.File

interface DownloadService {
    suspend fun downloadDocument(documentId: Long): File
}
