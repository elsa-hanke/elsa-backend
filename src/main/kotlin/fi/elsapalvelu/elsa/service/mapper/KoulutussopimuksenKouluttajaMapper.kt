package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.KoulutussopimuksenKouluttaja
import fi.elsapalvelu.elsa.service.dto.KoulutussopimuksenKouluttajaDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring",
    uses = [KayttajaMapper::class],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface KoulutussopimuksenKouluttajaMapper :
    EntityMapper<KoulutussopimuksenKouluttajaDTO, KoulutussopimuksenKouluttaja> {

    @Mappings(
        Mapping(source = "kouluttaja.id", target = "kayttajaId"),
        Mapping(source = "kouluttaja.user.id", target = "kayttajaUserId"),
        Mapping(source = "kouluttaja.user.email", target = "sahkoposti"),
        Mapping(source = "kouluttaja.user.phoneNumber", target = "puhelin"),
        Mapping(
            target = "nimi",
            expression = "java(entity.getKouluttaja() == null ? \"\" : entity.getKouluttaja().getNimi())"
        ),
        Mapping(source = "kouluttaja.nimike", target = "nimike")
    )
    override fun toDto(entity: KoulutussopimuksenKouluttaja): KoulutussopimuksenKouluttajaDTO

    @Mappings(
        Mapping(source = "kayttajaId", target = "kouluttaja")
    )
    override fun toEntity(dto: KoulutussopimuksenKouluttajaDTO): KoulutussopimuksenKouluttaja

    fun fromId(id: Long?) = id?.let {
        val koulutussopimuksenKouluttaja = KoulutussopimuksenKouluttaja()
        koulutussopimuksenKouluttaja.id = id
        koulutussopimuksenKouluttaja
    }
}
