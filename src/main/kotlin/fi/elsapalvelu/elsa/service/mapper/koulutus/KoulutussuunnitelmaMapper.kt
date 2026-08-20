package fi.elsapalvelu.elsa.service.mapper.koulutus

import fi.elsapalvelu.elsa.domain.koulutus.Koulutussuunnitelma
import fi.elsapalvelu.elsa.service.dto.koulutus.KoulutussuunnitelmaDTO
import org.mapstruct.*

import fi.elsapalvelu.elsa.service.mapper.EntityMapper
import fi.elsapalvelu.elsa.service.mapper.kayttaja.AsiakirjaMapper
import fi.elsapalvelu.elsa.service.mapper.kayttaja.OpintooikeusMapper
@Mapper(
    componentModel = "spring",
    uses = [
        OpintooikeusMapper::class,
        AsiakirjaMapper::class
    ],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface KoulutussuunnitelmaMapper :
    EntityMapper<KoulutussuunnitelmaDTO, Koulutussuunnitelma> {

    @Mappings(
        Mapping(
            source = "koulutussuunnitelmaAsiakirja",
            target = "koulutussuunnitelmaAsiakirja"
        ),
        Mapping(source = "motivaatiokirjeAsiakirja", target = "motivaatiokirjeAsiakirja")
    )
    override fun toDto(entity: Koulutussuunnitelma): KoulutussuunnitelmaDTO

    @Mappings(
        Mapping(target = "koulutusjaksot", ignore = true)
    )
    override fun toEntity(dto: KoulutussuunnitelmaDTO): Koulutussuunnitelma

    @Named("id")
    @BeanMapping(ignoreByDefault = true)
    @Mappings(
        Mapping(target = "id", source = "id")
    )
    fun toDtoId(koulutussuunnitelma: Koulutussuunnitelma): KoulutussuunnitelmaDTO
}
