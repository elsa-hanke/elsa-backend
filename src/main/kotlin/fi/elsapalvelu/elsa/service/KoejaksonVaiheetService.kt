package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.KoejaksonVaiheDTO

interface KoejaksonVaiheetService {
    fun findAllByKouluttajaKayttajaId(kayttajaId: String): List<KoejaksonVaiheDTO>

    fun findAllByVastuuhenkiloKayttajaId(kayttajaId: String): List<KoejaksonVaiheDTO>
}
