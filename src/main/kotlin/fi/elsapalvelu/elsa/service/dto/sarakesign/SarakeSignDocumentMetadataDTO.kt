package fi.elsapalvelu.elsa.service.dto.sarakesign

import java.io.Serializable

data class SarakeSignDocumentMetadataDTO(

    /*
    Indicates the type of request document
    1 = Document is an attachment and is not going to be signed
    2 = Document will be signed and a separate signature page will be appended to it
    3 = Document will be signed and overlay fields will be added to it
     */
    var signatureMode: Long? = null,

    /*
    Confidentiality of the document. This flag is effective only when document is archived:
    0 = Non-confidential: Owner and keyusers can read the document
    1 = Confidential: Owner can read the document. Keyusers can see document's metadata, but cannot read document content
    2 = Secret: Owner can read the document. Keyusers do not see the document
     */
    var confidentiality: Long? = null,

    var title: String? = null,

    var description: String? = null,

    var orderNumber: Long? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SarakeSignDocumentMetadataDTO

        if (title != other.title) return false

        return true
    }

    override fun hashCode() = 31
}
