package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.ArvioitavaOsaalue
import fi.elsapalvelu.elsa.service.dto.ArvioitavaOsaalueDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring",
    uses = [ArvioitavaKokonaisuusMapper::class],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface ArvioitavaOsaalueMapper :
    EntityMapper<ArvioitavaOsaalueDTO, ArvioitavaOsaalue> {

    @Mappings(
        Mapping(source = "arvioitavaKokonaisuus.id", target = "arvioitavaKokonaisuusId")
    )
    override fun toDto(entity: ArvioitavaOsaalue): ArvioitavaOsaalueDTO

    @Mappings(
        Mapping(source = "arvioitavaKokonaisuusId", target = "arvioitavaKokonaisuus")
    )
    override fun toEntity(dto: ArvioitavaOsaalueDTO): ArvioitavaOsaalue

    fun fromId(id: Long?) = id?.let {
        val arvioitavaOsaalue = ArvioitavaOsaalue()
        arvioitavaOsaalue.id = id
        arvioitavaOsaalue
    }
}
