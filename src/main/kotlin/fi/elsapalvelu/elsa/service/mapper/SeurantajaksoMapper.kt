package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.KoejaksonAloituskeskustelu
import fi.elsapalvelu.elsa.domain.Seurantajakso
import fi.elsapalvelu.elsa.service.dto.SeurantajaksoDTO
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy


@Mapper(
    componentModel = "spring",
    uses = [KayttajaMapper::class],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface SeurantajaksoMapper :
    EntityMapper<SeurantajaksoDTO, Seurantajakso> {

    override fun toDto(entity: Seurantajakso): SeurantajaksoDTO

    override fun toEntity(dto: SeurantajaksoDTO): Seurantajakso

    fun fromId(id: Long?) = id?.let {
        val aloituskeskustelu = KoejaksonAloituskeskustelu()
        aloituskeskustelu.id = id
        aloituskeskustelu
    }
}