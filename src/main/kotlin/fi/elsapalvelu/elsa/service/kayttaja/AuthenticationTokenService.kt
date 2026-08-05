package fi.elsapalvelu.elsa.service.kayttaja

interface AuthenticationTokenService {
    fun getCachedTokenOrRequestNew(): String?

    fun requestToken(): String?
}
