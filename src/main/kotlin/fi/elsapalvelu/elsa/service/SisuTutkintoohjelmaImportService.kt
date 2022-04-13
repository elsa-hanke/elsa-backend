package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.impl.Qualifications

interface SisuTutkintoohjelmaImportService {
    fun import(qualifications: Qualifications)
}
