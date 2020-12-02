package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.PoissaolonSyy
import fi.elsapalvelu.elsa.service.dto.PoissaolonSyyDTO
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring",
    uses = [],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface PoissaolonSyyMapper :
    EntityMapper<PoissaolonSyyDTO, PoissaolonSyy> {

    override fun toEntity(dto: PoissaolonSyyDTO): PoissaolonSyy

    @JvmDefault
    fun fromId(id: Long?) = id?.let {
        val poissaolonSyy = PoissaolonSyy()
        poissaolonSyy.id = id
        poissaolonSyy
    }
}
