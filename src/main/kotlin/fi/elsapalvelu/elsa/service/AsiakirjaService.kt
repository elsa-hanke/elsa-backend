package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.AsiakirjaDTO

interface AsiakirjaService {

    fun create(
        asiakirjat: List<AsiakirjaDTO>,
        kayttajaId: String,
        tyoskentelyJaksoId: Long? = null
    ): List<AsiakirjaDTO>?

    fun findAllByErikoistuvaLaakariId(kayttajaId: String): List<AsiakirjaDTO>

    fun findAllByErikoistuvaLaakariIdAndTyoskentelyjaksoId(
        kayttajaId: String,
        tyoskentelyJaksoId: Long?
    ): List<AsiakirjaDTO>

    fun findOne(id: Long, kayttajaId: String): AsiakirjaDTO?

    fun delete(id: Long, kayttajaId: String)

    fun delete(ids: List<Long>, kayttajaId: String)

    fun removeTyoskentelyjaksoReference(kayttajaId: String, tyoskentelyJaksoId: Long?)

}
