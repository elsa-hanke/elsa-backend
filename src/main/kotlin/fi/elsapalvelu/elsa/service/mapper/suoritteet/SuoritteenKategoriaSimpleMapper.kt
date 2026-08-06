package fi.elsapalvelu.elsa.service.mapper.suoritteet

import fi.elsapalvelu.elsa.domain.suoritteet.SuoritteenKategoria
import fi.elsapalvelu.elsa.service.dto.suoritteet.SuoritteenKategoriaSimpleDTO
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

import fi.elsapalvelu.elsa.service.mapper.EntityMapper
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
