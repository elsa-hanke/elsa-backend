package fi.elsapalvelu.elsa.service

interface SisuTreAccessTokenProvider {
    fun token(): String?
}
