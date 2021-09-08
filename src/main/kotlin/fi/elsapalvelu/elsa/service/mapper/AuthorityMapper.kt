package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.Authority
import org.mapstruct.Mapper

@Mapper(
    componentModel = "spring",
)
interface AuthorityMapper :
    EntityMapper<String, Authority> {

    override fun toEntity(dto: String): Authority {
        return Authority(dto)
    }

    override fun toDto(entity: Authority): String {
        return entity.name!!
    }

    override fun toEntity(dtoList: MutableList<String>): MutableList<Authority> {
        return dtoList.map { s -> Authority(s) }.toMutableList()
    }

    override fun toDto(entityList: MutableList<Authority>): MutableList<String> {
        return entityList.map { authority -> authority.name!! }.toMutableList()
    }
}

