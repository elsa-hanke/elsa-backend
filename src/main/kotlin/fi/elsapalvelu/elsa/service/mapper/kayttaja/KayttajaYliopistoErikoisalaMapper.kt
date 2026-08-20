package fi.elsapalvelu.elsa.service.mapper.kayttaja

import fi.elsapalvelu.elsa.domain.kayttaja.KayttajaYliopistoErikoisala
import fi.elsapalvelu.elsa.service.dto.kayttaja.KayttajaYliopistoErikoisalaDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.ReportingPolicy

import fi.elsapalvelu.elsa.service.mapper.EntityMapper
import fi.elsapalvelu.elsa.service.mapper.perustiedot.ErikoisalaMapper
import fi.elsapalvelu.elsa.service.mapper.perustiedot.VastuuhenkilonTehtavatyyppiMapper
import fi.elsapalvelu.elsa.service.mapper.perustiedot.YliopistoMapper
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
