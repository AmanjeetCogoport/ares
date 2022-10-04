package com.cogoport.ares.api.common.service.implementation

import com.cogoport.ares.api.common.service.interfaces.DownloadService
import com.cogoport.ares.api.payment.repository.AresDocumentRepository
import com.cogoport.brahma.s3.client.S3Client
import jakarta.inject.Inject
import java.io.File
import java.net.URLDecoder
import java.nio.file.Files
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DownloadServiceImpl : DownloadService {

    @Inject
    lateinit var aresDocumentRepository: AresDocumentRepository

    @Inject
    lateinit var s3Client: S3Client

    override suspend fun downloadDocument(documentId: Long): File {
        val document = aresDocumentRepository.findById(documentId)
        val url = URLDecoder.decode(document?.documentUrl, "UTF-8")
        val inputStreamFile = s3Client.download(url)
        val documentName = (document?.documentName)?.substringBeforeLast('.')
        val fileName = "${documentName}_${document?.id}_Error_List_" +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_hhmmss")) +
            ".${document?.documentType}"
        val file = File("/tmp/$fileName")
        Files.copy(inputStreamFile.inputStream(), file.toPath())
        return file
    }
}
