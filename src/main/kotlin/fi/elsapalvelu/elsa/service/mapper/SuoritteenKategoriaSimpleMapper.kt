package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.SuoritteenKategoria
import fi.elsapalvelu.elsa.service.dto.SuoritteenKategoriaSimpleDTO
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring",
    uses = [],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface SuoritteenKategoriaSimpleMapper :
    EntityMapper<SuoritteenKategoriaSimpleDTO, SuoritteenKategoria> {

    override fun toDto(entity: SuoritteenKategoria): SuoritteenKategoriaSimpleDTO

    override fun toEntity(dto: SuoritteenKategoriaSimpleDTO): SuoritteenKategoria

    fun fromId(id: Long?) = id?.let {
        val suoritteenKategoria = SuoritteenKategoria()
        suoritteenKategoria.id = id
        suoritteenKategoria
    }
}
