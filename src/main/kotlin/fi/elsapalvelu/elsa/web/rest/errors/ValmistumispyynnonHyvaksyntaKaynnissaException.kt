package fi.elsapalvelu.elsa.web.rest.errors

/**
 * Thrown when a valmistumispyynto cannot be approved because another approval of the same
 * valmistumispyynto is already running (it holds the row lock). Mapped to HTTP 409 by
 * [BadRequestExceptionAdvice]; the UI shows the translation of [ERROR_KEY].
 */
class ValmistumispyynnonHyvaksyntaKaynnissaException(
    val valmistumispyyntoId: Long,
    cause: Throwable? = null
) : RuntimeException(
    "Valmistumispyynnon hyvaksynta on jo kaynnissa [valmistumispyyntoId=$valmistumispyyntoId]",
    cause
) {
    companion object {
        const val ERROR_KEY = "dataillegal.valmistumispyynnon-hyvaksynta-on-jo-kaynnissa"
    }
}
