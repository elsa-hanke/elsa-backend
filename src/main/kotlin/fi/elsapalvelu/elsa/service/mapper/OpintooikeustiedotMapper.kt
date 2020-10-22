package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.Opintooikeustiedot
import fi.elsapalvelu.elsa.service.dto.OpintooikeustiedotDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.ReportingPolicy

@Mapper(componentModel = "spring", uses = [YliopistoMapper::class], unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface OpintooikeustiedotMapper :
    EntityMapper<OpintooikeustiedotDTO, Opintooikeustiedot> {

    @Mappings(
        Mapping(source = "yliopisto.id", target = "yliopistoId")
    )
    override fun toDto(entity: Opintooikeustiedot): OpintooikeustiedotDTO

    @Mappings(
        Mapping(source = "yliopistoId", target = "yliopisto")
    )
    override fun toEntity(dto: OpintooikeustiedotDTO): Opintooikeustiedot

    @JvmDefault
    fun fromId(id: Long?) = id?.let {
        val opintooikeustiedot = Opintooikeustiedot()
        opintooikeustiedot.id = id
        opintooikeustiedot
    }
}
