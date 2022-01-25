package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.Asiakirja
import fi.elsapalvelu.elsa.service.dto.AsiakirjaDTO
import org.mapstruct.*

@Mapper(
    componentModel = "spring",
    uses = [
        TyoskentelyjaksoMapper::class,
        OpintooikeusMapper::class,
        AsiakirjaDataMapper::class
    ],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface AsiakirjaMapper : EntityMapper<AsiakirjaDTO, Asiakirja> {

    @Mappings(
        Mapping(source = "tyoskentelyjakso.id", target = "tyoskentelyjaksoId"),
    )
    override fun toDto(entity: Asiakirja): AsiakirjaDTO

    @Mappings(
        Mapping(source = "tyoskentelyjaksoId", target = "tyoskentelyjakso")
    )
    override fun toEntity(dto: AsiakirjaDTO): Asiakirja

    fun fromId(id: Long?) = id?.let {
        val asiakirja = Asiakirja()
        asiakirja.id = id
        asiakirja
    }

    @Named("id")
    @BeanMapping(ignoreByDefault = true)
    @Mappings(
        Mapping(target = "id", source = "id")
    )
    fun toDtoId(asiakirja: Asiakirja): AsiakirjaDTO
}
