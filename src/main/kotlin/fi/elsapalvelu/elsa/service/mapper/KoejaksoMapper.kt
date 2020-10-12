package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.Koejakso
import fi.elsapalvelu.elsa.service.dto.KoejaksoDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings

/**
 * Mapper for the entity [Koejakso] and its DTO [KoejaksoDTO].
 */
@Mapper(componentModel = "spring", uses = [ErikoistuvaLaakariMapper::class, KayttajaMapper::class])
interface KoejaksoMapper :
    EntityMapper<KoejaksoDTO, Koejakso> {

    @Mappings(
        Mapping(source = "erikoistuvaLaakari.id", target = "erikoistuvaLaakariId"),
        Mapping(source = "lahikouluttaja.id", target = "lahikouluttajaId"),
        Mapping(source = "vastuuhenkilo.id", target = "vastuuhenkiloId")
    )
    override fun toDto(koejakso: Koejakso): KoejaksoDTO

    @Mappings(
        Mapping(source = "erikoistuvaLaakariId", target = "erikoistuvaLaakari"),
        Mapping(source = "lahikouluttajaId", target = "lahikouluttaja"),
        Mapping(source = "vastuuhenkiloId", target = "vastuuhenkilo")
    )
    override fun toEntity(koejaksoDTO: KoejaksoDTO): Koejakso

    @JvmDefault
    fun fromId(id: Long?) = id?.let {
        val koejakso = Koejakso()
        koejakso.id = id
        koejakso
    }
}
