package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.PikaviestiKeskustelu
import fi.elsapalvelu.elsa.service.dto.PikaviestiKeskusteluDTO
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring",
    uses = [KayttajaMapper::class],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface PikaviestiKeskusteluMapper :
    EntityMapper<PikaviestiKeskusteluDTO, PikaviestiKeskustelu> {

    override fun toEntity(dto: PikaviestiKeskusteluDTO): PikaviestiKeskustelu

    @JvmDefault
    fun fromId(id: Long?) = id?.let {
        val pikaviestiKeskustelu = PikaviestiKeskustelu()
        pikaviestiKeskustelu.id = id
        pikaviestiKeskustelu
    }
}
