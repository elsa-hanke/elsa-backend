package fi.elsapalvelu.elsa.service

interface AuthenticationTokenService {
    fun getCachedTokenOrRequestNew(): String?

    fun requestToken(): String?
}
