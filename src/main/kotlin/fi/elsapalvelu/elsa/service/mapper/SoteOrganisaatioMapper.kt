package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.SoteOrganisaatio
import fi.elsapalvelu.elsa.service.dto.SoteOrganisaatioDTO
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

@Mapper(componentModel = "spring", uses = [], unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface SoteOrganisaatioMapper :
    EntityMapper<SoteOrganisaatioDTO, SoteOrganisaatio> {

    override fun toEntity(dto: SoteOrganisaatioDTO): SoteOrganisaatio

    @JvmDefault
    fun fromId(organizationId: String?) = organizationId?.let {
        val soteOrganisaatio = SoteOrganisaatio()
        soteOrganisaatio.organizationId = organizationId
        soteOrganisaatio
    }
}
