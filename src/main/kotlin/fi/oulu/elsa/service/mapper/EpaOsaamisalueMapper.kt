package fi.oulu.elsa.service.mapper

import fi.oulu.elsa.domain.EpaOsaamisalue
import fi.oulu.elsa.service.dto.EpaOsaamisalueDTO
import org.mapstruct.Mapper

/**
 * Mapper for the entity [EpaOsaamisalue] and its DTO [EpaOsaamisalueDTO].
 */
@Mapper(componentModel = "spring", uses = [])
interface EpaOsaamisalueMapper :
    EntityMapper<EpaOsaamisalueDTO, EpaOsaamisalue> {

    override fun toEntity(epaOsaamisalueDTO: EpaOsaamisalueDTO): EpaOsaamisalue

    @JvmDefault
    fun fromId(id: Long?) = id?.let {
        val epaOsaamisalue = EpaOsaamisalue()
        epaOsaamisalue.id = id
        epaOsaamisalue
    }
}
