package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.Suorite
import fi.elsapalvelu.elsa.service.dto.SuoriteDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.ReportingPolicy

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
