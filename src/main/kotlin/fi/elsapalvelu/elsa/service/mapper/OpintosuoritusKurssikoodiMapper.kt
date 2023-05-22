package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.OpintosuoritusKurssikoodi
import fi.elsapalvelu.elsa.service.dto.OpintosuoritusKurssikoodiDTO
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring",
    uses = [OpintosuoritusTyyppiMapper::class],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface OpintosuoritusKurssikoodiMapper :
    EntityMapper<OpintosuoritusKurssikoodiDTO, OpintosuoritusKurssikoodi> {

    override fun toEntity(dto: OpintosuoritusKurssikoodiDTO): OpintosuoritusKurssikoodi

    fun fromId(id: Long?) = id?.let {
        val kurssikoodi = OpintosuoritusKurssikoodi()
        kurssikoodi.id = id
        kurssikoodi
    }
}
