package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.Koejakso
import fi.elsapalvelu.elsa.service.dto.KoejaksoDTO
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring",
    uses = [
        TyoskentelyjaksoMapper::class,
        ErikoistuvaLaakariMapper::class,
        KoejaksonKoulutussopimusMapper::class
    ],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface KoejaksoMapper :
    EntityMapper<KoejaksoDTO, Koejakso> {

    override fun toDto(entity: Koejakso): KoejaksoDTO

    override fun toEntity(dto: KoejaksoDTO): Koejakso

    @JvmDefault
    fun fromId(id: Long?) = id?.let {
        val koejakso = Koejakso()
        koejakso.id = id
        koejakso
    }
}
