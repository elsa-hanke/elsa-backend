package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.config.YEK_ERIKOISALA_ID
import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.domain.enumeration.VastuuhenkilonTehtavatyyppiEnum
import fi.elsapalvelu.elsa.extensions.toNimiPredicate
import fi.elsapalvelu.elsa.repository.OpintooikeusRepository
import fi.elsapalvelu.elsa.service.criteria.ErikoistujanEteneminenCriteria
import jakarta.persistence.criteria.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
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
        langkey: String?,
        excludedErikoisalaId: Long
    ): Page<Opintooikeus> {
        val specification = createSpecification(criteria) { root, _, cb ->
            val user: Join<Kayttaja, User> = root.join(Opintooikeus_.erikoistuvaLaakari)
                .join(ErikoistuvaLaakari_.kayttaja)
                .join(Kayttaja_.user)
            val yliopisto: Path<Yliopisto> = root.get(Opintooikeus_.yliopisto)
            val yliopistoPredicate = cb.and(cb.equal(yliopisto.get(Yliopisto_.id), yliopistoId))
            val excludedErikoisalaPredicate =
                cb.notEqual(root.get(Opintooikeus_.erikoisala).get(Erikoisala_.id), excludedErikoisalaId)

            if (criteria?.nimi != null) {
                val nimiPredicate = criteria.nimi.toNimiPredicate(user, cb, langkey)
                cb.and(yliopistoPredicate, excludedErikoisalaPredicate, nimiPredicate)
            } else cb.and(yliopistoPredicate, excludedErikoisalaPredicate)

        }
        val existingSort = pageable.sort
        val sortById = Sort.by(Sort.Order.asc(Opintooikeus_.ID))
        val finalSort = existingSort.and(sortById)
        val pageableWithSort = PageRequest.of(pageable.pageNumber, pageable.pageSize, finalSort)
        return opintooikeusRepository.findAll(specification, pageableWithSort)
    }

    @Transactional(readOnly = true)
    fun findByCriteriaAndYliopistoIdAndErikoisalaId(
        criteria: ErikoistujanEteneminenCriteria?,
        pageable: Pageable,
        yliopistoId: Long,
        erikoisalaId: Long,
        langkey: String?
    ): Page<Opintooikeus> {
        val specification = createSpecification(criteria) { root, _, cb ->
            val user: Join<Kayttaja, User> = root.join(Opintooikeus_.erikoistuvaLaakari)
                .join(ErikoistuvaLaakari_.kayttaja)
                .join(Kayttaja_.user)
            val yliopisto: Path<Yliopisto> = root.get(Opintooikeus_.yliopisto)
            val yliopistoPredicate = cb.equal(yliopisto.get(Yliopisto_.id), yliopistoId)
            val erikoisalaPredicate = cb.equal(root.get(Opintooikeus_.erikoisala).get(Erikoisala_.id), erikoisalaId)
            if (criteria?.nimi != null) {
                val nimiPredicate = criteria.nimi.toNimiPredicate(user, cb, langkey)
                cb.and(yliopistoPredicate, nimiPredicate, erikoisalaPredicate)
            } else {
                cb.and(yliopistoPredicate, erikoisalaPredicate)
            }
        }
        val existingSort = pageable.sort
        val sortById = Sort.by(Sort.Order.asc(Opintooikeus_.ID))
        val finalSort = existingSort.and(sortById)
        val pageableWithSort = PageRequest.of(pageable.pageNumber, pageable.pageSize, finalSort)
        return opintooikeusRepository.findAll(specification, pageableWithSort)
    }

    @Transactional(readOnly = true)
    fun findByErikoisalaAndYliopisto(
        criteria: ErikoistujanEteneminenCriteria?,
        pageable: Pageable,
        langkey: String?,
        yliopistotAndErikoisalat: MutableSet<KayttajaYliopistoErikoisala> = mutableSetOf()
    ): Page<Opintooikeus> {
        val specification = createSpecification(criteria) { root, _, cb ->
            val user: Join<Kayttaja, User> = root.join(Opintooikeus_.erikoistuvaLaakari)
                .join(ErikoistuvaLaakari_.kayttaja)
                .join(Kayttaja_.user)

            val myontamispaivaPredicate = cb.lessThanOrEqualTo(
                root.get(Opintooikeus_.opintooikeudenMyontamispaiva),
                LocalDate.now()
            )
            val orPredicates = yliopistotAndErikoisalat.map { ye ->
                if (criteria?.erikoisalaId?.equals == YEK_ERIKOISALA_ID && ye.vastuuhenkilonTehtavat.any {
                    it.nimi == VastuuhenkilonTehtavatyyppiEnum.YEK_VALMISTUMINEN || it.nimi == VastuuhenkilonTehtavatyyppiEnum.YEK_TERVEYSKESKUSKOULUTUSJAKSO
                }) {
                    cb.and(
                        cb.equal(root.get(Opintooikeus_.yliopisto).get(Yliopisto_.id), ye.yliopisto!!.id),
                        cb.equal(root.get(Opintooikeus_.erikoisala).get(Erikoisala_.id), YEK_ERIKOISALA_ID)
                    )
                } else {
                    cb.and(
                        cb.equal(root.get(Opintooikeus_.yliopisto).get(Yliopisto_.id), ye.yliopisto!!.id),
                        cb.equal(root.get(Opintooikeus_.erikoisala).get(Erikoisala_.id), ye.erikoisala!!.id)
                    )
                }
            }.toTypedArray()
            val combinedOrPredicate = cb.or(*orPredicates)

            if (criteria?.nimi != null) {
                val nimiPredicate = criteria.nimi.toNimiPredicate(user, cb, langkey)
                cb.and(myontamispaivaPredicate, combinedOrPredicate, nimiPredicate)
            } else {
                cb.and(myontamispaivaPredicate, combinedOrPredicate)
            }
        }
        val existingSort = pageable.sort
        val sortById = Sort.by(Sort.Order.asc(Opintooikeus_.ID))
        val finalSort = existingSort.and(sortById)
        val pageableWithSort = PageRequest.of(pageable.pageNumber, pageable.pageSize, finalSort)
        return opintooikeusRepository.findAll(specification, pageableWithSort)
    }

    @Transactional(readOnly = true)
    fun findByKouluttajaValtuutus(
        criteria: ErikoistujanEteneminenCriteria?,
        pageable: Pageable,
        kayttajaId: Long
    ): Page<Opintooikeus> {
        val specification = createSpecification(criteria) { root, _, cb ->
            val annetutValtuutukset: Join<Opintooikeus?, Kouluttajavaltuutus> =
                root.join(Opintooikeus_.annetutValtuutukset)

            val myontamispaivaPredicate = cb.lessThanOrEqualTo(
                root.get(Opintooikeus_.opintooikeudenMyontamispaiva),
                LocalDate.now()
            )
            val valtuutettuIdPredicate = cb.equal(
                annetutValtuutukset.get(Kouluttajavaltuutus_.valtuutettu)
                    .get(Kayttaja_.id), kayttajaId
            )
            val alkamispaivaPredicate = cb.lessThanOrEqualTo(
                annetutValtuutukset.get(Kouluttajavaltuutus_.alkamispaiva),
                LocalDate.now()
            )
            val paattymispaivaPredicate = cb.greaterThanOrEqualTo(
                annetutValtuutukset.get(Kouluttajavaltuutus_.paattymispaiva),
                LocalDate.now()
            )
            val dateBetweenPredicate = cb.and(alkamispaivaPredicate, paattymispaivaPredicate)
            cb.and(myontamispaivaPredicate, valtuutettuIdPredicate, dateBetweenPredicate)
        }
        val existingSort = pageable.sort
        val sortById = Sort.by(Sort.Order.asc(Opintooikeus_.ID))
        val finalSort = existingSort.and(sortById)
        val pageableWithSort = PageRequest.of(pageable.pageNumber, pageable.pageSize, finalSort)
        return opintooikeusRepository.findAll(specification, pageableWithSort)
    }

    protected fun createSpecification(
        criteria: ErikoistujanEteneminenCriteria?,
        spec: Specification<Opintooikeus?>? = null
    ): Specification<Opintooikeus?> {
        var specification: Specification<Opintooikeus?> = Specification.where(spec)
        criteria?.let {
            it.asetusId?.let {
                specification =
                    specification.and { root, _, cb ->
                        cb.equal(
                            root.join(Opintooikeus_.asetus, JoinType.INNER)
                                .get(Asetus_.id),
                            it.equals
                        )
                    }
            }
            it.erikoisalaId?.let {
                specification =
                    specification.and { root, _, cb ->
                        cb.equal(
                            root.join(Opintooikeus_.erikoisala, JoinType.INNER)
                                .get(Erikoisala_.id),
                            it.equals
                        )
                    }
            }
            if (it.naytaPaattyneet == null || it.naytaPaattyneet == false) {
                specification =
                    specification.and { root, _, cb ->
                        cb.greaterThanOrEqualTo(
                            root.get(Opintooikeus_.opintooikeudenPaattymispaiva),
                            LocalDate.now()
                        )
                    }
            }
            specification = specification.and { root, _, cb ->
                cb.greaterThanOrEqualTo(
                    root.get(Opintooikeus_.viimeinenKatselupaiva),
                    LocalDate.now()
                )
            }
        }
        return specification
    }

}
