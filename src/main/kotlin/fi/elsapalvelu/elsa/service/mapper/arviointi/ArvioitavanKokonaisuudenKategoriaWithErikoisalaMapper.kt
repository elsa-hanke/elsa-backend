package fi.elsapalvelu.elsa.service.mapper.arviointi

import fi.elsapalvelu.elsa.domain.arviointi.ArvioitavanKokonaisuudenKategoria
import fi.elsapalvelu.elsa.service.dto.arviointi.ArvioitavanKokonaisuudenKategoriaWithErikoisalaDTO
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

import fi.elsapalvelu.elsa.service.mapper.EntityMapper
import fi.elsapalvelu.elsa.service.mapper.perustiedot.ErikoisalaMapper
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
