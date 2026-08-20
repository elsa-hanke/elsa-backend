package fi.elsapalvelu.elsa.service.mapper.koulutus

import fi.elsapalvelu.elsa.domain.koulutus.Opintosuoritus
import fi.elsapalvelu.elsa.service.dto.koulutus.OpintosuoritusDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.ReportingPolicy

import fi.elsapalvelu.elsa.service.mapper.EntityMapper@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface OpintosuoritusMapper : EntityMapper<OpintosuoritusDTO, Opintosuoritus> {

    @Mappings(
        Mapping(target = "osakokonaisuudet", ignore = true)
    )
    override fun toEntity(dto: OpintosuoritusDTO): Opintosuoritus

}
