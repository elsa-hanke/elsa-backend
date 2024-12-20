package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.repository.SuoritusarviointiRepository
import fi.elsapalvelu.elsa.service.criteria.SuoritusarviointiCriteria
import fi.elsapalvelu.elsa.service.dto.SuoritusarviointiDTO
import fi.elsapalvelu.elsa.service.mapper.SuoritusarviointiMapper
import jakarta.persistence.criteria.*
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class SuoritusarviointiQueryService(
    private val suoritusarviointiRepository: SuoritusarviointiRepository,
    private val suoritusarviointiMapper: SuoritusarviointiMapper
) {

    @Transactional(readOnly = true)
    fun findByCriteriaAndTyoskentelyjaksoOpintooikeusId(
        criteria: SuoritusarviointiCriteria?,
        opintooikeusId: Long,
    ): List<SuoritusarviointiDTO> {
        val specification = createSpecification(criteria) { root, _, cb ->
            val opintooikeus: Join<Tyoskentelyjakso, Opintooikeus> =
                root.join(Suoritusarviointi_.tyoskentelyjakso)
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
                    specification.and { root, _, cb ->
                        cb.equal(
                            root.get(Suoritusarviointi_.id),
                            criteria.id!!.equals
                        )
                    }
            }
            if (criteria.tapahtumanAjankohta != null) {
                specification =
                    specification.and { root, _, cb ->
                        cb.equal(
                            root.get(Suoritusarviointi_.tapahtumanAjankohta),
                            criteria.tapahtumanAjankohta!!.equals
                        )
                    }
            }
            if (criteria.arvioitavaTapahtuma != null) {
                specification =
                    specification.and { root, _, cb ->
                        cb.equal(
                            root.get(Suoritusarviointi_.arvioitavaTapahtuma),
                            criteria.arvioitavaTapahtuma!!.equals
                        )
                    }
            }
            if (criteria.pyynnonAika != null) {
                specification =
                    specification.and { root, _, cb ->
                        cb.equal(
                            root.get(Suoritusarviointi_.pyynnonAika),
                            criteria.pyynnonAika!!.equals
                        )
                    }
            }
            if (criteria.vaativuustaso != null) {
                specification =
                    specification.and { root, _, cb ->
                        cb.equal(
                            root.get(Suoritusarviointi_.vaativuustaso),
                            criteria.vaativuustaso!!.equals
                        )
                    }
            }
            if (criteria.sanallinenArviointi != null) {
                specification =
                    specification.and { root, _, cb ->
                        cb.equal(
                            root.get(Suoritusarviointi_.sanallinenArviointi),
                            criteria.sanallinenArviointi!!.equals
                        )
                    }
            }
            if (criteria.arviointiAika != null) {
                specification =
                    specification.and { root, _, cb ->
                        if (criteria.arviointiAika!!.specified != null) {
                            if (criteria.arviointiAika!!.specified) cb.isNotNull(
                                root.get(
                                    Suoritusarviointi_.arviointiAika
                                )
                            ) else cb.isNull(root.get(Suoritusarviointi_.arviointiAika))
                        } else {
                            cb.equal(
                                root.get(Suoritusarviointi_.arviointiAika),
                                criteria.arviointiAika!!.equals
                            )
                        }
                    }
            }
            if (criteria.tyoskentelyjaksoId != null) {
                specification =
                    specification.and { root, _, cb ->
                        cb.equal(
                            root.join(Suoritusarviointi_.tyoskentelyjakso, JoinType.INNER)
                                .get(Tyoskentelyjakso_.id),
                            criteria.tyoskentelyjaksoId!!.equals
                        )
                    }
            }
            if (criteria.arvioitavaKokonaisuusId != null) {
                val kokonaisuusSpecification = createSpecification(null) { root, _, cb ->
                    val arvioitavaKokonaisuusJoin =
                        root.join(Suoritusarviointi_.arvioitavatKokonaisuudet)
                            .join(SuoritusarvioinninArvioitavaKokonaisuus_.arvioitavaKokonaisuus)
                    cb.equal(
                        arvioitavaKokonaisuusJoin.get(ArvioitavaKokonaisuus_.id),
                        criteria.arvioitavaKokonaisuusId
                    )
                }
                specification = specification.and(kokonaisuusSpecification)
            }
            if (criteria.arvioinninAntajaId != null) {
                specification =
                    specification.and { root, _, cb ->
                        cb.equal(
                            root.join(Suoritusarviointi_.arvioinninAntaja, JoinType.INNER)
                                .get(Kayttaja_.id),
                            criteria.arvioinninAntajaId!!.equals
                        )
                    }
            }
        }
        return specification
    }
}
