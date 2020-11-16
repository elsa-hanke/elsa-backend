package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.Oppimistavoite
import fi.elsapalvelu.elsa.service.dto.OppimistavoiteDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring",
    uses = [
        OppimistavoitteenKategoriaMapper::class
    ],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface OppimistavoiteMapper :
    EntityMapper<OppimistavoiteDTO, Oppimistavoite> {

    @Mappings(
        Mapping(source = "kategoria.id", target = "kategoriaId")
    )
    override fun toDto(entity: Oppimistavoite): OppimistavoiteDTO

    @Mappings(
        Mapping(source = "kategoriaId", target = "kategoria")
    )
    override fun toEntity(dto: OppimistavoiteDTO): Oppimistavoite

    @JvmDefault
    fun fromId(id: Long?) = id?.let {
        val oppimistavoite = Oppimistavoite()
        oppimistavoite.id = id
        oppimistavoite
    }
}
