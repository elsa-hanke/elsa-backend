package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.domain.SoteOrganisaatio
import fi.elsapalvelu.elsa.domain.SoteOrganisaatio_
import fi.elsapalvelu.elsa.repository.SoteOrganisaatioRepository
import fi.elsapalvelu.elsa.service.dto.SoteOrganisaatioCriteria
import fi.elsapalvelu.elsa.service.dto.SoteOrganisaatioDTO
import fi.elsapalvelu.elsa.service.mapper.SoteOrganisaatioMapper
import io.github.jhipster.service.QueryService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
@Service
@Transactional(readOnly = true)
class SoteOrganisaatioQueryService(
    private val soteOrganisaatioRepository: SoteOrganisaatioRepository,
    private val soteOrganisaatioMapper: SoteOrganisaatioMapper
) : QueryService<SoteOrganisaatio>() {

    @Transactional(readOnly = true)
    fun findByCriteria(criteria: SoteOrganisaatioCriteria?): MutableList<SoteOrganisaatioDTO> {
        val specification = createSpecification(criteria)
        return soteOrganisaatioMapper.toDto(soteOrganisaatioRepository.findAll(specification))
    }

    @Transactional(readOnly = true)
    fun findByCriteria(criteria: SoteOrganisaatioCriteria?, page: Pageable): Page<SoteOrganisaatioDTO> {
        val specification = createSpecification(criteria)
        return soteOrganisaatioRepository.findAll(specification, page)
            .map(soteOrganisaatioMapper::toDto)
    }

    @Transactional(readOnly = true)
    fun countByCriteria(criteria: SoteOrganisaatioCriteria?): Long {
        val specification = createSpecification(criteria)
        return soteOrganisaatioRepository.count(specification)
    }

    @Transactional(readOnly = true)
    fun findAllKunnat(): MutableList<String> {
        return soteOrganisaatioRepository.findAll().mapNotNull { it.postOffice }.distinct().toMutableList()
    }

    protected fun createSpecification(criteria: SoteOrganisaatioCriteria?): Specification<SoteOrganisaatio?> {
        var specification: Specification<SoteOrganisaatio?> = Specification.where(null)
        if (criteria != null) {
            if (criteria.organizationId != null) {
                specification = specification
                    .and(buildStringSpecification(criteria.organizationId, SoteOrganisaatio_.organizationId))
            }
            if (criteria.abbreviation != null) {
                specification = specification
                    .and(buildStringSpecification(criteria.abbreviation, SoteOrganisaatio_.abbreviation))
            }
            if (criteria.longName != null) {
                specification = specification
                    .and(buildStringSpecification(criteria.longName, SoteOrganisaatio_.longName))
            }
            if (criteria.parentId != null) {
                specification = specification
                    .and(buildStringSpecification(criteria.parentId, SoteOrganisaatio_.parentId))
            }
            if (criteria.hierarchyLevel != null) {
                specification = specification
                    .and(buildRangeSpecification(criteria.hierarchyLevel, SoteOrganisaatio_.hierarchyLevel))
            }
            if (criteria.beginningDate != null) {
                specification = specification
                    .and(buildRangeSpecification(criteria.beginningDate, SoteOrganisaatio_.beginningDate))
            }
            if (criteria.expiringDate != null) {
                specification = specification
                    .and(buildRangeSpecification(criteria.expiringDate, SoteOrganisaatio_.expiringDate))
            }
            if (criteria.lastModifiedDate != null) {
                specification = specification
                    .and(buildRangeSpecification(criteria.lastModifiedDate, SoteOrganisaatio_.lastModifiedDate))
            }
            if (criteria.description != null) {
                specification = specification
                    .and(buildStringSpecification(criteria.description, SoteOrganisaatio_.description))
            }
            if (criteria.oid != null) {
                specification = specification
                    .and(buildStringSpecification(criteria.oid, SoteOrganisaatio_.oid))
            }
            if (criteria.costCenter != null) {
                specification = specification
                    .and(buildStringSpecification(criteria.costCenter, SoteOrganisaatio_.costCenter))
            }
            if (criteria.postAddress != null) {
                specification = specification
                    .and(buildStringSpecification(criteria.postAddress, SoteOrganisaatio_.postAddress))
            }
            if (criteria.streetAddress != null) {
                specification = specification
                    .and(buildStringSpecification(criteria.streetAddress, SoteOrganisaatio_.streetAddress))
            }
            if (criteria.postNumber != null) {
                specification = specification
                    .and(buildStringSpecification(criteria.postNumber, SoteOrganisaatio_.postNumber))
            }
            if (criteria.postOffice != null) {
                specification = specification
                    .and(buildStringSpecification(criteria.postOffice, SoteOrganisaatio_.postOffice))
            }
            if (criteria.phoneNumber != null) {
                specification = specification
                    .and(buildStringSpecification(criteria.phoneNumber, SoteOrganisaatio_.phoneNumber))
            }
            if (criteria.faxNumber != null) {
                specification = specification
                    .and(buildStringSpecification(criteria.faxNumber, SoteOrganisaatio_.faxNumber))
            }
            if (criteria.createdDate != null) {
                specification = specification
                    .and(buildRangeSpecification(criteria.createdDate, SoteOrganisaatio_.createdDate))
            }
        }
        return specification
    }
}
