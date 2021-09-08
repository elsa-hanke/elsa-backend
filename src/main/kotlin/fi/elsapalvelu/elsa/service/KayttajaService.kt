package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.KayttajaDTO
import fi.elsapalvelu.elsa.service.dto.KayttooikeusHakemusDTO
import java.security.Principal
import java.util.*

interface KayttajaService {

    fun save(kayttajaDTO: KayttajaDTO): KayttajaDTO

    fun findAll(): List<KayttajaDTO>

    fun findOne(id: String): Optional<KayttajaDTO>

    fun delete(id: String)

    fun findKouluttajat(): List<KayttajaDTO>

    fun findVastuuhenkilot(): List<KayttajaDTO>

    fun findKouluttajatAndVastuuhenkilot(kayttajaId: String): List<KayttajaDTO>

    fun updateKayttajaAuthorities(
        principal: Principal?,
        kayttooikeusHakemusDTO: KayttooikeusHakemusDTO
    )

    fun getAuthenticatedKayttaja(principal: Principal?): KayttajaDTO

    fun existsByEmail(sahkopostiosoite: String): Boolean
}
