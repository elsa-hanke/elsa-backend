package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.Kouluttajavaltuutus
import fi.elsapalvelu.elsa.service.dto.KouluttajavaltuutusDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring",
    uses = [
        ErikoistuvaLaakariMapper::class,
        KayttajaMapper::class
    ],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface KouluttajavaltuutusMapper :
    EntityMapper<KouluttajavaltuutusDTO, Kouluttajavaltuutus> {

    @Mappings(
        Mapping(source = "valtuuttaja.id", target = "valtuuttajaId"),
        Mapping(source = "valtuutettu.id", target = "valtuutettuId")
    )
    override fun toDto(entity: Kouluttajavaltuutus): KouluttajavaltuutusDTO

    @Mappings(
        Mapping(source = "valtuuttajaId", target = "valtuuttaja"),
        Mapping(source = "valtuutettuId", target = "valtuutettu")
    )
    override fun toEntity(dto: KouluttajavaltuutusDTO): Kouluttajavaltuutus

    @JvmDefault
    fun fromId(id: Long?) = id?.let {
        val kouluttajavaltuutus = Kouluttajavaltuutus()
        kouluttajavaltuutus.id = id
        kouluttajavaltuutus
    }
}
