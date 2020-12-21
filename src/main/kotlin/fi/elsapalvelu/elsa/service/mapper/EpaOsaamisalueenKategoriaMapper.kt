package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.EpaOsaamisalueenKategoria
import fi.elsapalvelu.elsa.service.dto.EpaOsaamisalueenKategoriaDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring",
    uses = [
        EpaOsaamisalueMapper::class
    ],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface EpaOsaamisalueenKategoriaMapper :
    EntityMapper<EpaOsaamisalueenKategoriaDTO, EpaOsaamisalueenKategoria> {

    @Mappings(
        Mapping(target = "epaOsaamisalueet", ignore = true),
        Mapping(target = "removeEpaOsaamisalue", ignore = true),
    )
    override fun toEntity(dto: EpaOsaamisalueenKategoriaDTO): EpaOsaamisalueenKategoria

    @JvmDefault
    fun fromId(id: Long?) = id?.let {
        val epaOsaamisalueenKategoria = EpaOsaamisalueenKategoria()
        epaOsaamisalueenKategoria.id = id
        epaOsaamisalueenKategoria
    }
}
