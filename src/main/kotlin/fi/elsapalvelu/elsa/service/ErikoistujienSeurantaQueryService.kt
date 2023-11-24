package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.domain.enumeration.OpintooikeudenTila
import fi.elsapalvelu.elsa.extensions.toNimiPredicate
import fi.elsapalvelu.elsa.repository.OpintooikeusRepository
import fi.elsapalvelu.elsa.service.criteria.ErikoistujanEteneminenCriteria
import jakarta.persistence.criteria.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class ErikoistujienSeurantaQueryService(
    private val opintooikeusRepository: OpintooikeusRepository
) {

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
            } else yliopistoPredicate
        }
        return opintooikeusRepository.findAll(specification, pageable)
    }

    @Transactional(readOnly = true)
    fun findByErikoisalaAndYliopisto(
        criteria: ErikoistujanEteneminenCriteria?,
        pageable: Pageable,
        langkey: String?,
        yliopistotAndErikoisalat: MutableSet<KayttajaYliopistoErikoisala> = mutableSetOf(),
        validStates: List<OpintooikeudenTila>,
        endedStates: List<OpintooikeudenTila>,
    ): Page<Opintooikeus> {
        val specification = createSpecification(criteria) { root, _, cb ->
            val user: Join<Kayttaja, User> = root.join(Opintooikeus_.erikoistuvaLaakari)
                .join(ErikoistuvaLaakari_.kayttaja)
                .join(Kayttaja_.user)

            val validStatesExpression = root.get(Opintooikeus_.tila)
            val endedStatesExpression = root.get(Opintooikeus_.tila)

            val myontamispaivaPredicate = cb.lessThanOrEqualTo(
                root.get(Opintooikeus_.opintooikeudenMyontamispaiva),
                LocalDate.now()
            )
            val tilaPredicate = cb.or(
                validStatesExpression.`in`(validStates),
                cb.and(
                    endedStatesExpression.`in`(endedStates),
                    cb.lessThanOrEqualTo(root.get(Opintooikeus_.viimeinenKatselupaiva), LocalDate.now())
                )
            )
            val orPredicates = yliopistotAndErikoisalat.map { ye ->
                cb.and(
                    cb.equal(root.get(Opintooikeus_.yliopisto).get(Yliopisto_.id), ye.yliopisto!!.id),
                    cb.equal(root.get(Opintooikeus_.erikoisala).get(Erikoisala_.id), ye.erikoisala!!.id)
                )
            }.toTypedArray()
            val combinedOrPredicate = cb.or(*orPredicates)

            if (criteria?.nimi != null) {
                val nimiPredicate = criteria.nimi.toNimiPredicate(user, cb, langkey)
                cb.and(myontamispaivaPredicate, combinedOrPredicate, tilaPredicate, nimiPredicate)
            } else {
                cb.and(myontamispaivaPredicate, combinedOrPredicate, tilaPredicate)
            }
        }
        return opintooikeusRepository.findAll(specification, pageable)
    }

    @Transactional(readOnly = true)
    fun findByKouluttajaValtuutus(
        criteria: ErikoistujanEteneminenCriteria?,
        pageable: Pageable,
        kayttajaId: Long,
        validStates: List<OpintooikeudenTila>,
        endedStates: List<OpintooikeudenTila>,
    ): Page<Opintooikeus> {
        val specification = createSpecification(criteria) { root, _, cb ->
            val annetutValtuutukset: Join<Opintooikeus?, Kouluttajavaltuutus> = root.join(Opintooikeus_.annetutValtuutukset)

            val myontamispaivaPredicate = cb.lessThanOrEqualTo(
                root.get(Opintooikeus_.opintooikeudenMyontamispaiva),
                LocalDate.now()
            )
            val tilaPredicate = cb.or(
                root.get(Opintooikeus_.tila).`in`(validStates),
                cb.and(
                    root.get(Opintooikeus_.tila).`in`(endedStates),
                    cb.lessThanOrEqualTo(root.get(Opintooikeus_.viimeinenKatselupaiva), LocalDate.now())
                )
            )
            val valtuutettuIdPredicate = cb.equal(annetutValtuutukset.get(Kouluttajavaltuutus_.valtuutettu)
                .get(Kayttaja_.id), kayttajaId)
            val alkamispaivaPredicate = cb.lessThanOrEqualTo(
                annetutValtuutukset.get(Kouluttajavaltuutus_.alkamispaiva),
                LocalDate.now()
            )
            val paattymispaivaPredicate = cb.greaterThanOrEqualTo(
                annetutValtuutukset.get(Kouluttajavaltuutus_.paattymispaiva),
                LocalDate.now()
            )
            val dateBetweenPredicate = cb.and(alkamispaivaPredicate, paattymispaivaPredicate)
            cb.and(myontamispaivaPredicate, tilaPredicate, valtuutettuIdPredicate, dateBetweenPredicate)
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
                specification =
                    specification.and { root: Root<Opintooikeus?>, _: CriteriaQuery<*>, cb: CriteriaBuilder ->
                        cb.equal(
                            root.join(Opintooikeus_.asetus, JoinType.INNER)
                                .get(Asetus_.id),
                            it.equals
                        )
                    }
            }
            it.erikoisalaId?.let {
                specification =
                    specification.and { root: Root<Opintooikeus?>, _: CriteriaQuery<*>, cb: CriteriaBuilder ->
                        cb.equal(
                            root.join(Opintooikeus_.erikoisala, JoinType.INNER)
                                .get(Erikoisala_.id),
                            it.equals
                        )
                    }
            }
            if (it.naytaPaattyneet == null || it.naytaPaattyneet == false) {
                specification =
                    specification.and { root: Root<Opintooikeus?>, _: CriteriaQuery<*>, cb: CriteriaBuilder ->
                        cb.greaterThanOrEqualTo(
                            root.get(Opintooikeus_.opintooikeudenPaattymispaiva),
                            LocalDate.now()
                        )
                    }
            }
            specification.and { root: Root<Opintooikeus?>, _: CriteriaQuery<*>, cb: CriteriaBuilder ->
                cb.or(
                    cb.isNull(root.get(Opintooikeus_.viimeinenKatselupaiva)),
                    cb.greaterThanOrEqualTo(
                        root.get(Opintooikeus_.viimeinenKatselupaiva),
                        LocalDate.now()
                    )
                )
            }
        }
        return specification
    }

}
