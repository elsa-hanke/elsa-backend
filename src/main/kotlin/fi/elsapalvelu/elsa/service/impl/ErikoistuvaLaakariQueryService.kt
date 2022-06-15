package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.extensions.toNimiPredicate
import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.service.criteria.KayttajahallintaCriteria
import fi.elsapalvelu.elsa.service.dto.KayttajahallintaYliopistoErikoisalaDTO
import fi.elsapalvelu.elsa.service.dto.kayttajahallinta.KayttajahallintaKayttajaListItemDTO
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tech.jhipster.service.QueryService
import tech.jhipster.service.filter.BooleanFilter
import tech.jhipster.service.filter.LongFilter
import tech.jhipster.service.filter.StringFilter
import javax.persistence.criteria.*

@Service
@Transactional(readOnly = true)
class ErikoistuvaLaakariQueryService(
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository
) : QueryService<Any>() {

    @Transactional(readOnly = true)
    fun findErikoistuvatByCriteriaAndYliopistoId(
        criteria: KayttajahallintaCriteria?,
        pageable: Pageable,
        yliopistoId: Long?,
        langkey: String?
    ): Page<KayttajahallintaKayttajaListItemDTO> {
        val specification: Specification<ErikoistuvaLaakari> = Specification.where { root, cq, cb ->
            val predicates: MutableList<Predicate> = mutableListOf()

            getNimiPredicate(criteria?.nimi, root, cb, langkey)?.let {
                predicates.add(it)
            }
            getErikoisalaPredicate(criteria?.erikoisalaId, root, cq, cb)?.let {
                predicates.add(it)
            }
            getYliopistoPredicate(yliopistoId, root, cq, cb)?.let {
                predicates.add(it)
            }
            getUseaOpintooikeusPredicate(criteria?.useaOpintooikeus, root, cq, cb)?.let {
                predicates.add(it)
            }

            cb.and(*predicates.toTypedArray())
        }
        return erikoistuvaLaakariRepository.findAll(specification, pageable).map { mapErikoistuvaLaakari(it) }
    }

    @Transactional(readOnly = true)
    fun findErikoistuvatByCriteria(
        criteria: KayttajahallintaCriteria?,
        pageable: Pageable,
        langkey: String?
    ): Page<KayttajahallintaKayttajaListItemDTO> {
        val specification: Specification<ErikoistuvaLaakari> = Specification.where { root, cq, cb ->
            val predicates: MutableList<Predicate> = mutableListOf()

            getNimiPredicate(criteria?.nimi, root, cb, langkey)?.let {
                predicates.add(it)
            }
            getErikoisalaPredicate(criteria?.erikoisalaId, root, cq, cb)?.let {
                predicates.add(it)
            }
            getUseaOpintooikeusPredicate(criteria?.useaOpintooikeus, root, cq, cb)?.let {
                predicates.add(it)
            }

            cb.and(*predicates.toTypedArray())
        }
        return erikoistuvaLaakariRepository.findAll(specification, pageable).map { mapErikoistuvaLaakari(it) }
    }

    private fun mapErikoistuvaLaakari(erikoistuvaLaakari: ErikoistuvaLaakari) =
        KayttajahallintaKayttajaListItemDTO(
            kayttajaId = erikoistuvaLaakari.kayttaja?.id,
            etunimi = erikoistuvaLaakari.kayttaja?.user?.firstName,
            sukunimi = erikoistuvaLaakari.kayttaja?.user?.lastName,
            syntymaaika = erikoistuvaLaakari.syntymaaika,
            yliopistotAndErikoisalat = erikoistuvaLaakari.opintooikeudet.map { o ->
                KayttajahallintaYliopistoErikoisalaDTO(
                    yliopisto = o.yliopisto?.nimi,
                    erikoisala = o.erikoisala?.nimi
                )
            },
            kayttajatilinTila = erikoistuvaLaakari.kayttaja?.tila
        )

    private fun getNimiPredicate(
        nimiFilter: StringFilter?,
        root: Root<ErikoistuvaLaakari?>,
        cb: CriteriaBuilder,
        langkey: String?
    ): Predicate? {
        val user: Join<Kayttaja, User> = root.join(ErikoistuvaLaakari_.kayttaja)
            .join(Kayttaja_.user)
        return nimiFilter.toNimiPredicate(user, cb, langkey)
    }

    private fun getYliopistoPredicate(
        yliopistoId: Long?,
        root: Root<ErikoistuvaLaakari?>,
        cq: CriteriaQuery<*>,
        cb: CriteriaBuilder
    ): Predicate? {
        return yliopistoId?.let {
            val subquery = cq.subquery(Long::class.java)
            val subRoot = subquery.from(Opintooikeus::class.java)
            val rootJoin = subRoot.join(Opintooikeus_.erikoistuvaLaakari)
            val yliopistoJoin = subRoot.join(Opintooikeus_.yliopisto)
            subquery.select(subRoot.get(Opintooikeus_.id))
            subquery.where(
                cb.equal(yliopistoJoin.get(Yliopisto_.id), yliopistoId),
                cb.equal(root.get(ErikoistuvaLaakari_.id), rootJoin.get(ErikoistuvaLaakari_.id))
            )
            return cb.exists(subquery)
        }
    }

    private fun getErikoisalaPredicate(
        erikoisalaId: LongFilter?,
        root: Root<ErikoistuvaLaakari?>,
        cq: CriteriaQuery<*>,
        cb: CriteriaBuilder
    ): Predicate? {
        return erikoisalaId?.let {
            val subquery = cq.subquery(Long::class.java)
            val subRoot = subquery.from(Opintooikeus::class.java)
            val rootJoin = subRoot.join(Opintooikeus_.erikoistuvaLaakari)
            val erikoisalaJoin = subRoot.join(Opintooikeus_.erikoisala)
            subquery.select(subRoot.get(Opintooikeus_.id))
            subquery.where(
                cb.equal(erikoisalaJoin.get(Erikoisala_.id), erikoisalaId.equals),
                cb.equal(root.get(ErikoistuvaLaakari_.id), rootJoin.get(ErikoistuvaLaakari_.id))
            )
            return cb.exists(subquery)
        }
    }

    private fun getUseaOpintooikeusPredicate(
        useaOpintooikeus: BooleanFilter?,
        root: Root<ErikoistuvaLaakari>,
        cq: CriteriaQuery<*>,
        cb: CriteriaBuilder
    ): Predicate? {
        return if (useaOpintooikeus?.equals == true) {
            val subquery = cq.subquery(Long::class.java)
            val subRoot = subquery.from(Opintooikeus::class.java)
            val rootJoin = subRoot.join(Opintooikeus_.erikoistuvaLaakari)
            subquery.select(cb.count(subRoot.get(Opintooikeus_.id)))
            subquery.where(cb.equal(root.get(ErikoistuvaLaakari_.id), rootJoin.get(ErikoistuvaLaakari_.id)))
            return cb.greaterThan(subquery, 1L)
        } else {
            null
        }
    }
}

