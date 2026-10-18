package fi.elsapalvelu.elsa.service.valmistuminen

import java.io.ByteArrayOutputStream
import java.io.Closeable

/**
 * An open assembly of PDF documents into a single PDF. Documents can be added one after another
 * without rewriting the part already assembled; call [finish] to close the assembly and get the
 * finished document. Closing without finishing discards the assembly.
 *
 * Obtained from [PdfService.openAssembler]. The caller is responsible for closing it.
 */
interface PdfAssembler : Closeable {

    /** The number of pages assembled so far. */
    val pages: Int

    fun add(pdf: ByteArray)

    fun add(pdf: ByteArrayOutputStream) = add(pdf.toByteArray())

    /**
     * Closes the assembly and returns the finished PDF as bytes.
     */
    fun finish(): ByteArray
}
