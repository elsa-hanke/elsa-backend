package fi.elsapalvelu.elsa.service.impl.valmistuminen

import fi.elsapalvelu.elsa.domain.koulutus.OpintosuoritusTyyppiEnum
import fi.elsapalvelu.elsa.domain.koulutus.KaytannonKoulutusTyyppi
import fi.elsapalvelu.elsa.domain.perustiedot.ErikoisalaTyyppi
import fi.elsapalvelu.elsa.repository.koulutus.OpintosuoritusRepository
import fi.elsapalvelu.elsa.repository.tyoskentely.TyoskentelyjaksoRepository
import fi.elsapalvelu.elsa.service.dto.suoritteet.VanhentuneetSuorituksetDTO
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate

private const val KUULUSTELUN_VOIMASSAOLO_VUODET = 4L
private const val LAAKETIETEEN_KOULUTUKSEN_VOIMASSAOLO_VUODET = 10L
private const val HAMMASLAAKETIETEEN_KOULUTUKSEN_VOIMASSAOLO_VUODET = 6L

@Service
class ValmistumispyynnonVanhentumisService(
    private val tyoskentelyjaksoRepository: TyoskentelyjaksoRepository,
    private val opintosuoritusRepository: OpintosuoritusRepository,
    private val clock: Clock
) {

    @Transactional(readOnly = true)
    fun haeVanhentuneetSuoritukset(
        opintooikeusId: Long,
        erikoisalaTyyppi: ErikoisalaTyyppi
    ): VanhentuneetSuorituksetDTO {
        val voimassaoloVuodet = when (erikoisalaTyyppi) {
            ErikoisalaTyyppi.LAAKETIEDE -> LAAKETIETEEN_KOULUTUKSEN_VOIMASSAOLO_VUODET
            else -> HAMMASLAAKETIETEEN_KOULUTUKSEN_VOIMASSAOLO_VUODET
        }
        val vanhentumispaiva = LocalDate.now(clock).minusYears(voimassaoloVuodet)
        val onkoVanhentuneitaTyoskentelyjaksoja = tyoskentelyjaksoRepository
            .findAllByOpintooikeusId(opintooikeusId)
            .asSequence()
            .filter { it.kaytannonKoulutus != KaytannonKoulutusTyyppi.TERVEYSKESKUSTYO }
            .any { it.alkamispaiva?.isBefore(vanhentumispaiva) == true }

        val opintosuoritukset = opintosuoritusRepository.findAllByOpintooikeusId(opintooikeusId)
        val onkoVanhentuneitaSuorituksia = opintosuoritukset
            .asSequence()
            .filter { it.tyyppi?.nimi != OpintosuoritusTyyppiEnum.VALTAKUNNALLINEN_KUULUSTELU }
            .any { it.suorituspaiva?.isBefore(vanhentumispaiva) == true }
        val kuulustelunVanhentumispaiva =
            LocalDate.now(clock).minusYears(KUULUSTELUN_VOIMASSAOLO_VUODET)
        val onkoKuulusteluVanhentunut = opintosuoritukset
            .asSequence()
            .filter { it.tyyppi?.nimi == OpintosuoritusTyyppiEnum.VALTAKUNNALLINEN_KUULUSTELU }
            .any { it.suorituspaiva?.isBefore(kuulustelunVanhentumispaiva) == true }

        return VanhentuneetSuorituksetDTO(
            vanhojaTyoskentelyjaksojaOrSuorituksiaExists =
                onkoVanhentuneitaTyoskentelyjaksoja || onkoVanhentuneitaSuorituksia,
            kuulusteluVanhentunut = onkoKuulusteluVanhentunut
        )
    }
}
