package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.EpaOsaamisalueenKategoria
import fi.elsapalvelu.elsa.service.dto.EpaOsaamisalueenKategoriaSimpleDTO
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring",
    uses = [],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface EpaOsaamisalueenKategoriaSimpleMapper :
    EntityMapper<EpaOsaamisalueenKategoriaSimpleDTO, EpaOsaamisalueenKategoria> {

    override fun toEntity(dto: EpaOsaamisalueenKategoriaSimpleDTO): EpaOsaamisalueenKategoria

    @JvmDefault
    fun fromId(id: Long?) = id?.let {
        val epaOsaamisalueenKategoria = EpaOsaamisalueenKategoria()
        epaOsaamisalueenKategoria.id = id
        epaOsaamisalueenKategoria
    }
}
