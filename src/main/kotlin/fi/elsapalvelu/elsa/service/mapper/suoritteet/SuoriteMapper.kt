package fi.elsapalvelu.elsa.service.mapper.suoritteet

import fi.elsapalvelu.elsa.domain.suoritteet.Suorite
import fi.elsapalvelu.elsa.service.dto.suoritteet.SuoriteDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.ReportingPolicy

import fi.elsapalvelu.elsa.service.mapper.EntityMapper
@Mapper(
    componentModel = "spring",
    uses = [
        SuoritteenKategoriaMapper::class
    ],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface SuoriteMapper :
    EntityMapper<SuoriteDTO, Suorite> {

    @Mappings(
        Mapping(source = "kategoria.id", target = "kategoriaId")
    )
    override fun toDto(entity: Suorite): SuoriteDTO

    @Mappings(
        Mapping(source = "kategoriaId", target = "kategoria")
    )
    override fun toEntity(dto: SuoriteDTO): Suorite

    fun fromId(id: Long?) = id?.let {
        val suorite = Suorite()
        suorite.id = id
        suorite
    }
}
