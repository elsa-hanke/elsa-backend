package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.KoejaksoDTO
import fi.elsapalvelu.elsa.service.dto.TyoskentelyjaksoDTO
import java.util.*

interface KoejaksoService {

    fun save(koejaksoDTO: KoejaksoDTO): KoejaksoDTO

    fun findAll(): MutableList<KoejaksoDTO>

    fun findByErikoistuvaLaakariKayttajaUserId(userId: String): KoejaksoDTO

    fun findOne(id: Long): Optional<KoejaksoDTO>

    fun delete(id: Long)

    fun addTyoskentelyjakso(
        koejaksoId: Long,
        tyoskentelyjaksoId: Long,
        userId: String
    ): TyoskentelyjaksoDTO

    fun removeTyoskentelyjakso(koejaksoId: Long, tyoskentelyjaksoId: Long, userId: String)
}
