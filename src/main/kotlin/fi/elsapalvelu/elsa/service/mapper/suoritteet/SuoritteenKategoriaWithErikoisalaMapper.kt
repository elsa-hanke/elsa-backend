package fi.elsapalvelu.elsa.service.mapper.suoritteet

import fi.elsapalvelu.elsa.domain.suoritteet.SuoritteenKategoria
import fi.elsapalvelu.elsa.service.dto.suoritteet.SuoritteenKategoriaWithErikoisalaDTO
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

import fi.elsapalvelu.elsa.service.mapper.EntityMapper
import fi.elsapalvelu.elsa.service.mapper.perustiedot.ErikoisalaMapper
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
