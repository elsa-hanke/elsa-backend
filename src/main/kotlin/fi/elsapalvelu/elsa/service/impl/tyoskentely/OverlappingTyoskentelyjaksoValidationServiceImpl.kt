package fi.elsapalvelu.elsa.service.impl.tyoskentely

import fi.elsapalvelu.elsa.required

import java.time.LocalDate
import fi.elsapalvelu.elsa.domain.tyoskentely.PoissaolonSyy
import fi.elsapalvelu.elsa.domain.tyoskentely.Keskeytysaika
import fi.elsapalvelu.elsa.domain.tyoskentely.Tyoskentelyjakso
import fi.elsapalvelu.elsa.domain.tyoskentely.PoissaolonSyyTyyppi
import fi.elsapalvelu.elsa.extensions.isInRange
import fi.elsapalvelu.elsa.repository.tyoskentely.KeskeytysaikaRepository
import fi.elsapalvelu.elsa.repository.tyoskentely.TyoskentelyjaksoRepository
import fi.elsapalvelu.elsa.service.tyoskentely.OverlappingTyoskentelyjaksoValidationService
import fi.elsapalvelu.elsa.service.tyoskentely.TyoskentelyjaksonPituusCounterService
import fi.elsapalvelu.elsa.service.tyoskentely.PoissaolonHyvaksilukulaskuri
import fi.elsapalvelu.elsa.service.dto.tyoskentely.HyvaksiluettavatCounterData
import fi.elsapalvelu.elsa.service.dto.tyoskentely.KeskeytysaikaDTO
import fi.elsapalvelu.elsa.service.dto.tyoskentely.TyoskentelyjaksoDTO
import org.springframework.transaction.annotation.Transactional
import org.springframework.stereotype.Service
import java.time.ZoneId

@Service
@Transactional(readOnly = true)
class OverlappingTyoskentelyjaksoValidationServiceImpl(
    private val tyoskentelyjaksoRepository: TyoskentelyjaksoRepository,
    private val keskeytysaikaRepository: KeskeytysaikaRepository,
    private val tyoskentelyjaksonPituusCounterService: TyoskentelyjaksonPituusCounterService
) : OverlappingTyoskentelyjaksoValidationService {

    override fun validateTyoskentelyjakso(opintooikeusId: Long, tyoskentelyjaksoDTO: TyoskentelyjaksoDTO): Boolean {
        val tyoskentelyjaksoEndDate = tyoskentelyjaksoDTO.paattymispaiva ?: LocalDate.now(ZoneId.systemDefault())
        val tyoskentelyjaksot = tyoskentelyjaksoRepository.findAllByOpintooikeusUntilDateEagerWithRelationships(opintooikeusId, tyoskentelyjaksoEndDate)
            .map(::copyForValidation)

        if (tyoskentelyjaksot.isEmpty()) return true

        updateExistingTyoskentelyjaksoIfExists(tyoskentelyjaksoDTO, tyoskentelyjaksot)

        return validateTyoskentelyaika(
            tyoskentelyjaksoDTO.id,
            tyoskentelyjaksoDTO.alkamispaiva.required(),
            tyoskentelyjaksoEndDate,
            tyoskentelyjaksot,
            tyoskentelyjaksoDTO.osaaikaprosentti.required().toDouble()
        )
    }

    override fun validateKeskeytysaika(
        opintooikeusId: Long,
        keskeytysaikaDTO: KeskeytysaikaDTO
    ): Boolean {
        val tyoskentelyjaksoEndDate =
            keskeytysaikaDTO.tyoskentelyjakso?.paattymispaiva
                ?: LocalDate.now(ZoneId.systemDefault())
        val tyoskentelyjaksot =
            tyoskentelyjaksoRepository.findAllByOpintooikeusUntilDateEagerWithRelationships(
                opintooikeusId,
                tyoskentelyjaksoEndDate
            ).map(::copyForValidation)

        if (tyoskentelyjaksot.size == 1) return true

        updateExistingTyoskentelyjaksoKeskeytysaika(keskeytysaikaDTO, tyoskentelyjaksot)

        return validateTyoskentelyaika(
            keskeytysaikaDTO.tyoskentelyjaksoId,
            keskeytysaikaDTO.tyoskentelyjakso?.alkamispaiva.required(),
            tyoskentelyjaksoEndDate,
            tyoskentelyjaksot
        )
    }

    override fun validateKeskeytysaikaDelete(
        opintooikeusId: Long,
        keskeytysaikaId: Long
    ): Boolean {
        val keskeytysaika =
            keskeytysaikaRepository.findOneByIdAndTyoskentelyjaksoOpintooikeusId(
                keskeytysaikaId,
                opintooikeusId
            ) ?: return false

        val tyoskentelyjaksoId = keskeytysaika.tyoskentelyjakso?.id.required()
        val tyoskentelyjaksoEndDate =
            keskeytysaika.tyoskentelyjakso?.paattymispaiva ?: LocalDate.now(ZoneId.systemDefault())
        val tyoskentelyjaksot =
            tyoskentelyjaksoRepository.findAllByOpintooikeusUntilDateEagerWithRelationships(
                opintooikeusId,
                tyoskentelyjaksoEndDate
            ).map(::copyForValidation)

        if (tyoskentelyjaksot.size == 1) return true

        removeKeskeytysaikaFromTyoskententelyjakso(
            keskeytysaika.id.required(),
            tyoskentelyjaksoId,
            tyoskentelyjaksot
        )

        return validateTyoskentelyaika(
            tyoskentelyjaksoId,
            keskeytysaika.tyoskentelyjakso?.alkamispaiva.required(),
            tyoskentelyjaksoEndDate,
            tyoskentelyjaksot
        )
    }

    @Suppress("LoopWithTooManyJumpStatements")
    private fun validateTyoskentelyaika(existingTyoskentelyjaksoId: Long?, tyoskentelyjaksoStartDate: LocalDate, tyoskentelyjaksoEndDate: LocalDate,
        tyoskentelyjaksot: List<Tyoskentelyjakso>, osaaikaProsentti: Double? = null): Boolean {
        var hyvaksiluettavatCounterData: HyvaksiluettavatCounterData? = null
        fun getHyvaksiluettavatCounterData(
            counterTyoskentelyjaksot: List<Tyoskentelyjakso>,
            calculateUntilDate: LocalDate
        ): HyvaksiluettavatCounterData {
            if (hyvaksiluettavatCounterData == null) {
                hyvaksiluettavatCounterData = tyoskentelyjaksonPituusCounterService.calculateHyvaksiluettavatDaysLeft(counterTyoskentelyjaksot, calculateUntilDate)
            }
            return hyvaksiluettavatCounterData
        }

        // Mikäli työskentelyjakso sijoittuu tulevaisuuteen eikä sille ole määritelty päättymispäivää
        // ei validointia tehdä.
        if (tyoskentelyjaksoEndDate < tyoskentelyjaksoStartDate) return true

        dates@ for (date in tyoskentelyjaksoStartDate.datesUntil(tyoskentelyjaksoEndDate.plusDays(1))) {
            val overlappingTyoskentelyjaksotForCurrentDate = tyoskentelyjaksot.filter { date.isInRange(it.alkamispaiva.required(), it.paattymispaiva) }
            if (overlappingTyoskentelyjaksotForCurrentDate.isEmpty()) continue@dates

            var overallTyoskentelyaikaFactorForCurrentDate = overlappingTyoskentelyjaksotForCurrentDate.sumOf { it.osaaikaprosentti.required().toDouble() / 100.0 }

            // Jos kyseessä uusi työskentelyjakso, lisätään työskentelyaika päiväkohtaiseen kertymään.
            if (existingTyoskentelyjaksoId == null && osaaikaProsentti != null)
                overallTyoskentelyaikaFactorForCurrentDate += osaaikaProsentti / 100.0

            // Työaika ei ylitä 100% kyseisen päivän osalta -> ei tarvetta tarkastella poissaoloja.
            if (overallTyoskentelyaikaFactorForCurrentDate <= 1) continue@dates

            tyoskentelyjaksot@ for (tyoskentelyjakso in overlappingTyoskentelyjaksotForCurrentDate) {
                val keskeytyksetForCurrentDate =
                    tyoskentelyjakso.keskeytykset.filter { keskeytysaika ->
                        date.isInRange(keskeytysaika.alkamispaiva.required(), keskeytysaika.paattymispaiva)
                    }

                keskeytyksetForCurrentDate.forEach { keskeytysaika ->
                    overallTyoskentelyaikaFactorForCurrentDate -= laskePoissaolonVahennys(keskeytysaika, tyoskentelyjakso, date) {
                        getHyvaksiluettavatCounterData(tyoskentelyjaksot, date.minusDays(1))
                    }
                }
            }

            if (overallTyoskentelyaikaFactorForCurrentDate > 1) {
                return false
            }
        }
        return true
    }

    private fun laskePoissaolonVahennys(
        keskeytysaika: Keskeytysaika,
        tyoskentelyjakso: Tyoskentelyjakso,
        paiva: LocalDate,
        haeHyvaksiluettavienPaivienSaldo: () -> HyvaksiluettavatCounterData
    ): Double {
        val poissaolopaivat = keskeytysaika.poissaoloprosentti.required().toDouble() / 100.0 * (tyoskentelyjakso.osaaikaprosentti.required().toDouble() / 100.0)
        val poissaolonSyy = keskeytysaika.poissaolonSyy.required()
        return when (poissaolonSyy.vahennystyyppi) {
            PoissaolonSyyTyyppi.VAHENNETAAN_SUORAAN -> poissaolopaivat
            PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI -> PoissaolonHyvaksilukulaskuri.laskePaivittaisestaTyoskentelyajastaVahennettavaOsuus(
                poissaolopaivat, paiva.year, poissaolonSyy, haeHyvaksiluettavienPaivienSaldo()
            )
            else -> 0.0
        }
    }

    // Never apply hypothetical changes to managed entities: validation can join a write transaction.
    // Absences need separate objects and a separate collection to avoid dirty checking and orphan removal.
    private fun copyForValidation(source: Tyoskentelyjakso): Tyoskentelyjakso {
        val copy = Tyoskentelyjakso(
            id = source.id,
            alkamispaiva = source.alkamispaiva,
            paattymispaiva = source.paattymispaiva,
            osaaikaprosentti = source.osaaikaprosentti,
            suoritusarvioinnit = source.suoritusarvioinnit.toMutableSet(),
            suoritemerkinnat = source.suoritemerkinnat.toMutableSet()
        )
        copy.keskeytykset = source.keskeytykset.map { keskeytysaika ->
            Keskeytysaika(
                id = keskeytysaika.id,
                alkamispaiva = keskeytysaika.alkamispaiva,
                paattymispaiva = keskeytysaika.paattymispaiva,
                poissaoloprosentti = keskeytysaika.poissaoloprosentti,
                poissaolonSyy = keskeytysaika.poissaolonSyy,
                tyoskentelyjakso = copy
            )
        }.toMutableSet()
        return copy
    }

    private fun updateExistingTyoskentelyjaksoIfExists(
        tyoskentelyjaksoDTO: TyoskentelyjaksoDTO,
        tyoskentelyjaksot: List<Tyoskentelyjakso>
    ) {
        // Jos ollaan päivittämässä olemassaolevaa jaksoa, haetaan työskentelyjakso id:n perusteella ja päivitetään
        // tiedot suoraan siihen.
        if (tyoskentelyjaksoDTO.id != null) {
            tyoskentelyjaksot.find { it.id == tyoskentelyjaksoDTO.id }?.apply {
                if (this.hasTapahtumia()) {
                    paattymispaiva = tyoskentelyjaksoDTO.paattymispaiva
                } else {
                    alkamispaiva = tyoskentelyjaksoDTO.alkamispaiva
                    paattymispaiva = tyoskentelyjaksoDTO.paattymispaiva
                    osaaikaprosentti = tyoskentelyjaksoDTO.osaaikaprosentti
                }
            }
        }
    }

    private fun updateExistingTyoskentelyjaksoKeskeytysaika(
        keskeytysaikaDTO: KeskeytysaikaDTO,
        tyoskentelyjaksot: List<Tyoskentelyjakso>
    ) {
        val tyoskentelyjaksoWithUpdatedKeskeytysaika =
            findTyoskentelyjakso(keskeytysaikaDTO.tyoskentelyjaksoId.required(), tyoskentelyjaksot)

        tyoskentelyjaksoWithUpdatedKeskeytysaika?.keskeytykset?.find {
            it.id == keskeytysaikaDTO.id
        }?.apply {
            alkamispaiva = keskeytysaikaDTO.alkamispaiva
            paattymispaiva = keskeytysaikaDTO.paattymispaiva
            poissaoloprosentti = keskeytysaikaDTO.poissaoloprosentti
            keskeytysaikaDTO.poissaolonSyy?.let {
                poissaolonSyy = PoissaolonSyy(
                    id = it.id,
                    nimi = it.nimi,
                    vahennystyyppi = it.vahennystyyppi,
                    vahennetaanKerran = it.vahennetaanKerran,
                    voimassaolonAlkamispaiva = it.voimassaolonAlkamispaiva,
                    voimassaolonPaattymispaiva = it.voimassaolonPaattymispaiva
                )
            }
        }
    }

    private fun removeKeskeytysaikaFromTyoskententelyjakso(
        keskeytysaikaId: Long,
        tyoskentelyjaksoId: Long,
        tyoskentelyjaksot: List<Tyoskentelyjakso>
    ) {
        val tyoskentelyjaksoWithRemovedKeskeytysaika =
            findTyoskentelyjakso(tyoskentelyjaksoId, tyoskentelyjaksot)
        tyoskentelyjaksoWithRemovedKeskeytysaika?.keskeytykset?.removeIf {
            it.id == keskeytysaikaId
        }
    }

    private fun findTyoskentelyjakso(
        id: Long,
        tyoskentelyjaksot: List<Tyoskentelyjakso>
    ): Tyoskentelyjakso? {
        return tyoskentelyjaksot.find {
            it.id == id
        }
    }
}
