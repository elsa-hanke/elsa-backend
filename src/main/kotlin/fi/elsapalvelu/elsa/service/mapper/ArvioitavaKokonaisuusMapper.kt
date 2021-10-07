package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.ArvioitavaKokonaisuus
import fi.elsapalvelu.elsa.service.dto.ArvioitavaKokonaisuusDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring",
    uses = [
        ArviointiasteikkoMapper::class,
        ErikoisalaMapper::class,
        ArvioitavanKokonaisuudenKategoriaSimpleMapper::class
    ],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface ArvioitavaKokonaisuusMapper :
    EntityMapper<ArvioitavaKokonaisuusDTO, ArvioitavaKokonaisuus> {

    @Mappings(
        Mapping(source = "erikoisala.id", target = "erikoisalaId"),
        Mapping(source = "erikoisala.arviointiasteikko", target = "arviointiasteikko")
    )
    override fun toDto(entity: ArvioitavaKokonaisuus): ArvioitavaKokonaisuusDTO

    @Mappings(
        Mapping(source = "erikoisalaId", target = "erikoisala")
    )
    override fun toEntity(dto: ArvioitavaKokonaisuusDTO): ArvioitavaKokonaisuus

    fun fromId(id: Long?) = id?.let {
        val arvioitavaKokonaisuus = ArvioitavaKokonaisuus()
        arvioitavaKokonaisuus.id = id
        arvioitavaKokonaisuus
    }
}
