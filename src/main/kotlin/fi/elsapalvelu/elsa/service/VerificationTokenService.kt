package fi.elsapalvelu.elsa.service

interface VerificationTokenService {

    fun save(userId: String): String
}
