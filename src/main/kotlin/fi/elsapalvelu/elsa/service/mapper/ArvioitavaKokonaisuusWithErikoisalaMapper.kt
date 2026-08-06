package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.arviointi.ArvioitavaKokonaisuus
import fi.elsapalvelu.elsa.service.dto.arviointi.ArvioitavaKokonaisuusWithErikoisalaDTO
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring",
    uses = [
        ArvioitavanKokonaisuudenKategoriaWithErikoisalaMapper::class
    ],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface ArvioitavaKokonaisuusWithErikoisalaMapper :
    EntityMapper<ArvioitavaKokonaisuusWithErikoisalaDTO, ArvioitavaKokonaisuus> {

    override fun toDto(entity: ArvioitavaKokonaisuus): ArvioitavaKokonaisuusWithErikoisalaDTO

    override fun toEntity(dto: ArvioitavaKokonaisuusWithErikoisalaDTO): ArvioitavaKokonaisuus

    fun fromId(id: Long?) = id?.let {
        val arvioitavaKokonaisuus = ArvioitavaKokonaisuus()
        arvioitavaKokonaisuus.id = id
        arvioitavaKokonaisuus
    }
}
