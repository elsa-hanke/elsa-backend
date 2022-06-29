package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.Authority
import fi.elsapalvelu.elsa.domain.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.Kayttaja
import fi.elsapalvelu.elsa.domain.VastuuhenkilonTehtavatyyppi
import fi.elsapalvelu.elsa.repository.ErikoisalaRepository
import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.KayttajaRepository
import fi.elsapalvelu.elsa.repository.KayttajaYliopistoErikoisalaRepository
import fi.elsapalvelu.elsa.security.TEKNINEN_PAAKAYTTAJA
import fi.elsapalvelu.elsa.service.KayttajahallintaValidationService
import fi.elsapalvelu.elsa.service.constants.ERIKOISTUVA_LAAKARI_NOT_FOUND_ERROR
import fi.elsapalvelu.elsa.service.constants.KAYTTAJA_NOT_FOUND_ERROR
import fi.elsapalvelu.elsa.service.dto.*
import fi.elsapalvelu.elsa.service.dto.enumeration.ReassignedVastuuhenkilonTehtavaTyyppi
import fi.elsapalvelu.elsa.service.dto.kayttajahallinta.KayttajahallintaKayttajaDTO
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityNotFoundException

const val VIRKAILIJA_YLIOPISTO_NOT_FOUND_MSG = "Virkailijalle ei ole määritelty yliopistoa"

@Service
class KayttajahallintaValidationServiceImpl(
    private val kayttajaYliopistoErikoisalaRepository: KayttajaYliopistoErikoisalaRepository,
    private val erikoisalaRepository: ErikoisalaRepository,
    private val kayttajaRepository: KayttajaRepository,
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository
) : KayttajahallintaValidationService {

    @Transactional
    override fun validateNewVastuuhenkiloYliopistotAndErikoisalat(kayttajahallintaKayttajaDTO: KayttajahallintaKayttajaDTO): Boolean {
        var isValid = true
        val yliopistotAndErikoisalat = kayttajahallintaKayttajaDTO.yliopistotAndErikoisalat

        yliopistotAndErikoisalat.forEach yliopistotErikoisalat@{ kayttajaYliopistoErikoisalaDTO ->
            val assignedTehtavatForErikoisala =
                getAssignedTehtavatForErikoisala(kayttajaYliopistoErikoisalaDTO)

            // Jos muokattava käyttäjä on ainut vastuuhenkilö kyseisellä erikoisalalla, täytyy kaikkien erikoisalojen
            // tehtävien olla määritetty tälle.
            if (assignedTehtavatForErikoisala.isEmpty()) {
                val erikoisalaTehtavat =
                    getAllTehtavatForErikoisala(kayttajaYliopistoErikoisalaDTO.erikoisala?.id!!)
                if (kayttajaYliopistoErikoisalaDTO.vastuuhenkilonTehtavat.size != erikoisalaTehtavat.size) {
                    isValid = false
                    return@yliopistotErikoisalat
                }
            }

            kayttajaYliopistoErikoisalaDTO.vastuuhenkilonTehtavat.forEach {
                if (persistedTehtavatContains(assignedTehtavatForErikoisala, it.id) && !isReassignedTehtava(
                            kayttajahallintaKayttajaDTO.reassignedTehtavat,
                            kayttajaYliopistoErikoisalaDTO.erikoisala?.id!!,
                            it.id,
                            ReassignedVastuuhenkilonTehtavaTyyppi.REMOVE
                        )) {
                    isValid = false
                    return@yliopistotErikoisalat
                }
            }
        }

        return isValid
    }

    @Transactional
    override fun validateExistingVastuuhenkiloYliopistotAndErikoisalat(
        kayttajahallintaKayttajaDTO: KayttajahallintaKayttajaDTO,
        existingKayttajaDTO: KayttajaDTO
    ): Boolean {
        val givenYliopistotAndErikoisalat = kayttajahallintaKayttajaDTO.yliopistotAndErikoisalat
        val persistedYliopistotAndErikoisalat = existingKayttajaDTO.yliopistotAndErikoisalat
        requireNotNull(persistedYliopistotAndErikoisalat)

        val newErikoisalat =
            givenYliopistotAndErikoisalat.filter { removedKayttajaYliopistoErikoisalaDTO ->
                removedKayttajaYliopistoErikoisalaDTO.id !in (existingKayttajaDTO.yliopistotAndErikoisalat?.map { it.id }
                    ?: setOf())
            }.toSet()

        val removedErikoisalat =
            persistedYliopistotAndErikoisalat.filter { kayttajaYliopistoErikoisalaDTO ->
                kayttajaYliopistoErikoisalaDTO.id !in (givenYliopistotAndErikoisalat.map { it.id })
            }.toSet()

        val retainedErikoisalat = givenYliopistotAndErikoisalat.minus(newErikoisalat).minus(removedErikoisalat)

        return validateNewErikoisalat(newErikoisalat, kayttajahallintaKayttajaDTO.reassignedTehtavat) &&
            validateRemovedErikoisalat(removedErikoisalat, kayttajahallintaKayttajaDTO.reassignedTehtavat) &&
            validateErikoisalaTehtavat(
                retainedErikoisalat,
                persistedYliopistotAndErikoisalat,
                kayttajahallintaKayttajaDTO.reassignedTehtavat
            )
    }

    @Transactional
    override fun validateVirkailijaIsAllowedToCreateKayttajaByYliopistoId(
        virkailijaUserDTO: UserDTO,
        yliopistoId: Long
    ): Boolean {
        val virkailijaKayttaja = findKayttajaByUserId(virkailijaUserDTO.id!!)
        val virkailijaYliopisto = getVirkailijaYliopisto(virkailijaKayttaja)
        return virkailijaYliopisto.id == yliopistoId
    }

    @Transactional
    override fun validateVirkailijaIsAllowedToManageErikoistuvaLaakari(
        virkailijaUserDTO: UserDTO,
        kayttajaId: Long
    ): Boolean {
        val virkailijaKayttaja = findKayttajaByUserId(virkailijaUserDTO.id!!)
        val virkailijaYliopisto = getVirkailijaYliopisto(virkailijaKayttaja)
        val erikoistuvaLaakari = findErikoistuvaLaakariByKayttajaId(kayttajaId)
        return erikoistuvaLaakari.opintooikeudet.map { it.yliopisto?.id }.contains(virkailijaYliopisto.id)
    }

    @Transactional
    override fun validateVirkailijaIsAllowedToManageKayttaja(virkailijaUserDTO: UserDTO, kayttajaId: Long): Boolean {
        val kayttaja = findKayttajaById(kayttajaId)
        if (kayttaja.user?.authorities?.contains(Authority(TEKNINEN_PAAKAYTTAJA)) == true) {
            return false
        }

        val virkailijaKayttaja = findKayttajaByUserId(virkailijaUserDTO.id!!)
        val virkailijaYliopisto = getVirkailijaYliopisto(virkailijaKayttaja)
        val kayttajaYliopistot = kayttaja.yliopistot
        val kayttajaYliopistotAndErikoisalat = kayttaja.yliopistotAndErikoisalat
        val yliopistotContains = kayttajaYliopistot.map { it.id }.contains(virkailijaYliopisto.id)
        val yliopistotAndErikoisalatContains =
            kayttajaYliopistotAndErikoisalat.map { it.yliopisto?.id }.contains(virkailijaYliopisto.id)
        return yliopistotContains || yliopistotAndErikoisalatContains
    }

    private fun validateNewErikoisalat(
        newErikoisalat: Set<KayttajaYliopistoErikoisalaDTO>,
        reassignedTehtavat: Set<ReassignedVastuuhenkilonTehtavaDTO>
    ): Boolean {
        var isValid = true
        newErikoisalat.forEach yliopistotErikoisalat@{ newErikoisalaDTO ->
            val assignedTehtavatForErikoisala = getAssignedTehtavatForErikoisala(newErikoisalaDTO, newErikoisalaDTO.id)
            // Mikään vastuuhenkilölle määritellyistä tehtävistä ei saa löytyä toiselta vastuuhenkilöltä saman
            // erikoisalan sisällä.
            newErikoisalaDTO.vastuuhenkilonTehtavat.forEach {
                if (persistedTehtavatContains(assignedTehtavatForErikoisala, it.id) && !isReassignedTehtava(
                        reassignedTehtavat,
                        newErikoisalaDTO.erikoisala?.id!!,
                        it.id,
                        ReassignedVastuuhenkilonTehtavaTyyppi.REMOVE
                    )
                ) {
                    isValid = false
                    return@yliopistotErikoisalat
                }
            }
        }
        return isValid
    }

    private fun validateRemovedErikoisalat(
        removedErikoisalat: Set<KayttajaYliopistoErikoisalaDTO>,
        reassignedTehtavat: Set<ReassignedVastuuhenkilonTehtavaDTO>
    ): Boolean {
        var isValid = true
        removedErikoisalat.forEach yliopistotErikoisalat@{ removedErikoisalaDTO ->
            val assignedTehtavatForErikoisala =
                getAssignedTehtavatForErikoisala(removedErikoisalaDTO, removedErikoisalaDTO.id)
            // Kaikki poistetun erikoisalan tehtävät täytyy löytyä joltain saman erikoisalan vastuuhenkilöltä.
            removedErikoisalaDTO.vastuuhenkilonTehtavat.forEach {
                if (!persistedTehtavatContains(assignedTehtavatForErikoisala, it.id) && !isReassignedTehtava(
                        reassignedTehtavat,
                        removedErikoisalaDTO.id!!,
                        it.id,
                        ReassignedVastuuhenkilonTehtavaTyyppi.ADD
                    )
                ) {
                    isValid = false
                    return@yliopistotErikoisalat
                }
            }
        }
        return isValid
    }

    private fun validateErikoisalaTehtavat(
        erikoisalat: Set<KayttajaYliopistoErikoisalaDTO>,
        persistedErikoisalat: Set<KayttajaYliopistoErikoisalaDTO>,
        reassignedTehtavat: Set<ReassignedVastuuhenkilonTehtavaDTO>
    ): Boolean {
        var isValid = true
        erikoisalat.forEach yliopistotErikoisalat@{ kayttajaYliopistoErikoisalaDTO ->
            val allTehtavatForErikoisala = getAllTehtavatForErikoisala(kayttajaYliopistoErikoisalaDTO.erikoisala?.id!!)
            val assignedTehtavatForErikoisala =
                getAssignedTehtavatForErikoisala(kayttajaYliopistoErikoisalaDTO, kayttajaYliopistoErikoisalaDTO.id)
            val persistedErikoisalaTehtavat =
                persistedErikoisalat.find { it.id == kayttajaYliopistoErikoisalaDTO.id }?.vastuuhenkilonTehtavat
            allTehtavatForErikoisala.forEach {
                // Uusi tehtävä
                if (tehtavatContains(kayttajaYliopistoErikoisalaDTO.vastuuhenkilonTehtavat, it.id) &&
                    !tehtavatContains(persistedErikoisalaTehtavat, it.id)
                ) {
                    if (assignedTehtavatForErikoisala.contains(it) && !isReassignedTehtava(
                            reassignedTehtavat,
                            kayttajaYliopistoErikoisalaDTO.erikoisala?.id!!,
                            it.id,
                            ReassignedVastuuhenkilonTehtavaTyyppi.REMOVE
                        )
                    ) {
                        isValid = false
                        return@yliopistotErikoisalat
                    }
                }
                // Poistettu tehtävä
                else if (!tehtavatContains(kayttajaYliopistoErikoisalaDTO.vastuuhenkilonTehtavat, it.id) &&
                    tehtavatContains(persistedErikoisalaTehtavat, it.id)
                ) {
                    if (!assignedTehtavatForErikoisala.contains(it) && !isReassignedTehtava(
                            reassignedTehtavat,
                            kayttajaYliopistoErikoisalaDTO.erikoisala?.id!!,
                            it.id,
                            ReassignedVastuuhenkilonTehtavaTyyppi.ADD
                    )) {
                        isValid = false
                        return@yliopistotErikoisalat
                    }
                }
            }
        }
        return isValid
    }

    private fun persistedTehtavatContains(tehtavat: Set<VastuuhenkilonTehtavatyyppi>?, tehtavaId: Long?): Boolean =
        tehtavat?.map { it.id }?.contains(tehtavaId) == true

    private fun tehtavatContains(tehtavat: Set<VastuuhenkilonTehtavatyyppiDTO>?, tehtavaId: Long?): Boolean =
        tehtavat?.map { it.id }?.contains(tehtavaId) == true

    private fun isReassignedTehtava(
        reassignedTehtavat: Set<ReassignedVastuuhenkilonTehtavaDTO>,
        erikoisalaId: Long,
        tehtavaId: Long?,
        reassignedTyyppi: ReassignedVastuuhenkilonTehtavaTyyppi
    ): Boolean {
        return reassignedTehtavat.find {
            it.kayttajaYliopistoErikoisala?.erikoisala?.id == erikoisalaId &&
                it.tehtavaId == tehtavaId &&
                it.tyyppi == reassignedTyyppi
        } != null
    }

    private fun getAllTehtavatForErikoisala(erikoisalaId: Long): Set<VastuuhenkilonTehtavatyyppi> =
        erikoisalaRepository.findById(erikoisalaId)
            .orElseThrow { EntityNotFoundException("Erikoisalaa ei löydy") }.vastuuhenkilonTehtavatyypit?.toSet()
            ?: setOf()

    private fun getAssignedTehtavatForErikoisala(
        kayttajaYliopistoErikoisalaDTO: KayttajaYliopistoErikoisalaDTO,
        excludedId: Long? = null
    ): Set<VastuuhenkilonTehtavatyyppi> {
        var kayttajaYliopistoErikoisalatList = kayttajaYliopistoErikoisalaRepository.findAllByYliopistoIdAndErikoisalaId(
            kayttajaYliopistoErikoisalaDTO.yliopisto?.id!!,
            kayttajaYliopistoErikoisalaDTO.erikoisala?.id!!
        )
        excludedId?.let {
            kayttajaYliopistoErikoisalatList = kayttajaYliopistoErikoisalatList.filter { it.id != excludedId }
        }
        return kayttajaYliopistoErikoisalatList.map { it.vastuuhenkilonTehtavat }.flatten().toSet()
    }

    private fun getVirkailijaYliopisto(virkailijaKayttaja: Kayttaja) =
        virkailijaKayttaja.yliopistot.firstOrNull() ?: throw EntityNotFoundException(VIRKAILIJA_YLIOPISTO_NOT_FOUND_MSG)

    private fun findErikoistuvaLaakariByKayttajaId(kayttajaId: Long): ErikoistuvaLaakari {
        return erikoistuvaLaakariRepository.findOneByKayttajaId(kayttajaId) ?: throw EntityNotFoundException(
            ERIKOISTUVA_LAAKARI_NOT_FOUND_ERROR
        )
    }

    private fun findKayttajaByUserId(userId: String): Kayttaja {
        return kayttajaRepository.findOneByUserId(userId)
            .orElseThrow { EntityNotFoundException(KAYTTAJA_NOT_FOUND_ERROR) }
    }

    private fun findKayttajaById(id: Long): Kayttaja {
        return kayttajaRepository.findById(id).orElseThrow { EntityNotFoundException(KAYTTAJA_NOT_FOUND_ERROR) }
    }
}
