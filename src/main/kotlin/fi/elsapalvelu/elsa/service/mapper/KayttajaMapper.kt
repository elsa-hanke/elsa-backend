package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.Kayttaja
import fi.elsapalvelu.elsa.service.dto.KayttajaDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings

/**
 * Mapper for the entity [Kayttaja] and its DTO [KayttajaDTO].
 */
@Mapper(componentModel = "spring", uses = [UserMapper::class])
interface KayttajaMapper :
    EntityMapper<KayttajaDTO, Kayttaja> {

    @Mappings(
        Mapping(source = "user.id", target = "userId")
    )
    override fun toDto(kayttaja: Kayttaja): KayttajaDTO

    @Mappings(
        Mapping(source = "userId", target = "user"),
        Mapping(target = "keskustelus", ignore = true),
        Mapping(target = "removeKeskustelu", ignore = true)
    )
    override fun toEntity(kayttajaDTO: KayttajaDTO): Kayttaja

    @JvmDefault
    fun fromId(id: Long?) = id?.let {
        val kayttaja = Kayttaja()
        kayttaja.id = id
        kayttaja
    }
}
