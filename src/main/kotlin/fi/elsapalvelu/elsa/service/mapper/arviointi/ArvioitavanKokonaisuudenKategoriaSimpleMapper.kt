package fi.elsapalvelu.elsa.service.mapper.arviointi

import fi.elsapalvelu.elsa.domain.arviointi.ArvioitavanKokonaisuudenKategoria
import fi.elsapalvelu.elsa.service.dto.arviointi.ArvioitavanKokonaisuudenKategoriaSimpleDTO
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

import fi.elsapalvelu.elsa.service.mapper.EntityMapper
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
