package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.extensions.toNimiPredicate
import fi.elsapalvelu.elsa.repository.KayttajaRepository
import fi.elsapalvelu.elsa.security.OPINTOHALLINNON_VIRKAILIJA
import fi.elsapalvelu.elsa.service.criteria.KayttajahallintaCriteria
import fi.elsapalvelu.elsa.service.dto.KayttajahallintaYliopistoErikoisalaDTO
import fi.elsapalvelu.elsa.service.dto.kayttajahallinta.KayttajahallintaErikoistujaJaKouluttajaListItemDTO
import fi.elsapalvelu.elsa.service.dto.kayttajahallinta.KayttajahallintaKayttajaListItemDTO
import fi.elsapalvelu.elsa.service.mapper.VastuuhenkilonTehtavatyyppiMapper
import jakarta.persistence.criteria.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tech.jhipster.service.filter.LongFilter
import tech.jhipster.service.filter.StringFilter

@Service
@Transactional(readOnly = true)
class KayttajaQueryService(
    private val kayttajaRepository: KayttajaRepository,
    private val vastuuhenkilonTehtavatyyppiMapper: VastuuhenkilonTehtavatyyppiMapper,
) {

    @Transactional(readOnly = true)
    fun findByAuthorityAndCriteriaAndYliopistoId(
        criteria: KayttajahallintaCriteria?,
        authority: String,
        pageable: Pageable,
        yliopistoId: Long,
        langkey: String?
    ): Page<KayttajahallintaKayttajaListItemDTO> {
        val specification = Specification
            .where(hasYliopisto(authority, yliopistoId))
            .and(hasAuthority(authority))
            .and(hasName(criteria?.nimi, langkey))
            .and(hasErikoisala(criteria?.erikoisalaId))
        return kayttajaRepository.findAll(specification, pageable).map { mapKayttaja(it) }
    }

    @Transactional(readOnly = true)
    fun findByAuthorityAndCriteria(
        criteria: KayttajahallintaCriteria?,
        authority: String,
        pageable: Pageable,
        langkey: String?
    ): Page<KayttajahallintaKayttajaListItemDTO> {
        val specification = Specification
            .where(hasAuthority(authority))
            .and(hasName(criteria?.nimi, langkey))
            .and(hasErikoisala(criteria?.erikoisalaId))
        return kayttajaRepository.findAll(specification, pageable).map { mapKayttaja(it) }
    }

    @Transactional(readOnly = true)
    fun findByCriteriaAndAuthorities(
        activeAuthority: String?,
        criteria: KayttajahallintaCriteria?,
        pageable: Pageable,
        langkey: String?,
        authorities: List<String>,
        yliopistot: List<Long?>,
        nullAuthority: Boolean?
    ): Page<KayttajahallintaErikoistujaJaKouluttajaListItemDTO> {

        val specification = if (nullAuthority == true) {
            Specification
                .where(hasCertainOrNoAuthorities(authorities))
                .and(hasName(criteria?.nimi, langkey))
                .and(hasErikoisala(criteria?.erikoisalaId))
            } else {
                Specification
                    .where(hasAuthorities(authorities))
                    .and(hasName(criteria?.nimi, langkey))
                    .and(hasErikoisala(criteria?.erikoisalaId))
            }

        if (activeAuthority != null && activeAuthority == Authority(OPINTOHALLINNON_VIRKAILIJA).name && yliopistot.isNotEmpty()) {
            specification.and(hasOpintooikeusYliopisto(yliopistot[0]))
        }

        return kayttajaRepository.findAll(specification, pageable).map { mapKayttajaErikoistujaKouluttaja(it) }
    }

    private fun hasYliopisto(authority: String, yliopistoId: Long): Specification<Kayttaja> {
        return (Specification<Kayttaja> { root, query, cb ->
            if (authority == OPINTOHALLINNON_VIRKAILIJA) {
                val rootJoin = root.join(Kayttaja_.yliopistot)
                cb.`in`(rootJoin.get(Yliopisto_.id)).value(yliopistoId)
            } else {
                val subquery = query.subquery(Long::class.java)
                val subRoot = subquery.from(KayttajaYliopistoErikoisala::class.java)
                val rootJoin = subRoot.join(KayttajaYliopistoErikoisala_.kayttaja)
                val yliopistoJoin = subRoot.join(KayttajaYliopistoErikoisala_.yliopisto)
                subquery.select(subRoot.get(KayttajaYliopistoErikoisala_.id))
                subquery.where(
                    cb.equal(yliopistoJoin.get(Yliopisto_.id), yliopistoId),
                    cb.equal(root.get(Kayttaja_.id), rootJoin.get(Kayttaja_.id))
                )
                cb.exists(subquery)
            }
        })
    }

    private fun hasOpintooikeusYliopisto(yliopistoId: Long?): Specification<Kayttaja> {
        return (Specification<Kayttaja> { root, query, cb ->
            val subquery = query.subquery(Long::class.java)
            val subRoot = subquery.from(Opintooikeus::class.java)
            val rootJoin = subRoot.join(Opintooikeus_.erikoistuvaLaakari)
            val yliopistoJoin = subRoot.join(Opintooikeus_.yliopisto)
            subquery.select(subRoot.get(Opintooikeus_.id))
            subquery.where(
                cb.equal(yliopistoJoin.get(Yliopisto_.id), yliopistoId),
                cb.equal(root.get(Kayttaja_.id), rootJoin.get(ErikoistuvaLaakari_.kayttaja))
            )
            cb.exists(subquery)
        })
    }

    private fun hasAuthority(authority: String): Specification<Kayttaja> {
        return (Specification<Kayttaja> { root, _, cb ->
            val authorityJoin: Join<User, Authority> = root.join(Kayttaja_.user).join(User_.authorities)
            cb.`in`(authorityJoin.get(Authority_.name)).value(authority)
        })
    }

    private fun hasAuthorities(authorities: List<String>): Specification<Kayttaja> {
        return (Specification<Kayttaja> { root, _, _ ->
            val authorityJoin: Join<User, Authority> = root.join(Kayttaja_.user).join(User_.authorities)
            authorityJoin.get(Authority_.name).`in`(authorities)
        })
    }

    private fun hasCertainOrNoAuthorities(authorities: List<String>): Specification<Kayttaja> {
        return (Specification<Kayttaja> { root, _, cb ->
            val authorityJoin: Join<User, Authority> = root.join(Kayttaja_.user).join(User_.authorities, JoinType.LEFT)
            val predicate1 = authorityJoin.get(Authority_.name).`in`(authorities)
            val predicate2 = cb.isNull(authorityJoin.get(Authority_.name))
            cb.or(predicate1, predicate2)
        })
    }

    private fun hasName(nimiFilter: StringFilter?, langkey: String?): Specification<Kayttaja> {
        return (Specification<Kayttaja> { root, _, cb ->
            val user: Join<Kayttaja, User> = root.join(Kayttaja_.user)
            nimiFilter.toNimiPredicate(user, cb, langkey)
        })
    }

    private fun hasErikoisala(erikoisalaId: LongFilter?): Specification<Kayttaja> {
        return (Specification<Kayttaja> { root, query, cb ->
            erikoisalaId?.let {
                val subquery = query.subquery(Long::class.java)
                val subRoot = subquery.from(KayttajaYliopistoErikoisala::class.java)
                val rootJoin = subRoot.join(KayttajaYliopistoErikoisala_.kayttaja)
                val erikoisalaJoin = subRoot.join(KayttajaYliopistoErikoisala_.erikoisala)
                subquery.select(subRoot.get(KayttajaYliopistoErikoisala_.id))
                subquery.where(
                    cb.equal(erikoisalaJoin.get(Erikoisala_.id), erikoisalaId.equals),
                    cb.equal(root.get(Kayttaja_.id), rootJoin.get(Kayttaja_.id))
                )
                cb.exists(subquery)
            }
        })
    }

    private fun mapKayttaja(kayttaja: Kayttaja) =
        KayttajahallintaKayttajaListItemDTO(
            kayttajaId = kayttaja.id,
            etunimi = kayttaja.user?.firstName,
            sukunimi = kayttaja.user?.lastName,
            yliopistotAndErikoisalat = kayttaja.yliopistotAndErikoisalat.takeIf { it.isNotEmpty() }?.map { ye ->
                KayttajahallintaYliopistoErikoisalaDTO(
                    yliopisto = ye.yliopisto?.nimi,
                    erikoisala = ye.erikoisala?.nimi,
                    vastuuhenkilonTehtavat = ye.vastuuhenkilonTehtavat.map {
                        vastuuhenkilonTehtavatyyppiMapper.toDto(
                            it
                        )
                    }.toSet()
                )
            } ?: kayttaja.yliopistot.map { y ->
                KayttajahallintaYliopistoErikoisalaDTO(
                    yliopisto = y.nimi
                )
            },
            kayttajatilinTila = kayttaja.tila
        )

    private fun mapKayttajaErikoistujaKouluttaja(kayttaja: Kayttaja) =
        KayttajahallintaErikoistujaJaKouluttajaListItemDTO(
            kayttajaId = kayttaja.id,
            etunimi = kayttaja.user?.firstName,
            sukunimi = kayttaja.user?.lastName,
            yliopistotAndErikoisalat = kayttaja.yliopistotAndErikoisalat.takeIf { it.isNotEmpty() }?.map { ye ->
                KayttajahallintaYliopistoErikoisalaDTO(
                    yliopisto = ye.yliopisto?.nimi,
                    erikoisala = ye.erikoisala?.nimi,
                    vastuuhenkilonTehtavat = ye.vastuuhenkilonTehtavat.map {
                        vastuuhenkilonTehtavatyyppiMapper.toDto(
                            it
                        )
                    }.toSet()
                )
            } ?: kayttaja.yliopistot.map { y ->
                KayttajahallintaYliopistoErikoisalaDTO(
                    yliopisto = y.nimi
                )
            },
            authorities = kayttaja.user?.authorities?.takeIf { it.isNotEmpty() }?.map { a -> a.name!! }?.toList(),
            kayttajatilinTila = kayttaja.tila,
            sahkoposti = kayttaja.user?.email!!
        )
}
