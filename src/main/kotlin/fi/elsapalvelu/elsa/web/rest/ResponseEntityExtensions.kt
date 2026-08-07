package fi.elsapalvelu.elsa.web.rest

import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import java.io.InputStream

/**
 * Builds a file-download [ResponseEntity] from this [InputStream].
 * Sets Content-Disposition (attachment) and Content-Type headers,
 * reads the stream, and closes it.
 *
 * Replaces the repeated 7-line response-building block that appeared in
 * every file-download endpoint across the web layer.
 */
fun InputStream.toFileDownloadResponse(fileName: String, contentType: String): ResponseEntity<ByteArray> =
    use {
        ResponseEntity.ok()
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"${URLEncoder.encode(fileName, "UTF-8")}\""
            )
            .header(HttpHeaders.CONTENT_TYPE, "$contentType; charset=UTF-8")
            .body(it.readBytes())
    }

