package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.SuoritusarvioinninArvioitavaKokonaisuus
import fi.elsapalvelu.elsa.service.dto.SuoritusarvioinninArvioitavaKokonaisuusDTO
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
interface SuoritusarvioinninArvioitavaKokonaisuusMapper :
    EntityMapper<SuoritusarvioinninArvioitavaKokonaisuusDTO, SuoritusarvioinninArvioitavaKokonaisuus> {

    @Mappings(
        Mapping(source = "arvioitavaKokonaisuus.id", target = "arvioitavaKokonaisuusId"),
        Mapping(source = "suoritusarviointi.id", target = "suoritusarviointiId")
    )
    override fun toDto(entity: SuoritusarvioinninArvioitavaKokonaisuus): SuoritusarvioinninArvioitavaKokonaisuusDTO

    @Mappings(
        Mapping(source = "arvioitavaKokonaisuusId", target = "arvioitavaKokonaisuus"),
        Mapping(source = "suoritusarviointiId", target = "suoritusarviointi.id")
    )
    override fun toEntity(dto: SuoritusarvioinninArvioitavaKokonaisuusDTO): SuoritusarvioinninArvioitavaKokonaisuus

    fun fromId(id: Long?) = id?.let {
        val suoritusarvioinninArvioitavaKokonaisuus = SuoritusarvioinninArvioitavaKokonaisuus()
        suoritusarvioinninArvioitavaKokonaisuus.id = id
        suoritusarvioinninArvioitavaKokonaisuus
    }
}
