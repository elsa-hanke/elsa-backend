package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.KayttajaYliopistoErikoisala
import fi.elsapalvelu.elsa.service.dto.KayttajaYliopistoErikoisalaDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring",
    uses = [
        KayttajaMapper::class,
        YliopistoMapper::class,
        ErikoisalaMapper::class,
        VastuuhenkilonTehtavatyyppiMapper::class
    ],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface KayttajaYliopistoErikoisalaMapper :
    EntityMapper<KayttajaYliopistoErikoisalaDTO, KayttajaYliopistoErikoisala> {

    @Mappings(
        Mapping(source = "kayttaja.id", target = "kayttajaId")
    )
    override fun toDto(entity: KayttajaYliopistoErikoisala): KayttajaYliopistoErikoisalaDTO
}
