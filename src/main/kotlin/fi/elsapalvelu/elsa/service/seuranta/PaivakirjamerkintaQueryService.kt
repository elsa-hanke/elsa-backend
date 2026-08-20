package fi.elsapalvelu.elsa.service.seuranta

import fi.elsapalvelu.elsa.required

import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.domain.koejakso.*
import fi.elsapalvelu.elsa.domain.tyoskentely.*
import fi.elsapalvelu.elsa.domain.arviointi.*
import fi.elsapalvelu.elsa.domain.suoritteet.*
import fi.elsapalvelu.elsa.domain.koulutus.*
import fi.elsapalvelu.elsa.domain.seuranta.*
import fi.elsapalvelu.elsa.domain.valmistuminen.*
import fi.elsapalvelu.elsa.domain.kayttaja.*
import fi.elsapalvelu.elsa.domain.perustiedot.*
import fi.elsapalvelu.elsa.repository.seuranta.PaivakirjamerkintaRepository
import fi.elsapalvelu.elsa.service.criteria.PaivakirjamerkintaCriteria
import fi.elsapalvelu.elsa.service.dto.seuranta.PaivakirjamerkintaDTO
import fi.elsapalvelu.elsa.service.mapper.seuranta.PaivakirjamerkintaMapper
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

    private fun createSpecification(criteria: PaivakirjamerkintaCriteria?, spec: Specification<Paivakirjamerkinta?>? = null): Specification<Paivakirjamerkinta?> {
        var specification: Specification<Paivakirjamerkinta?> = spec ?: Specification.unrestricted()
        if (criteria != null) {
            if (criteria.id != null) {
                specification = specification.and { root, _, cb -> cb.equal(root.get(Paivakirjamerkinta_.id), criteria.id.required().equals) }
            }
            if (criteria.paivamaara != null) {
                if (criteria.paivamaara.required().specified != null) {
                    specification = specification.and { root, _, cb ->
                            if (criteria.paivamaara.required().specified) cb.isNotNull(root.get(Paivakirjamerkinta_.paivamaara))
                            else cb.isNull(root.get(Paivakirjamerkinta_.paivamaara))
                        }
                }
                if (criteria.paivamaara.required().greaterThanOrEqual != null) {
                    specification = specification.and { root, _, cb ->
                            cb.greaterThanOrEqualTo(root.get(Paivakirjamerkinta_.paivamaara), criteria.paivamaara.required().greaterThanOrEqual)
                        }
                }
                if (criteria.paivamaara.required().lessThanOrEqual != null) {
                    specification = specification.and { root, _, cb ->
                            cb.lessThanOrEqualTo(root.get(Paivakirjamerkinta_.paivamaara), criteria.paivamaara.required().lessThanOrEqual)
                        }
                }
                if (criteria.paivamaara.required().equals != null) {
                    specification = specification.and { root, _, cb ->
                            cb.equal(root.get(Paivakirjamerkinta_.paivamaara), criteria.paivamaara.required().equals)
                        }
                }
            }
            if (criteria.oppimistapahtumanNimi != null) {
                specification = specification.and { root, _, cb ->
                        cb.equal(root.get(Paivakirjamerkinta_.oppimistapahtumanNimi), criteria.oppimistapahtumanNimi.required().equals)
                    }
            }
            if (criteria.muunAiheenNimi != null) {
                specification = specification.and { root, _, cb ->
                        cb.equal(root.get(Paivakirjamerkinta_.muunAiheenNimi), criteria.muunAiheenNimi.required().equals)
                    }
            }
            if (criteria.yksityinen != null) {
                specification = specification.and { root, _, cb ->
                        cb.equal(root.get(Paivakirjamerkinta_.yksityinen), criteria.yksityinen.required().equals)
                    }
            }
            if (criteria.aihekategoriaId != null) {
                specification = specification.and { root, _, cb ->
                        cb.equal(root.join(Paivakirjamerkinta_.aihekategoriat, JoinType.LEFT).get(PaivakirjaAihekategoria_.id),
                            criteria.aihekategoriaId.required().equals)
                    }
            }
            if (criteria.teoriakoulutusId != null) {
                specification = specification.and { root, _, cb ->
                        cb.equal(root.join(Paivakirjamerkinta_.teoriakoulutus, JoinType.LEFT).get(Teoriakoulutus_.id),
                            criteria.teoriakoulutusId.required().equals)
                    }
            }
        }
        return specification
    }
}
