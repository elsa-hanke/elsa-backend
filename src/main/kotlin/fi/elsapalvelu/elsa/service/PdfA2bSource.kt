package fi.elsapalvelu.elsa.service

/** An original attachment; sources are consumed sequentially during archive assembly. */
internal data class PdfA2bSource(val data: ByteArray?, val contentType: String?)
