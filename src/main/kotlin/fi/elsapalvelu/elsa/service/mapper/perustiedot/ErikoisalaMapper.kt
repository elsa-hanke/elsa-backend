package fi.elsapalvelu.elsa.service.mapper.perustiedot

import fi.elsapalvelu.elsa.domain.perustiedot.Erikoisala
import fi.elsapalvelu.elsa.service.dto.perustiedot.ErikoisalaDTO
import org.mapstruct.*

import fi.elsapalvelu.elsa.service.mapper.EntityMapper
@Mapper(
    componentModel = "spring",
    uses = [],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface ErikoisalaMapper :
    EntityMapper<ErikoisalaDTO, Erikoisala> {

    override fun toEntity(dto: ErikoisalaDTO): Erikoisala

    fun fromId(id: Long?) = id?.let {
        val erikoisala = Erikoisala()
        erikoisala.id = id
        erikoisala
    }

    @Named("id")
    @BeanMapping(ignoreByDefault = true)
    @Mappings(
        Mapping(target = "id", source = "id")
    )
    fun toDtoId(erikoisala: Erikoisala): ErikoisalaDTO

    @Named("idSet")
    @BeanMapping(ignoreByDefault = true)
    @Mappings(
        Mapping(target = "id", source = "id")
    )
    fun toDtoIdSet(erikoisala: Set<Erikoisala>): Set<ErikoisalaDTO>
}
