package fi.elsapalvelu.elsa.service

interface VerificationTokenService {

    fun save(kayttajaId: String): String
}
