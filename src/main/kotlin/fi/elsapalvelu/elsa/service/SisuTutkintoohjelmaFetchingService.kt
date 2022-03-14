package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.impl.Qualifications

interface SisuTutkintoohjelmaFetchingService {
    suspend fun fetch(): Qualifications?
}
