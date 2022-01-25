package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.repository.SuoritusarviointiRepository
import fi.elsapalvelu.elsa.service.criteria.SuoritusarviointiCriteria
import fi.elsapalvelu.elsa.service.dto.SuoritusarviointiDTO
import fi.elsapalvelu.elsa.service.mapper.SuoritusarviointiMapper
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tech.jhipster.service.QueryService
import javax.persistence.criteria.Join

@Service
@Transactional(readOnly = true)
class SuoritusarviointiQueryService(
    private val suoritusarviointiRepository: SuoritusarviointiRepository,
    private val suoritusarviointiMapper: SuoritusarviointiMapper
) : QueryService<Suoritusarviointi>() {

    @Transactional(readOnly = true)
    fun findByCriteriaAndTyoskentelyjaksoOpintooikeusId(
        criteria: SuoritusarviointiCriteria?,
        opintooikeusId: Long,
    ): List<SuoritusarviointiDTO> {
        val specification = createSpecification(criteria) { root, _, cb ->
            val opintooikeus: Join<Tyoskentelyjakso, Opintooikeus> = root.join(Suoritusarviointi_.tyoskentelyjakso)
                .join(Tyoskentelyjakso_.opintooikeus)
            cb.equal(opintooikeus.get(Opintooikeus_.id), opintooikeusId)
        }

        return suoritusarviointiRepository.findAll(specification)
            .map(suoritusarviointiMapper::toDto)
    }

    @Transactional(readOnly = true)
    fun findByKouluttajaOrVastuuhenkiloUserId(userId: String): List<SuoritusarviointiDTO> {
        val specification = createSpecification(null) { root, _, cb ->
            val user: Join<Kayttaja, User> = root.join(Suoritusarviointi_.arvioinninAntaja)
                .join(Kayttaja_.user)
            cb.equal(user.get(User_.id), userId)
        }

        return suoritusarviointiRepository.findAll(specification)
            .map(suoritusarviointiMapper::toDto)
    }

    @Suppress("ComplexMethod")
    protected fun createSpecification(
        criteria: SuoritusarviointiCriteria?,
        spec: Specification<Suoritusarviointi?>? = null
    ): Specification<Suoritusarviointi?> {
        var specification: Specification<Suoritusarviointi?> = Specification.where(spec)
        if (criteria != null) {
            if (criteria.id != null) {
                specification =
                    specification.and(buildRangeSpecification(criteria.id, Suoritusarviointi_.id))
            }
            if (criteria.tapahtumanAjankohta != null) {
                specification = specification
                    .and(
                        buildRangeSpecification(
                            criteria.tapahtumanAjankohta,
                            Suoritusarviointi_.tapahtumanAjankohta
                        )
                    )
            }
            if (criteria.arvioitavaTapahtuma != null) {
                specification = specification
                    .and(
                        buildStringSpecification(
                            criteria.arvioitavaTapahtuma,
                            Suoritusarviointi_.arvioitavaTapahtuma
                        )
                    )
            }
            if (criteria.pyynnonAika != null) {
                specification = specification
                    .and(
                        buildRangeSpecification(
                            criteria.pyynnonAika,
                            Suoritusarviointi_.pyynnonAika
                        )
                    )
            }
            if (criteria.vaativuustaso != null) {
                specification = specification
                    .and(
                        buildRangeSpecification(
                            criteria.vaativuustaso,
                            Suoritusarviointi_.vaativuustaso
                        )
                    )
            }
            if (criteria.sanallinenArviointi != null) {
                specification = specification
                    .and(
                        buildStringSpecification(
                            criteria.sanallinenArviointi,
                            Suoritusarviointi_.sanallinenArviointi
                        )
                    )
            }
            if (criteria.arviointiAika != null) {
                specification = specification
                    .and(
                        buildRangeSpecification(
                            criteria.arviointiAika,
                            Suoritusarviointi_.arviointiAika
                        )
                    )
            }
            if (criteria.tyoskentelyjaksoId != null) {
                specification = specification
                    .and(
                        buildReferringEntitySpecification(
                            criteria.tyoskentelyjaksoId,
                            Suoritusarviointi_.tyoskentelyjakso,
                            Tyoskentelyjakso_.id
                        )
                    )
            }
            if (criteria.arvioitavaKokonaisuusId != null) {
                specification = specification
                    .and(
                        buildReferringEntitySpecification(
                            criteria.arvioitavaKokonaisuusId,
                            Suoritusarviointi_.arvioitavaKokonaisuus,
                            ArvioitavaKokonaisuus_.id
                        )
                    )
            }
            if (criteria.arvioinninAntajaId != null) {
                specification = specification
                    .and(
                        buildReferringEntitySpecification(
                            criteria.arvioinninAntajaId,
                            Suoritusarviointi_.arvioinninAntaja,
                            Kayttaja_.id
                        )
                    )
            }
        }
        return specification
    }
}
