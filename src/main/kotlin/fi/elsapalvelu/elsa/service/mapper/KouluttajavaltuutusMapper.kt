package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.Kouluttajavaltuutus
import fi.elsapalvelu.elsa.service.dto.KouluttajavaltuutusDTO
import org.mapstruct.Mapper
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

    override fun toDto(entity: Kouluttajavaltuutus): KouluttajavaltuutusDTO

    override fun toEntity(dto: KouluttajavaltuutusDTO): Kouluttajavaltuutus

    fun fromId(id: Long?) = id?.let {
        val kouluttajavaltuutus = Kouluttajavaltuutus()
        kouluttajavaltuutus.id = id
        kouluttajavaltuutus
    }
}
