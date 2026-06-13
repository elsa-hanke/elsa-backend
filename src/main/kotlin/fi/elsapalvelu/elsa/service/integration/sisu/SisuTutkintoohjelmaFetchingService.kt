package fi.elsapalvelu.elsa.service.integration.sisu

import fi.elsapalvelu.elsa.service.integration.sisu.Qualifications

interface SisuTutkintoohjelmaFetchingService {
    suspend fun fetch(): Qualifications?
}
