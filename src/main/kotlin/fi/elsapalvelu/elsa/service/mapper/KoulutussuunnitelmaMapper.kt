package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.Koulutussuunnitelma
import fi.elsapalvelu.elsa.service.dto.KoulutussuunnitelmaDTO
import org.mapstruct.*

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
