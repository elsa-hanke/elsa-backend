package fi.elsapalvelu.elsa.service.mapper.koulutus

import fi.elsapalvelu.elsa.domain.koulutus.OpintosuoritusKurssikoodi
import fi.elsapalvelu.elsa.service.dto.koulutus.OpintosuoritusKurssikoodiDTO
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

import fi.elsapalvelu.elsa.service.mapper.EntityMapper
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
