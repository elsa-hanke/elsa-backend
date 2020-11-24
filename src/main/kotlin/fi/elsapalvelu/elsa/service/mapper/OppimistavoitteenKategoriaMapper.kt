package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.OppimistavoitteenKategoria
import fi.elsapalvelu.elsa.service.dto.OppimistavoitteenKategoriaDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring",
    uses = [
        ErikoisalaMapper::class,
        OppimistavoiteMapper::class
    ],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface OppimistavoitteenKategoriaMapper :
    EntityMapper<OppimistavoitteenKategoriaDTO, OppimistavoitteenKategoria> {

    @Mappings(
        Mapping(source = "erikoisala.id", target = "erikoisalaId")
    )
    override fun toDto(entity: OppimistavoitteenKategoria): OppimistavoitteenKategoriaDTO

    @Mappings(
        Mapping(target = "oppimistavoitteet", ignore = true),
        Mapping(target = "removeOppimistavoite", ignore = true),
        Mapping(source = "erikoisalaId", target = "erikoisala")
    )
    override fun toEntity(dto: OppimistavoitteenKategoriaDTO): OppimistavoitteenKategoria

    @JvmDefault
    fun fromId(id: Long?) = id?.let {
        val oppimistavoitteenKategoria = OppimistavoitteenKategoria()
        oppimistavoitteenKategoria.id = id
        oppimistavoitteenKategoria
    }
}
