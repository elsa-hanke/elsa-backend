package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.Koulutussuunnitelma
import fi.elsapalvelu.elsa.service.dto.KoulutussuunnitelmaDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring",
    uses = [
        ErikoistuvaLaakariMapper::class,
        AsiakirjaMapper::class
    ],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface KoulutussuunnitelmaMapper :
    EntityMapper<KoulutussuunnitelmaDTO, Koulutussuunnitelma> {

    @Mappings(
        Mapping(source = "erikoistuvaLaakari.id", target = "erikoistuvaLaakariId"),
        Mapping(
            source = "koulutussuunnitelmaAsiakirja",
            target = "koulutussuunnitelmaAsiakirja",
            qualifiedByName = ["id"]
        ),
        Mapping(source = "motivaatiokirjeAsiakirja", target = "motivaatiokirjeAsiakirja", qualifiedByName = ["id"]),

        )
    override fun toDto(entity: Koulutussuunnitelma): KoulutussuunnitelmaDTO

    @Mappings(
        Mapping(source = "erikoistuvaLaakariId", target = "erikoistuvaLaakari"),
        Mapping(target = "koulutusjaksot", ignore = true),
    )
    override fun toEntity(dto: KoulutussuunnitelmaDTO): Koulutussuunnitelma


}
