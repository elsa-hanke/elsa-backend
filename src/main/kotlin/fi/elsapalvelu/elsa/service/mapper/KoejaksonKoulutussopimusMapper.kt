package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.KoejaksonKoulutussopimus
import fi.elsapalvelu.elsa.service.dto.KoejaksonKoulutussopimusDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.ReportingPolicy


@Mapper(
    componentModel = "spring",
    uses = [KayttajaMapper::class, KoulutussopimuksenKouluttajaMapper::class],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface KoejaksonKoulutussopimusMapper :
    EntityMapper<KoejaksonKoulutussopimusDTO, KoejaksonKoulutussopimus> {

    @Mappings(
        Mapping(source = "vastuuhenkilonNimi", target = "vastuuhenkilo.nimi"),
        Mapping(source = "vastuuhenkilonNimike", target = "vastuuhenkilo.nimike"),
        Mapping(source = "vastuuhenkiloHyvaksynyt", target = "vastuuhenkilo.sopimusHyvaksytty"),
        Mapping(source = "vastuuhenkilonKuittausaika", target = "vastuuhenkilo.kuittausaika"),
        Mapping(source = "vastuuhenkilo.id", target = "vastuuhenkilo.id"),
    )
    override fun toDto(entity: KoejaksonKoulutussopimus): KoejaksonKoulutussopimusDTO

    @Mappings(
        Mapping(source = "vastuuhenkilo.nimi", target = "vastuuhenkilonNimi"),
        Mapping(source = "vastuuhenkilo.nimike", target = "vastuuhenkilonNimike"),
        Mapping(source = "vastuuhenkilo.sopimusHyvaksytty", target = "vastuuhenkiloHyvaksynyt"),
        Mapping(source = "vastuuhenkilo.kuittausaika", target = "vastuuhenkilonKuittausaika"),
        Mapping(source = "vastuuhenkilo.id", target = "vastuuhenkilo.id")
    )
    override fun toEntity(dto: KoejaksonKoulutussopimusDTO): KoejaksonKoulutussopimus

    @JvmDefault
    fun fromId(id: Long?) = id?.let {
        val koulutussopimus = KoejaksonKoulutussopimus()
        koulutussopimus.id = id
        koulutussopimus
    }
}