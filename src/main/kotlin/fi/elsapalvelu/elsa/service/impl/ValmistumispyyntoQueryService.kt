package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.config.YEK_ERIKOISALA_ID
import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.domain.enumeration.VastuuhenkilonTehtavatyyppiEnum
import fi.elsapalvelu.elsa.extensions.toNimiPredicate
import fi.elsapalvelu.elsa.repository.ValmistumispyyntoRepository
import fi.elsapalvelu.elsa.service.criteria.NimiErikoisalaAndAvoinCriteria
import fi.elsapalvelu.elsa.service.dto.enumeration.ValmistumispyynnonHyvaksyjaRole
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
class ValmistumispyyntoQueryService(
    private val valmistumispyyntoRepository: ValmistumispyyntoRepository
) {

    @Transactional(readOnly = true)
    fun findValmistumispyynnotByCriteriaForVirkailija(
        criteria: NimiErikoisalaAndAvoinCriteria?,
        pageable: Pageable,
        yliopistoId: Long,
        erikoisalaIds: List<Long>,
        excludedErikoisalaIds: List<Long>,
        langkey: String?
    ): Page<Valmistumispyynto> {
        val specification: Specification<Valmistumispyynto> = Specification.where { root, cq, cb ->
            val predicates: MutableList<Predicate> = mutableListOf()
            val opintooikeusJoin: Join<Valmistumispyynto?, Opintooikeus> =
                root.join(Valmistumispyynto_.opintooikeus)

            if (criteria?.avoin == true) {
                predicates.add(
                    cb.and(
                        cb.isNotNull(root.get(Valmistumispyynto_.erikoistujanKuittausaika)),
                        cb.isNull(root.get(Valmistumispyynto_.virkailijanPalautusaika)),
                        cb.isNull(root.get(Valmistumispyynto_.virkailijanKuittausaika))
                    )
                )

                if (!erikoisalaIds.contains(YEK_ERIKOISALA_ID)) {
                    predicates.add(cb.and(cb.isNotNull(root.get(Valmistumispyynto_.vastuuhenkiloOsaamisenArvioijaKuittausaika))))
                }
            } else {
                predicates.add(
                    cb.or(
                        cb.isNotNull(root.get(Valmistumispyynto_.virkailijanPalautusaika)),
                        cb.isNotNull(root.get(Valmistumispyynto_.virkailijanKuittausaika)),
                        cb.and(
                            cb.isNotNull(root.get(Valmistumispyynto_.vastuuhenkiloHyvaksyjaPalautusaika)),
                            cb.isNull(root.get(Valmistumispyynto_.erikoistujanKuittausaika))
                        )
                    )
                )
            }
            if (erikoisalaIds.isNotEmpty()) {
                getErikoisalatPredicate(
                    erikoisalaIds,
                    opintooikeusJoin
                )?.let {
                    predicates.add(it)
                }
            }
            if (excludedErikoisalaIds.isNotEmpty()) {
                getExcludedErikoisalatPredicate(
                    excludedErikoisalaIds,
                    opintooikeusJoin,
                    cb
                )?.let {
                    predicates.add(it)
                }
            }
            getYliopistoPredicate(yliopistoId, opintooikeusJoin, cb).let { predicates.add(it) }
            getNimiPredicate(criteria?.erikoistujanNimi, opintooikeusJoin, cb, langkey)?.let { predicates.add(it) }

            cb.and(*predicates.toTypedArray())
        }
        return valmistumispyyntoRepository.findAll(specification, pageable)
    }

    @Transactional(readOnly = true)
    fun findValmistumispyynnotByCriteria(
        criteria: NimiErikoisalaAndAvoinCriteria?,
        role: List<Pair<Long, ValmistumispyynnonHyvaksyjaRole>>,
        pageable: Pageable,
        yliopistoId: Long,
        erikoisalaIds: List<Long>,
        excludedErikoisalaIds: List<Long>,
        langkey: String?
    ): Page<Valmistumispyynto> {
        val specification: Specification<Valmistumispyynto> = Specification.where { root, cq, cb ->
            val predicates: MutableList<Predicate> = mutableListOf()
            val opintooikeusJoin: Join<Valmistumispyynto?, Opintooikeus> =
                root.join(Valmistumispyynto_.opintooikeus)

                getVastuuhenkiloPredicate(
                    role,
                    criteria,
                    opintooikeusJoin,
                    root,
                    cb
                ).let { predicates.add(it) }
                getErikoisalatPredicate(erikoisalaIds, opintooikeusJoin)?.let {
                    predicates.add(it)
                }
                if (excludedErikoisalaIds.isNotEmpty()) {
                    getExcludedErikoisalatPredicate(
                        excludedErikoisalaIds,
                        opintooikeusJoin,
                        cb
                    )?.let {
                        predicates.add(it)
                    }
                }

            getYliopistoPredicate(yliopistoId, opintooikeusJoin, cb).let { predicates.add(it) }
            getNimiPredicate(criteria?.erikoistujanNimi, opintooikeusJoin, cb, langkey)?.let { predicates.add(it) }

            cb.and(*predicates.toTypedArray())
        }
        return valmistumispyyntoRepository.findAll(specification, pageable)
    }


    private fun getVastuuhenkiloPredicate(
        role: List<Pair<Long, ValmistumispyynnonHyvaksyjaRole>>,
        criteria: NimiErikoisalaAndAvoinCriteria?,
        opintooikeusJoin: Join<Valmistumispyynto?, Opintooikeus>,
        root: Root<Valmistumispyynto>,
        cb: CriteriaBuilder
    ): Predicate {
        val erikoisala: Path<Erikoisala> = opintooikeusJoin.get(Opintooikeus_.erikoisala)
        val predicates: MutableList<Predicate> = mutableListOf()

        role.forEach {
            predicates.add(
                cb.and(
                    cb.equal(erikoisala.get(Erikoisala_.id), it.first),
                    getRolePredicate(it.second, criteria, root, cb)
                )
            )
        }

        return cb.or(*predicates.toTypedArray())


    }

    private fun getRolePredicate(role: ValmistumispyynnonHyvaksyjaRole, criteria: NimiErikoisalaAndAvoinCriteria?, root: Root<Valmistumispyynto>, cb: CriteriaBuilder): Predicate {
        val arvioijaPredicate = getVastuuhenkiloPredicateByTehtavatyyppi(
            root,
            cb,
            criteria?.avoin,
            VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI
        )
        val hyvaksyjaPredicate = getVastuuhenkiloPredicateByTehtavatyyppi(
            root,
            cb,
            criteria?.avoin,
            if (criteria?.erikoisalaId?.equals == YEK_ERIKOISALA_ID) VastuuhenkilonTehtavatyyppiEnum.YEK_VALMISTUMINEN else VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_HYVAKSYNTA
        )

        return when (role) {
            ValmistumispyynnonHyvaksyjaRole.VASTUUHENKILO_OSAAMISEN_ARVIOIJA_HYVAKSYJA -> cb.or(
                cb.and(
                    cb.isNull(root.get(Valmistumispyynto_.virkailijanKuittausaika)),
                    arvioijaPredicate
                ),
                cb.and(
                    cb.isNotNull(root.get(Valmistumispyynto_.virkailijanKuittausaika)),
                    hyvaksyjaPredicate
                )
            )

            ValmistumispyynnonHyvaksyjaRole.VASTUUHENKILO_OSAAMISEN_ARVIOIJA -> arvioijaPredicate
            else -> hyvaksyjaPredicate
        }
    }

    private fun getVastuuhenkiloPredicateByTehtavatyyppi(
        root: Root<Valmistumispyynto>,
        cb: CriteriaBuilder,
        avoin: Boolean? = true,
        tehtavatyyppiEnum: VastuuhenkilonTehtavatyyppiEnum
    ): Predicate {
        if (avoin == true) {
            return when (tehtavatyyppiEnum) {
                VastuuhenkilonTehtavatyyppiEnum.YEK_VALMISTUMINEN -> {
                    cb.and(
                        cb.isNull(root.get(Valmistumispyynto_.vastuuhenkiloHyvaksyjaKuittausaika)),
                        cb.isNotNull(root.get(Valmistumispyynto_.virkailijanKuittausaika)),
                        cb.isNull(root.get(Valmistumispyynto_.vastuuhenkiloHyvaksyjaPalautusaika))
                    )
                }

                VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI -> {
                    cb.and(
                        cb.isNotNull(root.get(Valmistumispyynto_.erikoistujanKuittausaika)),
                        cb.isNull(root.get(Valmistumispyynto_.vastuuhenkiloOsaamisenArvioijaKuittausaika)),
                        cb.isNull(root.get(Valmistumispyynto_.vastuuhenkiloOsaamisenArvioijaPalautusaika)),
                    )
                }

                else -> {
                    cb.and(
                        cb.or(
                            cb.isNull(root.get(Valmistumispyynto_.vastuuhenkiloHyvaksyjaKuittausaika)),
                            cb.isNull(root.get(Valmistumispyynto_.allekirjoitusaika))
                        ),
                        cb.isNotNull(root.get(Valmistumispyynto_.vastuuhenkiloOsaamisenArvioijaKuittausaika)),
                        cb.isNotNull(root.get(Valmistumispyynto_.virkailijanKuittausaika)),
                        cb.isNull(root.get(Valmistumispyynto_.vastuuhenkiloHyvaksyjaPalautusaika))
                    )
                }
            }
        } else {
            return when (tehtavatyyppiEnum) {
                VastuuhenkilonTehtavatyyppiEnum.YEK_VALMISTUMINEN -> {
                    cb.or(
                        cb.isNotNull(root.get(Valmistumispyynto_.vastuuhenkiloHyvaksyjaPalautusaika)),
                        cb.isNotNull(root.get(Valmistumispyynto_.vastuuhenkiloHyvaksyjaKuittausaika)),
                    )
                }

                VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI -> {
                    cb.or(
                        cb.isNotNull(root.get(Valmistumispyynto_.vastuuhenkiloOsaamisenArvioijaKuittausaika)),
                        cb.and(
                            cb.isNull(root.get(Valmistumispyynto_.erikoistujanKuittausaika)),
                            cb.or(
                                cb.isNotNull(root.get(Valmistumispyynto_.vastuuhenkiloOsaamisenArvioijaPalautusaika)),
                                cb.isNotNull(root.get(Valmistumispyynto_.virkailijanPalautusaika)),
                                cb.isNotNull(root.get(Valmistumispyynto_.vastuuhenkiloHyvaksyjaPalautusaika))
                            )
                        )
                    )
                }

                else -> {
                    cb.or(
                        cb.isNotNull(root.get(Valmistumispyynto_.vastuuhenkiloHyvaksyjaPalautusaika)),
                        cb.and(
                            cb.isNotNull(root.get(Valmistumispyynto_.vastuuhenkiloHyvaksyjaKuittausaika)),
                            cb.isNotNull(root.get(Valmistumispyynto_.allekirjoitusaika))
                        )
                    )
                }
            }
        }
    }

    private fun getYliopistoPredicate(
        yliopistoId: Long,
        opintooikeusJoin: Join<Valmistumispyynto?, Opintooikeus>,
        cb: CriteriaBuilder
    ): Predicate {
        val yliopisto: Path<Yliopisto> = opintooikeusJoin.get(Opintooikeus_.yliopisto)
        return cb.equal(yliopisto.get(Yliopisto_.id), yliopistoId)
    }

    private fun getErikoisalaPredicate(
        erikoisalaId: LongFilter?,
        opintooikeusJoin: Join<Valmistumispyynto?, Opintooikeus>,
        cb: CriteriaBuilder
    ): Predicate? {
        return erikoisalaId?.let {
            val erikoisala: Path<Erikoisala> = opintooikeusJoin.get(Opintooikeus_.erikoisala)
            cb.equal(erikoisala.get(Erikoisala_.id), erikoisalaId.equals)
        }
    }

    private fun getErikoisalatPredicate(
        erikoisalaIds: List<Long>,
        opintooikeusJoin: Join<Valmistumispyynto?, Opintooikeus>,
    ): Predicate? {
        val erikoisala: Path<Erikoisala> = opintooikeusJoin.get(Opintooikeus_.erikoisala)
        return (erikoisala.get(Erikoisala_.id)).`in`(erikoisalaIds)
    }

    private fun getExcludedErikoisalatPredicate(
        excludedErikoisalaIds: List<Long>,
        opintooikeusJoin: Join<Valmistumispyynto?, Opintooikeus>,
        cb: CriteriaBuilder
    ): Predicate? {
        val erikoisala: Path<Erikoisala> = opintooikeusJoin.get(Opintooikeus_.erikoisala)
        return cb.not((erikoisala.get(Erikoisala_.id)).`in`(excludedErikoisalaIds))
    }

    private fun getNimiPredicate(
        nimiFilter: StringFilter?,
        opintooikeusJoin: Join<Valmistumispyynto?, Opintooikeus>,
        cb: CriteriaBuilder,
        langkey: String?
    ): Predicate? {
        val erikoistuvaLaakariJoin: Join<Opintooikeus, ErikoistuvaLaakari> =
            opintooikeusJoin.join(Opintooikeus_.erikoistuvaLaakari)
        val kayttajaJoin: Join<ErikoistuvaLaakari, Kayttaja> = erikoistuvaLaakariJoin.join(ErikoistuvaLaakari_.kayttaja)
        val userJoin: Join<Kayttaja, User> = kayttajaJoin.join(Kayttaja_.user)
        return nimiFilter.toNimiPredicate(userJoin, cb, langkey)
    }
}
