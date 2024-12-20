package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.repository.PaivakirjamerkintaRepository
import fi.elsapalvelu.elsa.service.criteria.PaivakirjamerkintaCriteria
import fi.elsapalvelu.elsa.service.dto.PaivakirjamerkintaDTO
import fi.elsapalvelu.elsa.service.mapper.PaivakirjamerkintaMapper
import jakarta.persistence.criteria.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PaivakirjamerkintaQueryService(
    private val paivakirjamerkintaRepository: PaivakirjamerkintaRepository,
    private val paivakirjamerkintaMapper: PaivakirjamerkintaMapper
) {

    @Transactional(readOnly = true)
    fun findByCriteriaAndOpintooikeusId(
        criteria: PaivakirjamerkintaCriteria?,
        page: Pageable,
        opintooikeusId: Long
    ): Page<PaivakirjamerkintaDTO> {
        val specification = createSpecification(criteria) { root, _, cb ->
            val opintooikeus: Path<Opintooikeus> = root.get(Paivakirjamerkinta_.opintooikeus)
            cb.equal(opintooikeus.get(Opintooikeus_.id), opintooikeusId)
        }

        return paivakirjamerkintaRepository.findAll(specification, page)
            .map(paivakirjamerkintaMapper::toDto)
    }

    @Suppress("ComplexMethod")
    protected fun createSpecification(
        criteria: PaivakirjamerkintaCriteria?,
        spec: Specification<Paivakirjamerkinta?>? = null
    ): Specification<Paivakirjamerkinta?> {
        var specification: Specification<Paivakirjamerkinta?> = Specification.where(spec)
        if (criteria != null) {
            if (criteria.id != null) {
                specification =
                    specification.and { root, _, cb ->
                        cb.equal(
                            root.get(Paivakirjamerkinta_.id),
                            criteria.id!!.equals
                        )
                    }
            }
            if (criteria.paivamaara != null) {
                if (criteria.paivamaara!!.specified != null) {
                    specification =
                        specification.and { root, _, cb ->
                            if (criteria.paivamaara!!.specified) cb.isNotNull(
                                root.get(
                                    Paivakirjamerkinta_.paivamaara
                                )
                            ) else cb.isNull(root.get(Paivakirjamerkinta_.paivamaara))
                        }
                }
                if (criteria.paivamaara!!.greaterThanOrEqual != null) {
                    specification =
                        specification.and { root, _, cb ->
                            cb.greaterThanOrEqualTo(
                                root.get(Paivakirjamerkinta_.paivamaara),
                                criteria.paivamaara!!.greaterThanOrEqual
                            )
                        }
                }
                if (criteria.paivamaara!!.lessThanOrEqual != null) {
                    specification =
                        specification.and { root, _, cb ->
                            cb.lessThanOrEqualTo(
                                root.get(Paivakirjamerkinta_.paivamaara),
                                criteria.paivamaara!!.lessThanOrEqual
                            )
                        }
                }
                if (criteria.paivamaara!!.equals != null) {
                    specification =
                        specification.and { root, _, cb ->
                            cb.equal(
                                root.get(Paivakirjamerkinta_.paivamaara),
                                criteria.paivamaara!!.equals
                            )
                        }
                }
            }
            if (criteria.oppimistapahtumanNimi != null) {
                specification =
                    specification.and { root, _, cb ->
                        cb.equal(
                            root.get(Paivakirjamerkinta_.oppimistapahtumanNimi),
                            criteria.oppimistapahtumanNimi!!.equals
                        )
                    }
            }
            if (criteria.muunAiheenNimi != null) {
                specification =
                    specification.and { root, _, cb ->
                        cb.equal(
                            root.get(Paivakirjamerkinta_.muunAiheenNimi),
                            criteria.muunAiheenNimi!!.equals
                        )
                    }
            }
            if (criteria.yksityinen != null) {
                specification =
                    specification.and { root, _, cb ->
                        cb.equal(
                            root.get(Paivakirjamerkinta_.yksityinen),
                            criteria.yksityinen!!.equals
                        )
                    }
            }
            if (criteria.aihekategoriaId != null) {
                specification =
                    specification.and { root, _, cb ->
                        cb.equal(
                            root.join(Paivakirjamerkinta_.aihekategoriat, JoinType.LEFT)
                                .get(PaivakirjaAihekategoria_.id),
                            criteria.aihekategoriaId!!.equals
                        )
                    }
            }
            if (criteria.teoriakoulutusId != null) {
                specification =
                    specification.and { root, _, cb ->
                        cb.equal(
                            root.join(Paivakirjamerkinta_.teoriakoulutus, JoinType.LEFT)
                                .get(Teoriakoulutus_.id),
                            criteria.teoriakoulutusId!!.equals
                        )
                    }
            }
        }
        return specification
    }
}
