package fi.elsapalvelu.elsa.service.integration.sisu

interface SisuTutkintoohjelmaFetchingService {
    suspend fun fetch(): Qualifications?
}
