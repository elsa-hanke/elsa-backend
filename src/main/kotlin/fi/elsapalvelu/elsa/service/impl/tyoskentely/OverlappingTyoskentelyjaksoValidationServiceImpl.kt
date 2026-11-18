package fi.elsapalvelu.elsa.service.impl.tyoskentely

import fi.elsapalvelu.elsa.required

import java.time.Clock
import java.time.LocalDate
import fi.elsapalvelu.elsa.domain.tyoskentely.PoissaolonSyy
import fi.elsapalvelu.elsa.domain.tyoskentely.Keskeytysaika
import fi.elsapalvelu.elsa.domain.tyoskentely.Tyoskentelyjakso
import fi.elsapalvelu.elsa.repository.tyoskentely.KeskeytysaikaRepository
import fi.elsapalvelu.elsa.repository.tyoskentely.TyoskentelyjaksoRepository
import fi.elsapalvelu.elsa.service.tyoskentely.OverlappingTyoskentelyjaksoValidationService
import fi.elsapalvelu.elsa.service.tyoskentely.TyoskentelyajanPaallekkaisyystarkistus
import fi.elsapalvelu.elsa.service.dto.tyoskentely.KeskeytysaikaDTO
import fi.elsapalvelu.elsa.service.dto.tyoskentely.TyoskentelyjaksoDTO
import org.springframework.transaction.annotation.Transactional
import org.springframework.stereotype.Service

@Service
@Transactional(readOnly = true)
class OverlappingTyoskentelyjaksoValidationServiceImpl(
    private val tyoskentelyjaksoRepository: TyoskentelyjaksoRepository,
    private val keskeytysaikaRepository: KeskeytysaikaRepository,
    private val paallekkaisyystarkistus: TyoskentelyajanPaallekkaisyystarkistus,
    private val clock: Clock
) : OverlappingTyoskentelyjaksoValidationService {

    override fun validateTyoskentelyjakso(opintooikeusId: Long, tyoskentelyjaksoDTO: TyoskentelyjaksoDTO): Boolean {
        val tarkistuksenPaattymispaiva = tyoskentelyjaksoDTO.paattymispaiva ?: LocalDate.now(clock)
        val tyoskentelyjaksot = haeTarkistusjaksot(opintooikeusId, tarkistuksenPaattymispaiva)

        if (tyoskentelyjaksot.isEmpty()) return true

        sovellaJaksonMuutokset(tyoskentelyjaksoDTO, tyoskentelyjaksot)

        val alkamispaiva = tyoskentelyjaksoDTO.alkamispaiva.required()
        val osaaikaosuus = tyoskentelyjaksoDTO.osaaikaprosentti.required().toDouble() / 100.0
        return paallekkaisyystarkistus.onkoTyoskentelyaikaSallittu(
            tyoskentelyjaksot,
            alkamispaiva,
            tarkistuksenPaattymispaiva,
            uudenJaksonOsuus = osaaikaosuus.takeIf { tyoskentelyjaksoDTO.id == null }
        )
    }

    override fun validateKeskeytysaika(
        opintooikeusId: Long,
        keskeytysaikaDTO: KeskeytysaikaDTO
    ): Boolean {
        val tarkistuksenPaattymispaiva =
            keskeytysaikaDTO.tyoskentelyjakso?.paattymispaiva
                ?: LocalDate.now(clock)
        val tyoskentelyjaksot = haeTarkistusjaksot(opintooikeusId, tarkistuksenPaattymispaiva)

        if (tyoskentelyjaksot.size == 1) return true

        sovellaKeskeytysajanMuutokset(keskeytysaikaDTO, tyoskentelyjaksot)

        return paallekkaisyystarkistus.onkoTyoskentelyaikaSallittu(
            tyoskentelyjaksot,
            keskeytysaikaDTO.tyoskentelyjakso?.alkamispaiva.required(),
            tarkistuksenPaattymispaiva
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
        val tarkistuksenPaattymispaiva =
            keskeytysaika.tyoskentelyjakso?.paattymispaiva ?: LocalDate.now(clock)
        val tyoskentelyjaksot = haeTarkistusjaksot(opintooikeusId, tarkistuksenPaattymispaiva)

        if (tyoskentelyjaksot.size == 1) return true

        poistaKeskeytysaikaTarkistuksesta(
            keskeytysaika.id.required(),
            tyoskentelyjaksoId,
            tyoskentelyjaksot
        )

        return paallekkaisyystarkistus.onkoTyoskentelyaikaSallittu(
            tyoskentelyjaksot,
            keskeytysaika.tyoskentelyjakso?.alkamispaiva.required(),
            tarkistuksenPaattymispaiva
        )
    }

    private fun haeTarkistusjaksot(opintooikeusId: Long, paattymispaiva: LocalDate): List<Tyoskentelyjakso> =
        tyoskentelyjaksoRepository.findAllByOpintooikeusUntilDateEagerWithRelationships(opintooikeusId, paattymispaiva)
            .map(::kopioiTarkistustaVarten)

    // Tarkistus voi liittyä kirjoittavaan transaktioon. Ehdotetut muutokset tehdään vain kopioihin,
    // jotta dirty checking ja orphanRemoval eivät muuta tietokannan jaksoja tai keskeytyksiä.
    private fun kopioiTarkistustaVarten(alkuperainen: Tyoskentelyjakso): Tyoskentelyjakso {
        val kopio = Tyoskentelyjakso(
            id = alkuperainen.id,
            alkamispaiva = alkuperainen.alkamispaiva,
            paattymispaiva = alkuperainen.paattymispaiva,
            osaaikaprosentti = alkuperainen.osaaikaprosentti,
            suoritusarvioinnit = alkuperainen.suoritusarvioinnit.toMutableSet(),
            suoritemerkinnat = alkuperainen.suoritemerkinnat.toMutableSet()
        )
        kopio.keskeytykset = alkuperainen.keskeytykset.map { keskeytysaika ->
            Keskeytysaika(
                id = keskeytysaika.id,
                alkamispaiva = keskeytysaika.alkamispaiva,
                paattymispaiva = keskeytysaika.paattymispaiva,
                poissaoloprosentti = keskeytysaika.poissaoloprosentti,
                poissaolonSyy = keskeytysaika.poissaolonSyy,
                tyoskentelyjakso = kopio
            )
        }.toMutableSet()
        return kopio
    }

    private fun sovellaJaksonMuutokset(
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

    private fun sovellaKeskeytysajanMuutokset(
        keskeytysaikaDTO: KeskeytysaikaDTO,
        tyoskentelyjaksot: List<Tyoskentelyjakso>
    ) {
        val muokattavaJakso =
            haeTyoskentelyjakso(keskeytysaikaDTO.tyoskentelyjaksoId.required(), tyoskentelyjaksot)

        muokattavaJakso?.keskeytykset?.find {
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

    private fun poistaKeskeytysaikaTarkistuksesta(
        keskeytysaikaId: Long,
        tyoskentelyjaksoId: Long,
        tyoskentelyjaksot: List<Tyoskentelyjakso>
    ) {
        val muokattavaJakso =
            haeTyoskentelyjakso(tyoskentelyjaksoId, tyoskentelyjaksot)
        muokattavaJakso?.keskeytykset?.removeIf {
            it.id == keskeytysaikaId
        }
    }

    private fun haeTyoskentelyjakso(
        id: Long,
        tyoskentelyjaksot: List<Tyoskentelyjakso>
    ): Tyoskentelyjakso? {
        return tyoskentelyjaksot.find {
            it.id == id
        }
    }
}
