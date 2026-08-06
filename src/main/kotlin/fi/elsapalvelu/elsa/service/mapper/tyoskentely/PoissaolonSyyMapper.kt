package fi.elsapalvelu.elsa.service.mapper.tyoskentely

import fi.elsapalvelu.elsa.domain.tyoskentely.PoissaolonSyy
import fi.elsapalvelu.elsa.service.dto.tyoskentely.PoissaolonSyyDTO
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

import fi.elsapalvelu.elsa.service.mapper.EntityMapper
@Mapper(
    componentModel = "spring",
    uses = [],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface PoissaolonSyyMapper :
    EntityMapper<PoissaolonSyyDTO, PoissaolonSyy> {

    override fun toEntity(dto: PoissaolonSyyDTO): PoissaolonSyy

    fun fromId(id: Long?) = id?.let {
        val poissaolonSyy = PoissaolonSyy()
        poissaolonSyy.id = id
        poissaolonSyy
    }
}
