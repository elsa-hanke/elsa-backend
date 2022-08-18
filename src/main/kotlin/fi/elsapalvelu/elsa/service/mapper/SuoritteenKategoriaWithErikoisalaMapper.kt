package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.SuoritteenKategoria
import fi.elsapalvelu.elsa.service.dto.SuoritteenKategoriaWithErikoisalaDTO
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring",
    uses = [ErikoisalaMapper::class],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface SuoritteenKategoriaWithErikoisalaMapper :
    EntityMapper<SuoritteenKategoriaWithErikoisalaDTO, SuoritteenKategoria> {

    override fun toDto(entity: SuoritteenKategoria): SuoritteenKategoriaWithErikoisalaDTO

    override fun toEntity(dto: SuoritteenKategoriaWithErikoisalaDTO): SuoritteenKategoria

    fun fromId(id: Long?) = id?.let {
        val suoritteenKategoria = SuoritteenKategoria()
        suoritteenKategoria.id = id
        suoritteenKategoria
    }
}
