package fi.elsapalvelu.elsa.service.mapper.koejakso

import fi.elsapalvelu.elsa.domain.koejakso.KoulutussopimuksenKoulutuspaikka
import fi.elsapalvelu.elsa.service.dto.koejakso.KoulutussopimuksenKoulutuspaikkaDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.ReportingPolicy

import fi.elsapalvelu.elsa.service.mapper.EntityMapper
import fi.elsapalvelu.elsa.service.mapper.perustiedot.YliopistoMapper
@Mapper(
    componentModel = "spring",
    uses = [YliopistoMapper::class],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface KoulutussopimuksenKoulutuspaikkaMapper :
    EntityMapper<KoulutussopimuksenKoulutuspaikkaDTO, KoulutussopimuksenKoulutuspaikka> {

    @Mappings(
        Mapping(source = "yliopisto.id", target = "yliopistoId"),
        Mapping(source = "yliopisto.nimi", target = "yliopisto")
    )
    override fun toDto(entity: KoulutussopimuksenKoulutuspaikka): KoulutussopimuksenKoulutuspaikkaDTO

    @Mappings(
        Mapping(source = "yliopistoId", target = "yliopisto")
    )
    override fun toEntity(dto: KoulutussopimuksenKoulutuspaikkaDTO): KoulutussopimuksenKoulutuspaikka

    fun fromId(id: Long?) = id?.let {
        val koulutussopimuksenKoulutuspaikka = KoulutussopimuksenKoulutuspaikka()
        koulutussopimuksenKoulutuspaikka.id = id
        koulutussopimuksenKoulutuspaikka
    }
}
