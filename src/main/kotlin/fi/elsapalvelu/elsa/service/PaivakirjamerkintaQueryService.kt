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
import javax.persistence.criteria.Join
import javax.persistence.criteria.JoinType

@Service
@Transactional(readOnly = true)
class PaivakirjamerkintaQueryService(
    private val paivakirjamerkintaRepository: PaivakirjamerkintaRepository,
    private val paivakirjamerkintaMapper: PaivakirjamerkintaMapper
) : QueryService<Paivakirjamerkinta>() {

    @Transactional(readOnly = true)
    fun findByCriteriaAndErikoistuvaLaakariKayttajaUserId(
        criteria: PaivakirjamerkintaCriteria?,
        page: Pageable,
        userId: String
    ): Page<PaivakirjamerkintaDTO> {
        val specification = createSpecification(criteria) { root, _, cb ->
            val user: Join<Kayttaja, User> = root.join(Paivakirjamerkinta_.erikoistuvaLaakari)
                .join(ErikoistuvaLaakari_.kayttaja)
                .join(Kayttaja_.user)
            cb.equal(user.get(User_.id), userId)
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
            if (criteria.erikoistuvaLaakariId != null) {
                specification = specification.and(
                    buildSpecification(criteria.erikoistuvaLaakariId as Filter<Long>) {
                        it.join(Paivakirjamerkinta_.erikoistuvaLaakari, JoinType.LEFT).get(ErikoistuvaLaakari_.id)
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
