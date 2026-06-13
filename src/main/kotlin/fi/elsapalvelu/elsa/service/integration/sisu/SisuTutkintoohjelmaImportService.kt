package fi.elsapalvelu.elsa.service.integration.sisu

import fi.elsapalvelu.elsa.service.integration.sisu.Qualifications

interface SisuTutkintoohjelmaImportService {
    fun import(qualifications: Qualifications)
}
