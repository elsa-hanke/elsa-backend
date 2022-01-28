package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.repository.PaivakirjamerkintaRepository
import fi.elsapalvelu.elsa.service.criteria.PaivakirjamerkintaCriteria
import fi.elsapalvelu.elsa.service.dto.PaivakirjamerkintaDTO
import fi.elsapalvelu.elsa.service.mapper.PaivakirjamerkintaMapper
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tech.jhipster.service.QueryService
import tech.jhipster.service.filter.Filter
import javax.persistence.criteria.JoinType
import javax.persistence.criteria.Path

@Service
@Transactional(readOnly = true)
class PaivakirjamerkintaQueryService(
    private val paivakirjamerkintaRepository: PaivakirjamerkintaRepository,
    private val paivakirjamerkintaMapper: PaivakirjamerkintaMapper
) : QueryService<Paivakirjamerkinta>() {

    @Transactional(readOnly = true)
    fun findByCriteriaAndOpintooikeusId(
        criteria: PaivakirjamerkintaCriteria?,
        page: Pageable,
        opintooikeusId: Long
    ): Page<PaivakirjamerkintaDTO> {
        val specification = createSpecification(criteria) { root, _, cb ->
            val opintooikeus: Path<Opintooikeus> = root.get(Paivakirjamerkinta_.opintooikeus)
            cb.equal(opintooikeus.get(Opintooikeus_.id), opintooikeusId)
        }

        return paivakirjamerkintaRepository.findAll(specification, page)
            .map(paivakirjamerkintaMapper::toDto)
    }

    @Suppress("ComplexMethod")
    protected fun createSpecification(
        criteria: PaivakirjamerkintaCriteria?,
        spec: Specification<Paivakirjamerkinta?>? = null
    ): Specification<Paivakirjamerkinta?> {
        var specification: Specification<Paivakirjamerkinta?> = Specification.where(spec)
        if (criteria != null) {
            if (criteria.id != null) {
                specification = specification.and(buildRangeSpecification(criteria.id, Paivakirjamerkinta_.id))
            }
            if (criteria.paivamaara != null) {
                specification =
                    specification.and(buildRangeSpecification(criteria.paivamaara, Paivakirjamerkinta_.paivamaara))
            }
            if (criteria.oppimistapahtumanNimi != null) {
                specification = specification.and(
                    buildStringSpecification(
                        criteria.oppimistapahtumanNimi,
                        Paivakirjamerkinta_.oppimistapahtumanNimi
                    )
                )
            }
            if (criteria.muunAiheenNimi != null) {
                specification = specification.and(
                    buildStringSpecification(
                        criteria.muunAiheenNimi,
                        Paivakirjamerkinta_.muunAiheenNimi
                    )
                )
            }
            if (criteria.yksityinen != null) {
                specification =
                    specification.and(buildSpecification(criteria.yksityinen, Paivakirjamerkinta_.yksityinen))
            }
            if (criteria.aihekategoriaId != null) {
                specification = specification.and(
                    buildSpecification(criteria.aihekategoriaId as Filter<Long>) {
                        it.join(Paivakirjamerkinta_.aihekategoriat, JoinType.LEFT).get(PaivakirjaAihekategoria_.id)
                    }
                )
            }
            if (criteria.teoriakoulutusId != null) {
                specification = specification.and(
                    buildSpecification(criteria.teoriakoulutusId as Filter<Long>) {
                        it.join(Paivakirjamerkinta_.teoriakoulutus, JoinType.LEFT).get(Teoriakoulutus_.id)
                    }
                )
            }
        }
        return specification
    }
}
