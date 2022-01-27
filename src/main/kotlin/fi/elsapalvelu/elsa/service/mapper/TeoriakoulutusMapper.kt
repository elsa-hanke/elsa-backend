package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.Teoriakoulutus
import fi.elsapalvelu.elsa.service.dto.TeoriakoulutusDTO
import org.mapstruct.*

@Mapper(
    componentModel = "spring", uses = [],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface TeoriakoulutusMapper :
    EntityMapper<TeoriakoulutusDTO, Teoriakoulutus> {

    override fun toDto(entity: Teoriakoulutus): TeoriakoulutusDTO

    override fun toEntity(dto: TeoriakoulutusDTO): Teoriakoulutus

    @Named("id")
    @BeanMapping(ignoreByDefault = true)
    @Mappings(
        Mapping(target = "id", source = "id")
    )
    fun toDtoId(teoriakoulutus: Teoriakoulutus): TeoriakoulutusDTO

    fun fromId(id: Long?) = id?.let {
        val teoriakoulutus = Teoriakoulutus()
        teoriakoulutus.id = id
        teoriakoulutus
    }
}
