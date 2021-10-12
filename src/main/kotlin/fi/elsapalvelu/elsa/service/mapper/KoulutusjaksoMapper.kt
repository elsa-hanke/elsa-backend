package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.Koulutusjakso
import fi.elsapalvelu.elsa.service.dto.KoulutusjaksoDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring", uses = [
        TyoskentelyjaksoMapper::class,
        ArvioitavaKokonaisuusMapper::class,
        KoulutussuunnitelmaMapper::class
    ],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface KoulutusjaksoMapper :
    EntityMapper<KoulutusjaksoDTO, Koulutusjakso> {

    @Mappings(
        Mapping(source = "tyoskentelyjaksot", target = "tyoskentelyjaksot", qualifiedByName = ["idSet"]),
        Mapping(source = "osaamistavoitteet", target = "osaamistavoitteet", qualifiedByName = ["idSet"]),
        Mapping(target = "koulutussuunnitelma", source = "koulutussuunnitelma", qualifiedByName = ["id"])
    )
    override fun toDto(entity: Koulutusjakso): KoulutusjaksoDTO
}
