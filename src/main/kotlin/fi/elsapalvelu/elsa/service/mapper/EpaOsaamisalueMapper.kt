package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.EpaOsaamisalue
import fi.elsapalvelu.elsa.service.dto.EpaOsaamisalueDTO
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

@Mapper(componentModel = "spring", uses = [], unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface EpaOsaamisalueMapper :
    EntityMapper<EpaOsaamisalueDTO, EpaOsaamisalue> {

    override fun toEntity(dto: EpaOsaamisalueDTO): EpaOsaamisalue

    @JvmDefault
    fun fromId(id: Long?) = id?.let {
        val epaOsaamisalue = EpaOsaamisalue()
        epaOsaamisalue.id = id
        epaOsaamisalue
    }
}
