package fi.elsapalvelu.elsa.service.mapper.seuranta

import fi.elsapalvelu.elsa.domain.seuranta.PaivakirjaAihekategoria
import fi.elsapalvelu.elsa.service.dto.seuranta.PaivakirjaAihekategoriaDTO
import org.mapstruct.*

import fi.elsapalvelu.elsa.service.mapper.EntityMapper
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
