package fi.elsapalvelu.elsa.service.mapper.kayttaja

import fi.elsapalvelu.elsa.domain.kayttaja.AsiakirjaData
import fi.elsapalvelu.elsa.service.dto.kayttaja.AsiakirjaDataDTO
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

import fi.elsapalvelu.elsa.service.mapper.EntityMapper
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface AsiakirjaDataMapper : EntityMapper<AsiakirjaDataDTO, AsiakirjaData> {

    fun fromId(id: Long?) = id?.let {
        val asiakirjaData = AsiakirjaData()
        asiakirjaData.id = id
        asiakirjaData
    }
}

