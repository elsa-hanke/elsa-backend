package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.EpaOsaamisalue
import fi.elsapalvelu.elsa.service.dto.EpaOsaamisalueDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring",
    uses = [
        ErikoisalaMapper::class,
        EpaOsaamisalueenKategoriaSimpleMapper::class
    ],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface EpaOsaamisalueMapper :
    EntityMapper<EpaOsaamisalueDTO, EpaOsaamisalue> {

    @Mappings(
        Mapping(source = "erikoisala.id", target = "erikoisalaId")
    )
    override fun toDto(entity: EpaOsaamisalue): EpaOsaamisalueDTO

    @Mappings(
        Mapping(target = "arvioitavatOsaalueet", ignore = true),
        Mapping(target = "removeArvioitavaOsaalue", ignore = true),
        Mapping(source = "erikoisalaId", target = "erikoisala")
    )
    override fun toEntity(dto: EpaOsaamisalueDTO): EpaOsaamisalue

    @JvmDefault
    fun fromId(id: Long?) = id?.let {
        val epaOsaamisalue = EpaOsaamisalue()
        epaOsaamisalue.id = id
        epaOsaamisalue
    }
}
