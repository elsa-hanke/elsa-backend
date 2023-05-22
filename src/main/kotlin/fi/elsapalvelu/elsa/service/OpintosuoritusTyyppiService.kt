package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.OpintosuoritusTyyppiDTO

interface OpintosuoritusTyyppiService {

    fun findAll(): List<OpintosuoritusTyyppiDTO>?
}
