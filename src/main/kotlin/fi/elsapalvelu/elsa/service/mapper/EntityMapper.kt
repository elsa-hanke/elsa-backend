package fi.elsapalvelu.elsa.service.mapper

import org.mapstruct.BeanMapping
import org.mapstruct.MappingTarget
import org.mapstruct.Named
import org.mapstruct.NullValuePropertyMappingStrategy

/**
 * Contract for a generic dto to entity mapper.
 *
 * @param D DTO type parameter.
 * @param E Entity type parameter.
 */

interface EntityMapper<D, E> {

    fun toEntity(dto: D): E

    fun toDto(entity: E): D

    fun toEntity(dtoList: MutableList<D>): MutableList<E>

    fun toDto(entityList: MutableList<E>): MutableList<D>

    @Named("partialUpdate")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    fun partialUpdate(@MappingTarget entity: E, dto: D)
}
