package fi.elsapalvelu.elsa.service.koulutus

import fi.elsapalvelu.elsa.service.dto.koulutus.OpintosuoritusTyyppiDTO

interface OpintosuoritusTyyppiService {

    fun findAll(): List<OpintosuoritusTyyppiDTO>?
}
