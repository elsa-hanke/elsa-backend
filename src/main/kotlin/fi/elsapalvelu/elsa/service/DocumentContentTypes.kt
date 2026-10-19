package fi.elsapalvelu.elsa.service

internal object DocumentContentTypes {
    fun canonical(contentType: String?): String? =
        contentType?.substringBefore(';')?.trim()?.lowercase()?.let {
            if (it == "image/jpg") "image/jpeg" else it
        }

    fun isImage(contentType: String?): Boolean = canonical(contentType) in setOf("image/jpeg", "image/png")
}
