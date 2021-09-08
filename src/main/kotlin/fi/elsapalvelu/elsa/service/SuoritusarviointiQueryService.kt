package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.repository.SuoritusarviointiRepository
import fi.elsapalvelu.elsa.service.dto.SuoritusarviointiCriteria
import fi.elsapalvelu.elsa.service.dto.SuoritusarviointiDTO
import fi.elsapalvelu.elsa.service.mapper.SuoritusarviointiMapper
import io.github.jhipster.service.QueryService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
@Service
@Transactional(readOnly = true)
class SuoritusarviointiQueryService(
    private val suoritusarviointiRepository: SuoritusarviointiRepository,
    private val suoritusarviointiMapper: SuoritusarviointiMapper
) : QueryService<Suoritusarviointi>() {

    @Transactional(readOnly = true)
    fun findByCriteria(criteria: SuoritusarviointiCriteria?): MutableList<SuoritusarviointiDTO> {
        val specification = createSpecification(criteria)
        return suoritusarviointiMapper.toDto(suoritusarviointiRepository.findAll(specification))
    }

    @Transactional(readOnly = true)
    fun findByCriteria(
        criteria: SuoritusarviointiCriteria?,
        page: Pageable
    ): Page<SuoritusarviointiDTO> {
        val specification = createSpecification(criteria)
        return suoritusarviointiRepository.findAll(specification, page)
            .map(suoritusarviointiMapper::toDto)
    }

    @Transactional(readOnly = true)
    fun findByCriteriaAndTyoskentelyjaksoErikoistuvaLaakariKayttajaId(
        criteria: SuoritusarviointiCriteria?,
        kayttajaId: String,
        page: Pageable

    ): Page<SuoritusarviointiDTO> {
        val specification = createSpecification(criteria) { root, _, cb ->
            val kayttaja = root.join(Suoritusarviointi_.tyoskentelyjakso)
                .join(Tyoskentelyjakso_.erikoistuvaLaakari)
                .join(ErikoistuvaLaakari_.kayttaja)
            cb.equal(kayttaja.get(Kayttaja_.id), kayttajaId)
        }

        return suoritusarviointiRepository.findAll(specification, page)
            .map(suoritusarviointiMapper::toDto)
    }

    @Transactional(readOnly = true)
    fun findByCriteriaAndTyoskentelyjaksoErikoistuvaLaakariKayttajaId(
        criteria: SuoritusarviointiCriteria?,
        kayttajaId: String,
    ): List<SuoritusarviointiDTO> {
        val specification = createSpecification(criteria) { root, _, cb ->
            val kayttaja = root.join(Suoritusarviointi_.tyoskentelyjakso)
                .join(Tyoskentelyjakso_.erikoistuvaLaakari)
                .join(ErikoistuvaLaakari_.kayttaja)
            cb.equal(kayttaja.get(Kayttaja_.id), kayttajaId)
        }

        return suoritusarviointiRepository.findAll(specification)
            .map(suoritusarviointiMapper::toDto)
    }

    @Transactional(readOnly = true)
    fun countByCriteria(criteria: SuoritusarviointiCriteria?): Long {
        val specification = createSpecification(criteria)
        return suoritusarviointiRepository.count(specification)
    }

    @Transactional(readOnly = true)
    fun findByKouluttajaOrVastuuhenkiloId(kayttajaId: String): List<SuoritusarviointiDTO> {
        val specification = createSpecification(null) { root, _, cb ->
            val kayttaja = root.join(Suoritusarviointi_.arvioinninAntaja)
            cb.equal(kayttaja.get(Kayttaja_.id), kayttajaId)
        }

        return suoritusarviointiRepository.findAll(specification)
            .map(suoritusarviointiMapper::toDto)
    }

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
            if (criteria.arvioitavaOsaalueId != null) {
                specification = specification
                    .and(
                        buildReferringEntitySpecification(
                            criteria.arvioitavaOsaalueId,
                            Suoritusarviointi_.arvioitavaOsaalue,
                            EpaOsaamisalue_.id
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
