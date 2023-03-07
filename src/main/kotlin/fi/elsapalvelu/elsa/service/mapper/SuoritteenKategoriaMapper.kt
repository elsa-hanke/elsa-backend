package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.SuoritteenKategoria
import fi.elsapalvelu.elsa.service.dto.SuoritteenKategoriaDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring",
    uses = [
        ErikoisalaMapper::class
    ],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface SuoritteenKategoriaMapper :
    EntityMapper<SuoritteenKategoriaDTO, SuoritteenKategoria> {

    @Mappings(
        Mapping(source = "erikoisala.id", target = "erikoisalaId")
    )
    override fun toDto(entity: SuoritteenKategoria): SuoritteenKategoriaDTO

    @Mappings(
        Mapping(target = "suoritteet", ignore = true),
        Mapping(source = "erikoisalaId", target = "erikoisala")
    )
    override fun toEntity(dto: SuoritteenKategoriaDTO): SuoritteenKategoria

    fun fromId(id: Long?) = id?.let {
        val suoritteenKategoria = SuoritteenKategoria()
        suoritteenKategoria.id = id
        suoritteenKategoria
    }
}
