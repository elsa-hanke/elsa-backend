package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.Keskeytysaika
import fi.elsapalvelu.elsa.service.dto.KeskeytysaikaDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring",
    uses = [
        PoissaolonSyyMapper::class,
        TyoskentelyjaksoMapper::class
    ],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface KeskeytysaikaMapper :
    EntityMapper<KeskeytysaikaDTO, Keskeytysaika> {

    @Mappings(
        Mapping(source = "poissaolonSyy.id", target = "poissaolonSyyId"),
        Mapping(source = "tyoskentelyjakso.id", target = "tyoskentelyjaksoId")
    )
    override fun toDto(entity: Keskeytysaika): KeskeytysaikaDTO

    @Mappings(
        Mapping(source = "poissaolonSyyId", target = "poissaolonSyy"),
        Mapping(source = "tyoskentelyjaksoId", target = "tyoskentelyjakso")
    )
    override fun toEntity(dto: KeskeytysaikaDTO): Keskeytysaika

    @JvmDefault
    fun fromId(id: Long?) = id?.let {
        val keskeytysaika = Keskeytysaika()
        keskeytysaika.id = id
        keskeytysaika
    }
}
