package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.Pikaviesti
import fi.elsapalvelu.elsa.service.dto.PikaviestiDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings

/**
 * Mapper for the entity [Pikaviesti] and its DTO [PikaviestiDTO].
 */
@Mapper(componentModel = "spring", uses = [PikaviestiKeskusteluMapper::class, KayttajaMapper::class])
interface PikaviestiMapper :
    EntityMapper<PikaviestiDTO, Pikaviesti> {

    @Mappings(
        Mapping(source = "keskustelu.id", target = "keskusteluId"),
        Mapping(source = "lahettaja.id", target = "lahettajaId")
    )
    override fun toDto(pikaviesti: Pikaviesti): PikaviestiDTO

    @Mappings(
        Mapping(source = "keskusteluId", target = "keskustelu"),
        Mapping(source = "lahettajaId", target = "lahettaja")
    )
    override fun toEntity(pikaviestiDTO: PikaviestiDTO): Pikaviesti

    @JvmDefault
    fun fromId(id: Long?) = id?.let {
        val pikaviesti = Pikaviesti()
        pikaviesti.id = id
        pikaviesti
    }
}
