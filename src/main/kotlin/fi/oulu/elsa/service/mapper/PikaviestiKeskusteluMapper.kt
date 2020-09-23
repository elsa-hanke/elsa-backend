package fi.oulu.elsa.service.mapper

import fi.oulu.elsa.domain.PikaviestiKeskustelu
import fi.oulu.elsa.service.dto.PikaviestiKeskusteluDTO
import org.mapstruct.Mapper

/**
 * Mapper for the entity [PikaviestiKeskustelu] and its DTO [PikaviestiKeskusteluDTO].
 */
@Mapper(componentModel = "spring", uses = [KayttajaMapper::class])
interface PikaviestiKeskusteluMapper :
    EntityMapper<PikaviestiKeskusteluDTO, PikaviestiKeskustelu> {

    override fun toEntity(pikaviestiKeskusteluDTO: PikaviestiKeskusteluDTO): PikaviestiKeskustelu

    @JvmDefault
    fun fromId(id: Long?) = id?.let {
        val pikaviestiKeskustelu = PikaviestiKeskustelu()
        pikaviestiKeskustelu.id = id
        pikaviestiKeskustelu
    }
}
