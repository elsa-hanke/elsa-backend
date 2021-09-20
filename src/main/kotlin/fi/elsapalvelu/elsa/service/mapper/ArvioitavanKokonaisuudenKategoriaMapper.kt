package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.ArvioitavanKokonaisuudenKategoria
import fi.elsapalvelu.elsa.service.dto.ArvioitavanKokonaisuudenKategoriaDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring",
    uses = [
        ArvioitavaKokonaisuusMapper::class
    ],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface ArvioitavanKokonaisuudenKategoriaMapper :
    EntityMapper<ArvioitavanKokonaisuudenKategoriaDTO, ArvioitavanKokonaisuudenKategoria> {

    @Mappings(
        Mapping(target = "arvioitavatKokonaisuudet", ignore = true)
    )
    override fun toEntity(dto: ArvioitavanKokonaisuudenKategoriaDTO): ArvioitavanKokonaisuudenKategoria

    fun fromId(id: Long?) = id?.let {
        val arvioitavanKokonaisuudenKategoria = ArvioitavanKokonaisuudenKategoria()
        arvioitavanKokonaisuudenKategoria.id = id
        arvioitavanKokonaisuudenKategoria
    }
}
