package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.SeurantajaksoDTO

interface SeurantajaksoService {

    fun create(
        seurantajaksoDTO: SeurantajaksoDTO,
        userId: String
    ): SeurantajaksoDTO

    fun update(
        seurantajaksoDTO: SeurantajaksoDTO,
        userId: String
    ): SeurantajaksoDTO

    fun findByErikoistuvaLaakariKayttajaUserId(
        userId: String
    ): List<SeurantajaksoDTO>

    fun findOne(id: Long, userId: String): SeurantajaksoDTO?

    fun delete(id: Long, userId: String)
}
