package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.SoteOrganisaatio
import fi.elsapalvelu.elsa.service.dto.SoteOrganisaatioDTO
import org.mapstruct.Mapper

@Mapper(componentModel = "spring", uses = [])
interface SoteOrganisaatioMapper :
    EntityMapper<SoteOrganisaatioDTO, SoteOrganisaatio> {

    override fun toEntity(soteOrganisaatioDTO: SoteOrganisaatioDTO): SoteOrganisaatio

    @JvmDefault
    fun fromId(organizationId: String?) = organizationId?.let {
        val soteOrganisaatio = SoteOrganisaatio()
        soteOrganisaatio.organizationId = organizationId
        soteOrganisaatio
    }
}
