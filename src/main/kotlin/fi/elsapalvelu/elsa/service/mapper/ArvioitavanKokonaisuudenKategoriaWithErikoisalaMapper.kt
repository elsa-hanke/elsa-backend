package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.ArvioitavanKokonaisuudenKategoria
import fi.elsapalvelu.elsa.service.dto.ArvioitavanKokonaisuudenKategoriaWithErikoisalaDTO
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring",
    uses = [
        ArvioitavaKokonaisuusMapper::class,
        ErikoisalaMapper::class
    ],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface ArvioitavanKokonaisuudenKategoriaWithErikoisalaMapper :
    EntityMapper<ArvioitavanKokonaisuudenKategoriaWithErikoisalaDTO, ArvioitavanKokonaisuudenKategoria> {

    override fun toEntity(dto: ArvioitavanKokonaisuudenKategoriaWithErikoisalaDTO): ArvioitavanKokonaisuudenKategoria

    override fun toDto(entity: ArvioitavanKokonaisuudenKategoria): ArvioitavanKokonaisuudenKategoriaWithErikoisalaDTO

    fun fromId(id: Long?) = id?.let {
        val arvioitavanKokonaisuudenKategoria = ArvioitavanKokonaisuudenKategoria()
        arvioitavanKokonaisuudenKategoria.id = id
        arvioitavanKokonaisuudenKategoria
    }
}
