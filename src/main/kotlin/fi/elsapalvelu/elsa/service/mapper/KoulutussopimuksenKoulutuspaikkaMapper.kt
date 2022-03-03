package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.KoulutussopimuksenKoulutuspaikka
import fi.elsapalvelu.elsa.service.dto.KoulutussopimuksenKoulutuspaikkaDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring",
    uses = [YliopistoMapper::class],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface KoulutussopimuksenKoulutuspaikkaMapper :
    EntityMapper<KoulutussopimuksenKoulutuspaikkaDTO, KoulutussopimuksenKoulutuspaikka> {

    @Mappings(
        Mapping(source = "yliopisto.id", target = "yliopistoId"),
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
