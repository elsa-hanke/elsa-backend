package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.PaivakirjaAihekategoria
import fi.elsapalvelu.elsa.service.dto.PaivakirjaAihekategoriaDTO
import org.mapstruct.*

@Mapper(componentModel = "spring", uses = [], unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface PaivakirjaAihekategoriaMapper :
    EntityMapper<PaivakirjaAihekategoriaDTO, PaivakirjaAihekategoria> {

    @Named("idSet")
    @BeanMapping(ignoreByDefault = true)
    @Mappings(
        Mapping(target = "id", source = "id")
    )
    fun toDtoIdSet(paivakirjaAihekategoria: Set<PaivakirjaAihekategoria>): Set<PaivakirjaAihekategoriaDTO>
}
