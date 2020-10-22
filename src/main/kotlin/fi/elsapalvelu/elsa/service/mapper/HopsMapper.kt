package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.Hops
import fi.elsapalvelu.elsa.service.dto.HopsDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring",
    uses = [ErikoistuvaLaakariMapper::class],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface HopsMapper :
    EntityMapper<HopsDTO, Hops> {

    @Mappings(
        Mapping(source = "erikoistuvaLaakari.id", target = "erikoistuvaLaakariId")
    )
    override fun toDto(entity: Hops): HopsDTO

    @Mappings(
        Mapping(source = "erikoistuvaLaakariId", target = "erikoistuvaLaakari")
    )
    override fun toEntity(dto: HopsDTO): Hops

    @JvmDefault
    fun fromId(id: Long?) = id?.let {
        val hops = Hops()
        hops.id = id
        hops
    }
}
