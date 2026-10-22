package fi.elsapalvelu.elsa.service.mapper.arviointi

import fi.elsapalvelu.elsa.service.RichTextHtmlSanitizer
import org.mapstruct.AfterMapping
import org.mapstruct.MappingTarget

import fi.elsapalvelu.elsa.domain.arviointi.ArvioitavaKokonaisuus
import fi.elsapalvelu.elsa.service.dto.arviointi.ArvioitavaKokonaisuusWithErikoisalaDTO
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

import fi.elsapalvelu.elsa.service.mapper.EntityMapper
@Mapper(
    componentModel = "spring",
    uses = [
        ArvioitavanKokonaisuudenKategoriaWithErikoisalaMapper::class
    ],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface ArvioitavaKokonaisuusWithErikoisalaMapper :
    EntityMapper<ArvioitavaKokonaisuusWithErikoisalaDTO, ArvioitavaKokonaisuus> {

    override fun toDto(entity: ArvioitavaKokonaisuus): ArvioitavaKokonaisuusWithErikoisalaDTO

    override fun toEntity(dto: ArvioitavaKokonaisuusWithErikoisalaDTO): ArvioitavaKokonaisuus

    fun fromId(id: Long?) = id?.let {
        val arvioitavaKokonaisuus = ArvioitavaKokonaisuus()
        arvioitavaKokonaisuus.id = id
        arvioitavaKokonaisuus
    }

    @AfterMapping
    fun sanitizeDto(@MappingTarget dto: ArvioitavaKokonaisuusWithErikoisalaDTO) {
        dto.kuvaus = RichTextHtmlSanitizer.sanitize(dto.kuvaus)
        dto.kuvausSv = RichTextHtmlSanitizer.sanitize(dto.kuvausSv)
    }

    @AfterMapping
    fun sanitizeEntity(source: ArvioitavaKokonaisuusWithErikoisalaDTO, @MappingTarget entity: ArvioitavaKokonaisuus) {
        entity.kuvaus = RichTextHtmlSanitizer.sanitize(source.kuvaus ?: entity.kuvaus)
        entity.kuvausSv = RichTextHtmlSanitizer.sanitize(source.kuvausSv ?: entity.kuvausSv)
    }
}
