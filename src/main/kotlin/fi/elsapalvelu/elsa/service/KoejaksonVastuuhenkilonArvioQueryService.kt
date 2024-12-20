package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.extensions.toNimiPredicate
import fi.elsapalvelu.elsa.repository.KoejaksonVastuuhenkilonArvioRepository
import fi.elsapalvelu.elsa.service.criteria.NimiErikoisalaAndAvoinCriteria
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import jakarta.persistence.criteria.*


@Service
@Transactional(readOnly = true)
class KoejaksonVastuuhenkilonArvioQueryService(
    private val koejaksonVastuuhenkilonArvioRepository: KoejaksonVastuuhenkilonArvioRepository
) {

    @Transactional(readOnly = true)
    fun findByCriteriaAndYliopistoId(
        criteria: NimiErikoisalaAndAvoinCriteria?,
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
                    cb.isNotNull(root.get(KoejaksonVastuuhenkilonArvio_.erikoistuvanKuittausaika))
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
        criteria: NimiErikoisalaAndAvoinCriteria?,
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
        return Specification<KoejaksonVastuuhenkilonArvio?> { root, _, cb ->
            val opintooikeus: Join<KoejaksonVastuuhenkilonArvio, Opintooikeus> =
                root.join("opintooikeus")
            val erikoisala: Join<Opintooikeus, Erikoisala> = opintooikeus.join("erikoisala")
            cb.equal(erikoisala.get<Long>("id"), erikoisalaId)
        }
    }
}
