package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.Yliopisto
import fi.elsapalvelu.elsa.service.dto.YliopistoDTO
import org.mapstruct.*

@Mapper(
    componentModel = "spring",
    uses = [
        ErikoisalaMapper::class
    ],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface YliopistoMapper :
    EntityMapper<YliopistoDTO, Yliopisto> {

    override fun toEntity(dto: YliopistoDTO): Yliopisto

    fun fromId(id: Long?) = id?.let {
        val yliopisto = Yliopisto()
        yliopisto.id = id
        yliopisto
    }

    @Named("id")
    @BeanMapping(ignoreByDefault = true)
    @Mappings(
        Mapping(target = "id", source = "id")
    )
    fun toDtoId(yliopisto: Yliopisto): YliopistoDTO

    @Named("idSet")
    @BeanMapping(ignoreByDefault = true)
    @Mappings(
        Mapping(target = "id", source = "id")
    )
    fun toDtoIdSet(yliopisto: Set<Yliopisto>): Set<YliopistoDTO>
}
