package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.extensions.toNimiPredicate
import fi.elsapalvelu.elsa.repository.KoejaksonVastuuhenkilonArvioRepository
import fi.elsapalvelu.elsa.service.criteria.KoejaksoCriteria
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tech.jhipster.service.QueryService
import javax.persistence.criteria.*


@Service
@Transactional(readOnly = true)
class KoejaksonVastuuhenkilonArvioQueryService(
    private val koejaksonVastuuhenkilonArvioRepository: KoejaksonVastuuhenkilonArvioRepository
) : QueryService<KoejaksonVastuuhenkilonArvio>() {

    @Transactional(readOnly = true)
    fun findByCriteriaAndYliopistoId(
        criteria: KoejaksoCriteria?,
        pageable: Pageable,
        yliopistoId: Long,
        langkey: String?
    ): Page<KoejaksonVastuuhenkilonArvio> {
        val specification = createSpecification(criteria) { root, _, cb ->
            val opintooikeus: Join<KoejaksonVastuuhenkilonArvio?, Opintooikeus> =
                root.join(KoejaksonVastuuhenkilonArvio_.opintooikeus)
            val yliopisto: Path<Yliopisto> = opintooikeus.get(Opintooikeus_.yliopisto)
            var result = cb.equal(yliopisto.get(Yliopisto_.id), yliopistoId)

            if (criteria?.erikoistujanNimi != null) {
                val user: Join<Kayttaja, User> =
                    opintooikeus.join(Opintooikeus_.erikoistuvaLaakari)
                        .join(ErikoistuvaLaakari_.kayttaja)
                        .join(Kayttaja_.user)

                val nimiPredicate = criteria.erikoistujanNimi.toNimiPredicate(user, cb, langkey)
                result = cb.and(result, nimiPredicate)
            }

            if (criteria?.avoin != null) {
                val avoinExpr = cb.and(
                    cb.equal(root.get(KoejaksonVastuuhenkilonArvio_.virkailijaHyvaksynyt), false),
                    cb.isNull(root.get(KoejaksonVastuuhenkilonArvio_.korjausehdotus))
                )
                result = if (criteria.avoin == true) {
                    cb.and(result, avoinExpr)
                } else {
                    cb.and(result, cb.not(avoinExpr))
                }
            }

            result
        }
        return koejaksonVastuuhenkilonArvioRepository.findAll(specification, pageable)
    }

    protected fun createSpecification(
        criteria: KoejaksoCriteria?,
        spec: Specification<KoejaksonVastuuhenkilonArvio?>? = null
    ): Specification<KoejaksonVastuuhenkilonArvio?> {
        var specification: Specification<KoejaksonVastuuhenkilonArvio?> = Specification.where(spec)
        criteria?.let {
            it.erikoisalaId?.let { erikoisalaId ->
                specification = specification.and(
                    buildErikoisalaSpecification(erikoisalaId.equals)
                )
            }
        }
        return specification
    }

    fun buildErikoisalaSpecification(erikoisalaId: Long): Specification<KoejaksonVastuuhenkilonArvio?>? {
        return Specification<KoejaksonVastuuhenkilonArvio?> { root: Root<KoejaksonVastuuhenkilonArvio?>, _: CriteriaQuery<*>?, criteriaBuilder: CriteriaBuilder ->
            val opintooikeus: Join<KoejaksonVastuuhenkilonArvio, Opintooikeus> =
                root.join("opintooikeus")
            val erikoisala: Join<Opintooikeus, Erikoisala> = opintooikeus.join("erikoisala")
            criteriaBuilder.equal(erikoisala.get<Long>("id"), erikoisalaId)
        }
    }
}
