package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.AsiakirjaData
import fi.elsapalvelu.elsa.service.dto.AsiakirjaDataDTO
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface AsiakirjaDataMapper : EntityMapper<AsiakirjaDataDTO, AsiakirjaData> {

    @JvmDefault
    fun fromId(id: Long?) = id?.let {
        val asiakirjaData = AsiakirjaData()
        asiakirjaData.id = id
        asiakirjaData
    }
}

