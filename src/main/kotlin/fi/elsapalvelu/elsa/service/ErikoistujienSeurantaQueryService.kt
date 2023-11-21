package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.domain.*
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
    ): Page<Opintooikeus> {
        val specification = createSpecification(criteria) { root, _, cb ->
            val user: Join<Kayttaja, User> = root.join(Opintooikeus_.erikoistuvaLaakari)
                .join(ErikoistuvaLaakari_.kayttaja)
                .join(Kayttaja_.user)

            val yliopisto: Path<Yliopisto> = root.get(Opintooikeus_.yliopisto)

            if (yliopistotAndErikoisalat.isNotEmpty()) {
                val orPredicates = yliopistotAndErikoisalat.map { ye ->
                    cb.and(
                        cb.equal(yliopisto.get(Yliopisto_.id), ye.yliopisto!!.id),
                        cb.equal(root.get(Opintooikeus_.erikoisala).get(Erikoisala_.id), ye.erikoisala!!.id)
                    )
                }.toTypedArray()
                cb.or(*orPredicates)
            } else {
                if (criteria?.nimi != null) {
                    val nimiPredicate = criteria.nimi.toNimiPredicate(user, cb, langkey)
                    nimiPredicate
                } else {
                    cb.isTrue(cb.literal(true))
                }
            }
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
            if (it.tila != null) {
                it.tila.let { tilaList ->
                    specification =
                        specification.and { root: Root<Opintooikeus?>, _: CriteriaQuery<*>, cb: CriteriaBuilder ->
                            cb.or(
                                *tilaList!!.map { tilaValue ->
                                    cb.equal(root.get(Opintooikeus_.tila), tilaValue)
                                }.toTypedArray()
                            )
                        }
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
