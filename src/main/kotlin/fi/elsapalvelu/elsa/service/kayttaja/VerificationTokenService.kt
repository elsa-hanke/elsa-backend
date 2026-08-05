package fi.elsapalvelu.elsa.service.kayttaja

interface VerificationTokenService {

    fun save(userId: String): String

    fun findOne(userId: String): String?
}
