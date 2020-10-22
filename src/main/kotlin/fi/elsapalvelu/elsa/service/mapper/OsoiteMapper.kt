package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.Osoite
import fi.elsapalvelu.elsa.service.dto.OsoiteDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring",
    uses = [ErikoistuvaLaakariMapper::class],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface OsoiteMapper :
    EntityMapper<OsoiteDTO, Osoite> {

    @Mappings(
        Mapping(source = "erikoistuvaLaakari.id", target = "erikoistuvaLaakariId")
    )
    override fun toDto(entity: Osoite): OsoiteDTO

    @Mappings(
        Mapping(source = "erikoistuvaLaakariId", target = "erikoistuvaLaakari")
    )
    override fun toEntity(dto: OsoiteDTO): Osoite

    @JvmDefault
    fun fromId(id: Long?) = id?.let {
        val osoite = Osoite()
        osoite.id = id
        osoite
    }
}
