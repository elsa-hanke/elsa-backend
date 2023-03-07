package fi.elsapalvelu.elsa.web.rest.errors

open class BadRequestAlertException(
    val defaultMessage: String,
    val entityName: String,
    val errorKey: String
) : RuntimeException() {
}
