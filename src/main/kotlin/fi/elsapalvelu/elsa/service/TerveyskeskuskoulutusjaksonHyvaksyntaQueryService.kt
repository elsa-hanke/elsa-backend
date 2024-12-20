package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.config.YEK_ERIKOISALA_ID
import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.extensions.toNimiPredicate
import fi.elsapalvelu.elsa.repository.TerveyskeskuskoulutusjaksonHyvaksyntaRepository
import fi.elsapalvelu.elsa.service.criteria.NimiErikoisalaAndAvoinCriteria
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import jakarta.persistence.criteria.*


@Service
@Transactional(readOnly = true)
class TerveyskeskuskoulutusjaksonHyvaksyntaQueryService(
    private val terveyskeskuskoulutusjaksonHyvaksyntaRepository: TerveyskeskuskoulutusjaksonHyvaksyntaRepository
) {

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

            if (!isVirkailija) {
                result = cb.and(
                    result,
                    cb.or(
                        cb.isNotNull(root.get(TerveyskeskuskoulutusjaksonHyvaksynta_.vastuuhenkilonKorjausehdotus)),
                        cb.equal(
                            root.get(TerveyskeskuskoulutusjaksonHyvaksynta_.virkailijaHyvaksynyt),
                            true
                        )
                    )
                )
            }

            if (criteria?.avoin != null) {
                val avoinExpr: Predicate
                if (isVirkailija) {
                    avoinExpr = cb.and(
                        cb.equal(
                            root.get(TerveyskeskuskoulutusjaksonHyvaksynta_.virkailijaHyvaksynyt),
                            false
                        ), cb.equal(
                            root.get(TerveyskeskuskoulutusjaksonHyvaksynta_.erikoistujaLahettanyt),
                            true
                        )
                    )
                } else {
                    avoinExpr = cb.and(
                        cb.equal(
                            root.get(TerveyskeskuskoulutusjaksonHyvaksynta_.virkailijaHyvaksynyt),
                            true
                        ),
                        cb.equal(
                            root.get(TerveyskeskuskoulutusjaksonHyvaksynta_.vastuuhenkiloHyvaksynyt),
                            false
                        )
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
        if (criteria?.erikoisalaId == null) {
            specification = specification.and(
                buildExcludedErikoisalaSpecification(YEK_ERIKOISALA_ID)
            )
        }
        return specification
    }

    fun buildErikoisalaSpecification(erikoisalaId: Long): Specification<TerveyskeskuskoulutusjaksonHyvaksynta?>? {
        return Specification<TerveyskeskuskoulutusjaksonHyvaksynta?> { root, _, cb ->
            val opintooikeus: Join<TerveyskeskuskoulutusjaksonHyvaksynta, Opintooikeus> =
                root.join("opintooikeus")
            val erikoisala: Join<Opintooikeus, Erikoisala> = opintooikeus.join("erikoisala")
            cb.equal(erikoisala.get<Long>("id"), erikoisalaId)
        }
    }

    fun buildExcludedErikoisalaSpecification(excludedErikoisalaId: Long): Specification<TerveyskeskuskoulutusjaksonHyvaksynta?>? {
        return Specification<TerveyskeskuskoulutusjaksonHyvaksynta?> { root, _, cb ->
            val opintooikeus: Join<TerveyskeskuskoulutusjaksonHyvaksynta, Opintooikeus> =
                root.join("opintooikeus")
            val erikoisala: Join<Opintooikeus, Erikoisala> = opintooikeus.join("erikoisala")
            cb.not(cb.equal(erikoisala.get<Long>("id"), excludedErikoisalaId))
        }
    }
}
