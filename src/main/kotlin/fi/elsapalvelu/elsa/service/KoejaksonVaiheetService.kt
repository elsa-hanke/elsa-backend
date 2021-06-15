package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.KoejaksonVaiheDTO

interface KoejaksonVaiheetService {
    fun findAllByKouluttajaKayttajaUserId(userId: String): List<KoejaksonVaiheDTO>

    fun findAllByVastuuhenkiloKayttajaUserId(userId: String): List<KoejaksonVaiheDTO>
}
