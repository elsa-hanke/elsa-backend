package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.extensions.toNimiPredicate
import fi.elsapalvelu.elsa.repository.OpintooikeusRepository
import fi.elsapalvelu.elsa.service.criteria.ErikoistujanEteneminenCriteria
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tech.jhipster.service.QueryService
import javax.persistence.criteria.Join
import javax.persistence.criteria.Path

@Service
@Transactional(readOnly = true)
class ErikoistujienSeurantaQueryService(
    private val opintooikeusRepository: OpintooikeusRepository
) : QueryService<Opintooikeus>() {

    @Transactional(readOnly = true)
    fun findByCriteriaAndYliopistoId(
        criteria: ErikoistujanEteneminenCriteria?,
        pageable: Pageable,
        yliopistoId: Long,
        langkey: String?
    ): Page<Opintooikeus> {
        val specification = createSpecification(criteria) { root, _, cb ->
            val user: Join<Kayttaja, User> = root.join(Opintooikeus_.erikoistuvaLaakari)
                .join(ErikoistuvaLaakari_.kayttaja)
                .join(Kayttaja_.user)
            val yliopisto: Path<Yliopisto> = root.get(Opintooikeus_.yliopisto)
            val yliopistoPredicate = cb.and(cb.equal(yliopisto.get(Yliopisto_.id), yliopistoId))

            if (criteria?.nimi != null) {
                val nimiPredicate = criteria.nimi.toNimiPredicate(user, cb, langkey)
                cb.and(yliopistoPredicate, nimiPredicate)
            }
            else yliopistoPredicate
        }
        return opintooikeusRepository.findAll(specification, pageable)
    }

    protected fun createSpecification(
        criteria: ErikoistujanEteneminenCriteria?,
        spec: Specification<Opintooikeus?>? = null
    ): Specification<Opintooikeus?> {
        var specification: Specification<Opintooikeus?> = Specification.where(spec)
        criteria?.let {
            it.asetusId?.let {
                specification = specification.and(
                    buildReferringEntitySpecification(
                        criteria.asetusId,
                        Opintooikeus_.asetus,
                        Asetus_.id
                    )
                )
            }
            it.erikoisalaId?.let {
                specification = specification.and(
                    buildReferringEntitySpecification(
                        criteria.erikoisalaId,
                        Opintooikeus_.erikoisala,
                        Erikoisala_.id
                    )
                )
            }
        }
        return specification
    }
}
