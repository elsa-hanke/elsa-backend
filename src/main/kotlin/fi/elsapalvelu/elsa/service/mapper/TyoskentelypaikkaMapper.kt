package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.Tyoskentelypaikka
import fi.elsapalvelu.elsa.service.dto.TyoskentelypaikkaDTO
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring",
    uses = [],
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface TyoskentelypaikkaMapper :
    EntityMapper<TyoskentelypaikkaDTO, Tyoskentelypaikka> {

    override fun toEntity(tyoskentelypaikkaDTO: TyoskentelypaikkaDTO): Tyoskentelypaikka

    @JvmDefault
    fun fromId(id: Long?) = id?.let {
        val tyoskentelypaikka = Tyoskentelypaikka()
        tyoskentelypaikka.id = id
        tyoskentelypaikka
    }
}
