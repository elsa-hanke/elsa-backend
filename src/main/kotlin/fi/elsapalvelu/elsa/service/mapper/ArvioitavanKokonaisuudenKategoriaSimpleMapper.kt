package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.ArvioitavanKokonaisuudenKategoria
import fi.elsapalvelu.elsa.service.dto.ArvioitavanKokonaisuudenKategoriaSimpleDTO
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring",
    uses = [],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface ArvioitavanKokonaisuudenKategoriaSimpleMapper :
    EntityMapper<ArvioitavanKokonaisuudenKategoriaSimpleDTO, ArvioitavanKokonaisuudenKategoria> {

    override fun toEntity(dto: ArvioitavanKokonaisuudenKategoriaSimpleDTO): ArvioitavanKokonaisuudenKategoria

    override fun toDto(entity: ArvioitavanKokonaisuudenKategoria): ArvioitavanKokonaisuudenKategoriaSimpleDTO

    fun fromId(id: Long?) = id?.let {
        val arvioitavanKokonaisuudenKategoria = ArvioitavanKokonaisuudenKategoria()
        arvioitavanKokonaisuudenKategoria.id = id
        arvioitavanKokonaisuudenKategoria
    }
}
