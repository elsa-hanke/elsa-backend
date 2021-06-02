package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.KoejaksonLoppukeskustelu
import fi.elsapalvelu.elsa.domain.KoejaksonVastuuhenkilonArvio
import fi.elsapalvelu.elsa.service.dto.KoejaksonLoppukeskusteluDTO
import fi.elsapalvelu.elsa.service.dto.KoejaksonVastuuhenkilonArvioDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.ReportingPolicy


@Mapper(
    componentModel = "spring",
    uses = [KayttajaMapper::class],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface KoejaksonVastuuhenkilonArvioMapper :
    EntityMapper<KoejaksonVastuuhenkilonArvioDTO, KoejaksonVastuuhenkilonArvio> {

    @Mappings(
        Mapping(source = "vastuuhenkilo.id", target = "vastuuhenkilo.id"),
        Mapping(source = "vastuuhenkilonNimi", target = "vastuuhenkilo.nimi"),
        Mapping(source = "vastuuhenkiloHyvaksynyt", target = "vastuuhenkilo.sopimusHyvaksytty"),
        Mapping(source = "vastuuhenkilonKuittausaika", target = "vastuuhenkilo.kuittausaika")
    )
    override fun toDto(entity: KoejaksonVastuuhenkilonArvio): KoejaksonVastuuhenkilonArvioDTO

    @Mappings(
        Mapping(source = "vastuuhenkilo.id", target = "vastuuhenkilo"),
        Mapping(source = "vastuuhenkilo.nimi", target = "vastuuhenkilonNimi"),
        Mapping(source = "vastuuhenkilo.sopimusHyvaksytty", target = "vastuuhenkiloHyvaksynyt"),
        Mapping(source = "vastuuhenkilo.kuittausaika", target = "vastuuhenkilonKuittausaika")
    )
    override fun toEntity(dto: KoejaksonVastuuhenkilonArvioDTO): KoejaksonVastuuhenkilonArvio

    @JvmDefault
    fun fromId(id: Long?) = id?.let {
        val loppukeskustelu = KoejaksonLoppukeskustelu()
        loppukeskustelu.id = id
        loppukeskustelu
    }
}
