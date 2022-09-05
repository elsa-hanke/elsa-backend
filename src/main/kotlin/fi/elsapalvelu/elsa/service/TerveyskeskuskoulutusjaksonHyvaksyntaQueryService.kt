package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.extensions.toNimiPredicate
import fi.elsapalvelu.elsa.repository.TerveyskeskuskoulutusjaksonHyvaksyntaRepository
import fi.elsapalvelu.elsa.service.criteria.NimiErikoisalaAndAvoinCriteria
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tech.jhipster.service.QueryService
import javax.persistence.criteria.*


@Service
@Transactional(readOnly = true)
class TerveyskeskuskoulutusjaksonHyvaksyntaQueryService(
    private val terveyskeskuskoulutusjaksonHyvaksyntaRepository: TerveyskeskuskoulutusjaksonHyvaksyntaRepository
) : QueryService<TerveyskeskuskoulutusjaksonHyvaksynta>() {

    @Transactional(readOnly = true)
    fun findByCriteriaAndYliopistoId(
        criteria: NimiErikoisalaAndAvoinCriteria?,
        pageable: Pageable,
        yliopistoIds: List<Long>,
        isVirkailija: Boolean,
        langkey: String?
    ): Page<TerveyskeskuskoulutusjaksonHyvaksynta> {
        val specification = createSpecification(criteria) { root, _, cb ->
            val opintooikeus: Join<TerveyskeskuskoulutusjaksonHyvaksynta?, Opintooikeus> =
                root.join(TerveyskeskuskoulutusjaksonHyvaksynta_.opintooikeus)
            val yliopisto: Path<Yliopisto> = opintooikeus.get(Opintooikeus_.yliopisto)
            var result = yliopisto.get(Yliopisto_.id).`in`(yliopistoIds)

            if (criteria?.erikoistujanNimi != null) {
                val user: Join<Kayttaja, User> =
                    opintooikeus.join(Opintooikeus_.erikoistuvaLaakari)
                        .join(ErikoistuvaLaakari_.kayttaja)
                        .join(Kayttaja_.user)

                val nimiPredicate = criteria.erikoistujanNimi.toNimiPredicate(user, cb, langkey)
                result = cb.and(result, nimiPredicate)
            }

            if (criteria?.avoin != null) {
                var avoinExpr =
                    cb.isNull(root.get(TerveyskeskuskoulutusjaksonHyvaksynta_.korjausehdotus))
                if (isVirkailija) {
                    avoinExpr = cb.and(
                        cb.equal(
                            root.get(TerveyskeskuskoulutusjaksonHyvaksynta_.virkailijaHyvaksynyt),
                            false
                        ), avoinExpr
                    )
                } else {
                    avoinExpr = cb.and(
                        cb.equal(
                            root.get(TerveyskeskuskoulutusjaksonHyvaksynta_.virkailijaHyvaksynyt),
                            true
                        ),
                        avoinExpr
                    )
                    avoinExpr = cb.and(
                        cb.equal(
                            root.get(TerveyskeskuskoulutusjaksonHyvaksynta_.vastuuhenkiloHyvaksynyt),
                            false
                        ),
                        avoinExpr
                    )
                }
                result = if (criteria.avoin == true) {
                    cb.and(result, avoinExpr)
                } else {
                    cb.and(result, cb.not(avoinExpr))
                }
            }

            result
        }
        return terveyskeskuskoulutusjaksonHyvaksyntaRepository.findAll(specification, pageable)
    }

    protected fun createSpecification(
        criteria: NimiErikoisalaAndAvoinCriteria?,
        spec: Specification<TerveyskeskuskoulutusjaksonHyvaksynta?>? = null
    ): Specification<TerveyskeskuskoulutusjaksonHyvaksynta?> {
        var specification: Specification<TerveyskeskuskoulutusjaksonHyvaksynta?> =
            Specification.where(spec)
        criteria?.let {
            it.erikoisalaId?.let { erikoisalaId ->
                specification = specification.and(
                    buildErikoisalaSpecification(erikoisalaId.equals)
                )
            }
        }
        return specification
    }

    fun buildErikoisalaSpecification(erikoisalaId: Long): Specification<TerveyskeskuskoulutusjaksonHyvaksynta?>? {
        return Specification<TerveyskeskuskoulutusjaksonHyvaksynta?> { root: Root<TerveyskeskuskoulutusjaksonHyvaksynta?>, _: CriteriaQuery<*>?, criteriaBuilder: CriteriaBuilder ->
            val opintooikeus: Join<TerveyskeskuskoulutusjaksonHyvaksynta, Opintooikeus> =
                root.join("opintooikeus")
            val erikoisala: Join<Opintooikeus, Erikoisala> = opintooikeus.join("erikoisala")
            criteriaBuilder.equal(erikoisala.get<Long>("id"), erikoisalaId)
        }
    }
}
