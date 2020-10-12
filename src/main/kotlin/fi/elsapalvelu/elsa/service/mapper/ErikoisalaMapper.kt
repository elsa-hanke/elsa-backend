package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.Erikoisala
import fi.elsapalvelu.elsa.service.dto.ErikoisalaDTO
import org.mapstruct.Mapper

/**
 * Mapper for the entity [Erikoisala] and its DTO [ErikoisalaDTO].
 */
@Mapper(componentModel = "spring", uses = [])
interface ErikoisalaMapper :
    EntityMapper<ErikoisalaDTO, Erikoisala> {

    override fun toEntity(erikoisalaDTO: ErikoisalaDTO): Erikoisala

    @JvmDefault
    fun fromId(id: Long?) = id?.let {
        val erikoisala = Erikoisala()
        erikoisala.id = id
        erikoisala
    }
}
