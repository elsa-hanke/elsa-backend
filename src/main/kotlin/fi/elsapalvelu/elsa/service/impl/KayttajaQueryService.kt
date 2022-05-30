package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.extensions.toNimiPredicate
import fi.elsapalvelu.elsa.repository.KayttajaRepository
import fi.elsapalvelu.elsa.service.criteria.KayttajahallintaCriteria
import fi.elsapalvelu.elsa.service.dto.YliopistoErikoisalaDTO
import fi.elsapalvelu.elsa.service.dto.kayttajahallinta.KayttajahallintaKayttajaListItemDTO
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tech.jhipster.service.QueryService
import tech.jhipster.service.filter.LongFilter
import tech.jhipster.service.filter.StringFilter
import javax.persistence.criteria.*

@Service
@Transactional(readOnly = true)
class KayttajaQueryService(
    private val kayttajaRepository: KayttajaRepository
) : QueryService<Any>() {

    @Transactional(readOnly = true)
    fun findByAuthorityAndCriteriaAndYliopistoId(
        criteria: KayttajahallintaCriteria?,
        authority: String,
        pageable: Pageable,
        yliopistoId: Long?,
        langkey: String?
    ): Page<KayttajahallintaKayttajaListItemDTO> {
        val specification: Specification<Kayttaja> = Specification.where { root, cq, cb ->
            val predicates: MutableList<Predicate> = mutableListOf()

            getAuthorityPredicate(authority, root, cb).let {
                predicates.add(it)
            }
            getYliopistoPredicate(yliopistoId, root, cq, cb)?.let {
                predicates.add(it)
            }
            getNimiPredicate(criteria?.nimi, root, cb, langkey)?.let {
                predicates.add(it)
            }
            getErikoisalaPredicate(criteria?.erikoisalaId, root, cq, cb)?.let {
                predicates.add(it)
            }

            cb.and(*predicates.toTypedArray())
        }
        return kayttajaRepository.findAll(specification, pageable).map { mapKayttaja(it) }
    }

    @Transactional(readOnly = true)
    fun findByAuthorityAndCriteria(
        criteria: KayttajahallintaCriteria?,
        authority: String,
        pageable: Pageable,
        langkey: String?
    ): Page<KayttajahallintaKayttajaListItemDTO> {
        val specification: Specification<Kayttaja> = Specification.where { root, cq, cb ->
            val predicates: MutableList<Predicate> = mutableListOf()

            getAuthorityPredicate(authority, root, cb).let {
                predicates.add(it)
            }
            getNimiPredicate(criteria?.nimi, root, cb, langkey)?.let {
                predicates.add(it)
            }
            getErikoisalaPredicate(criteria?.erikoisalaId, root, cq, cb)?.let {
                predicates.add(it)
            }

            cb.and(*predicates.toTypedArray())
        }
        return kayttajaRepository.findAll(specification, pageable).map { mapKayttaja(it) }
    }

    private fun getAuthorityPredicate(
        authority: String,
        root: Root<Kayttaja>,
        cb: CriteriaBuilder
    ): Predicate {
        val authorityJoin: Join<User, Authority> = root.join(Kayttaja_.user).join(User_.authorities)
        return cb.`in`(authorityJoin.get(Authority_.name)).value(authority)
    }

    private fun getNimiPredicate(
        nimiFilter: StringFilter?,
        root: Root<Kayttaja>,
        cb: CriteriaBuilder,
        langkey: String?
    ): Predicate? {
        val user: Join<Kayttaja, User> = root.join(Kayttaja_.user)
        return nimiFilter.toNimiPredicate(user, cb, langkey)
    }

    private fun getYliopistoPredicate(
        yliopistoId: Long?,
        root: Root<Kayttaja>,
        cq: CriteriaQuery<*>,
        cb: CriteriaBuilder
    ): Predicate? {
        return yliopistoId?.let {
            val subquery = cq.subquery(Long::class.java)
            val subRoot = subquery.from(KayttajaYliopistoErikoisala::class.java)
            val rootJoin = subRoot.join(KayttajaYliopistoErikoisala_.kayttaja)
            val yliopistoJoin = subRoot.join(KayttajaYliopistoErikoisala_.yliopisto)
            subquery.select(subRoot.get(KayttajaYliopistoErikoisala_.id))
            subquery.where(
                cb.equal(yliopistoJoin.get(Yliopisto_.id), yliopistoId),
                cb.equal(root.get(Kayttaja_.id), rootJoin.get(Kayttaja_.id))
            )
            return cb.exists(subquery)
        }
    }

    private fun getErikoisalaPredicate(
        erikoisalaId: LongFilter?,
        root: Root<Kayttaja>,
        cq: CriteriaQuery<*>,
        cb: CriteriaBuilder
    ): Predicate? {
        return erikoisalaId?.let {
            val subquery = cq.subquery(Long::class.java)
            val subRoot = subquery.from(KayttajaYliopistoErikoisala::class.java)
            val rootJoin = subRoot.join(KayttajaYliopistoErikoisala_.kayttaja)
            val erikoisalaJoin = subRoot.join(KayttajaYliopistoErikoisala_.erikoisala)
            subquery.select(subRoot.get(KayttajaYliopistoErikoisala_.id))
            subquery.where(
                cb.equal(erikoisalaJoin.get(Erikoisala_.id), erikoisalaId.equals),
                cb.equal(root.get(Kayttaja_.id), rootJoin.get(Kayttaja_.id))
            )
            return cb.exists(subquery)
        }
    }

    private fun mapKayttaja(kayttaja: Kayttaja) =
        KayttajahallintaKayttajaListItemDTO(
            kayttajaId = kayttaja.id,
            etunimi = kayttaja.user?.firstName,
            sukunimi = kayttaja.user?.lastName,
            yliopistotAndErikoisalat = kayttaja.yliopistotAndErikoisalat.map { ye ->
                YliopistoErikoisalaDTO(
                    yliopisto = ye.yliopisto?.nimi,
                    erikoisala = ye.erikoisala?.nimi
                )
            },
            kayttajatilinTila = kayttaja.tila
        )
}
