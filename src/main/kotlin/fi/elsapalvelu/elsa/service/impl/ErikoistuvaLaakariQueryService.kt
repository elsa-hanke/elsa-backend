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
import tech.jhipster.service.filter.BooleanFilter
import tech.jhipster.service.filter.LongFilter
import tech.jhipster.service.filter.StringFilter
import jakarta.persistence.criteria.*

@Service
@Transactional(readOnly = true)
class ErikoistuvaLaakariQueryService(
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository
) {

    @Transactional(readOnly = true)
    fun findErikoistuvatByCriteriaAndYliopistoId(
        criteria: KayttajahallintaCriteria?,
        pageable: Pageable,
        yliopistoId: Long?,
        langkey: String?
    ): Page<KayttajahallintaKayttajaListItemDTO> {
        val specification = Specification
            .where(hasName(criteria?.nimi, langkey))
            .and(hasErikoisala(criteria?.erikoisalaId))
            .and(hasYliopisto(yliopistoId))
            .and(hasUseaOpintooikeus(criteria?.useaOpintooikeus))
        return erikoistuvaLaakariRepository.findAll(specification, pageable).map { mapErikoistuvaLaakari(it) }
    }

    @Transactional(readOnly = true)
    fun findErikoistuvatByCriteria(
        criteria: KayttajahallintaCriteria?,
        pageable: Pageable,
        langkey: String?
    ): Page<KayttajahallintaKayttajaListItemDTO> {
        val specification = Specification
            .where(hasName(criteria?.nimi, langkey))
            .and(hasErikoisala(criteria?.erikoisalaId))
            .and(hasUseaOpintooikeus(criteria?.useaOpintooikeus))
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

    private fun hasName(nimiFilter: StringFilter?, langkey: String?): Specification<ErikoistuvaLaakari> {
        return (Specification<ErikoistuvaLaakari> { root, _, cb ->
            val user: Join<Kayttaja, User> = root.join(ErikoistuvaLaakari_.kayttaja)
                .join(Kayttaja_.user)
            nimiFilter.toNimiPredicate(user, cb, langkey)
        })
    }

    private fun hasErikoisala(erikoisalaId: LongFilter?): Specification<ErikoistuvaLaakari> {
        return (Specification<ErikoistuvaLaakari> { root, query, cb ->
            erikoisalaId?.let {
                val subquery = query.subquery(Long::class.java)
                val subRoot = subquery.from(Opintooikeus::class.java)
                val rootJoin = subRoot.join(Opintooikeus_.erikoistuvaLaakari)
                val erikoisalaJoin = subRoot.join(Opintooikeus_.erikoisala)
                subquery.select(subRoot.get(Opintooikeus_.id))
                subquery.where(
                    cb.equal(erikoisalaJoin.get(Erikoisala_.id), erikoisalaId.equals),
                    cb.equal(root.get(ErikoistuvaLaakari_.id), rootJoin.get(ErikoistuvaLaakari_.id))
                )
                cb.exists(subquery)
            }
        })
    }

    private fun hasYliopisto(yliopistoId: Long?): Specification<ErikoistuvaLaakari> {
        return (Specification<ErikoistuvaLaakari> { root, query, cb ->
            yliopistoId?.let {
                val subquery = query.subquery(Long::class.java)
                val subRoot = subquery.from(Opintooikeus::class.java)
                val rootJoin = subRoot.join(Opintooikeus_.erikoistuvaLaakari)
                val yliopistoJoin = subRoot.join(Opintooikeus_.yliopisto)
                subquery.select(subRoot.get(Opintooikeus_.id))
                subquery.where(
                    cb.equal(yliopistoJoin.get(Yliopisto_.id), yliopistoId),
                    cb.equal(root.get(ErikoistuvaLaakari_.id), rootJoin.get(ErikoistuvaLaakari_.id))
                )
                cb.exists(subquery)
            }
        })
    }

    private fun hasUseaOpintooikeus(useaOpintooikeus: BooleanFilter?): Specification<ErikoistuvaLaakari> {
        return (Specification<ErikoistuvaLaakari> { root, query, cb ->
            if (useaOpintooikeus?.equals == true) {
                val subquery = query.subquery(Long::class.java)
                val subRoot = subquery.from(Opintooikeus::class.java)
                val rootJoin = subRoot.join(Opintooikeus_.erikoistuvaLaakari)
                subquery.select(cb.count(subRoot.get(Opintooikeus_.id)))
                subquery.where(cb.equal(root.get(ErikoistuvaLaakari_.id), rootJoin.get(ErikoistuvaLaakari_.id)))
                cb.greaterThan(subquery, 1L)
            } else {
                null
            }
        })
    }
}

