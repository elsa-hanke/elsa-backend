package fi.elsapalvelu.elsa.service.kayttaja.etusivu

import fi.elsapalvelu.elsa.domain.kayttaja.AvoinAsiaTyyppiEnum
import fi.elsapalvelu.elsa.extensions.pattern
import fi.elsapalvelu.elsa.repository.koejakso.KoejaksonAloituskeskusteluRepository
import fi.elsapalvelu.elsa.repository.koejakso.KoejaksonKoulutussopimusRepository
import fi.elsapalvelu.elsa.repository.koejakso.KoejaksonVastuuhenkilonArvioRepository
import fi.elsapalvelu.elsa.repository.seuranta.SeurantajaksoRepository
import fi.elsapalvelu.elsa.repository.valmistuminen.ValmistumispyyntoRepository
import fi.elsapalvelu.elsa.service.dto.kayttaja.AvoinAsiaDTO
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.springframework.context.MessageSource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class EtusivunPalautuspalvelu(
    private val koejaksonKoulutussopimusRepository: KoejaksonKoulutussopimusRepository,
    private val koejaksonAloituskeskusteluRepository: KoejaksonAloituskeskusteluRepository,
    private val koejaksonVastuuhenkilonArvioRepository: KoejaksonVastuuhenkilonArvioRepository,
    private val seurantajaksoRepository: SeurantajaksoRepository,
    private val valmistumispyyntoRepository: ValmistumispyyntoRepository,
    private val messageSource: MessageSource
) {

    fun lisaaKoejaksonJaSeurantajaksojenPalautukset(avoimetAsiat: MutableList<AvoinAsiaDTO>, opintooikeusId: Long, kielialue: Locale) {
        lisaaKoulutussopimuksenPalautus(avoimetAsiat, opintooikeusId, kielialue)
        lisaaAloituskeskustelunPalautus(avoimetAsiat, opintooikeusId, kielialue)
        lisaaVastuuhenkilonArvionPalautus(avoimetAsiat, opintooikeusId, kielialue)
        lisaaSeurantajaksojenPalautukset(avoimetAsiat, opintooikeusId, kielialue)
    }

    private fun lisaaKoulutussopimuksenPalautus(avoimetAsiat: MutableList<AvoinAsiaDTO>, opintooikeusId: Long, kielialue: Locale) {
        koejaksonKoulutussopimusRepository.findByOpintooikeusId(opintooikeusId).ifPresent {
            if (!it.korjausehdotus.isNullOrBlank()) {
                avoimetAsiat.add(
                    AvoinAsiaDTO(
                        tyyppi = AvoinAsiaTyyppiEnum.KOULUTUSSOPIMUS,
                        asia = messageSource.getMessage(
                            "avoimetasiat.koulutussopimus",
                            null,
                            kielialue
                        ),
                        pvm = it.muokkauspaiva
                    )
                )
            }
        }
    }

    private fun lisaaAloituskeskustelunPalautus(avoimetAsiat: MutableList<AvoinAsiaDTO>, opintooikeusId: Long, kielialue: Locale) {
        koejaksonAloituskeskusteluRepository.findByOpintooikeusId(opintooikeusId).ifPresent {
            if (!it.korjausehdotus.isNullOrBlank()) {
                avoimetAsiat.add(
                    AvoinAsiaDTO(
                        tyyppi = AvoinAsiaTyyppiEnum.ALOITUSKESKUSTELU,
                        asia = messageSource.getMessage(
                            "avoimetasiat.aloituskeskustelu",
                            null,
                            kielialue
                        ),
                        pvm = it.muokkauspaiva
                    )
                )
            }
        }
    }

    private fun lisaaVastuuhenkilonArvionPalautus(avoimetAsiat: MutableList<AvoinAsiaDTO>, opintooikeusId: Long, kielialue: Locale) {
        koejaksonVastuuhenkilonArvioRepository.findByOpintooikeusId(opintooikeusId).ifPresent {
            if (!it.virkailijanKorjausehdotus.isNullOrBlank() || !it.vastuuhenkilonKorjausehdotus.isNullOrBlank()) {
                avoimetAsiat.add(
                    AvoinAsiaDTO(
                        tyyppi = AvoinAsiaTyyppiEnum.VASTUUHENKILON_ARVIO,
                        asia = messageSource.getMessage(
                            "avoimetasiat.vastuuhenkilonarvio",
                            null,
                            kielialue
                        ),
                        pvm = it.muokkauspaiva
                    )
                )
            }
        }
    }

    private fun lisaaSeurantajaksojenPalautukset(avoimetAsiat: MutableList<AvoinAsiaDTO>, opintooikeusId: Long, kielialue: Locale) {
        seurantajaksoRepository.findByOpintooikeusId(opintooikeusId).forEach {
            if (!it.korjausehdotus.isNullOrBlank()) {
                avoimetAsiat.add(
                    AvoinAsiaDTO(
                        id = it.id,
                        tyyppi = AvoinAsiaTyyppiEnum.SEURANTAJAKSO,
                        asia = messageSource.getMessage(
                            "avoimetasiat.seurantajakso",
                            arrayOf(
                                "${it.alkamispaiva?.format(DateTimeFormatter.ofPattern(kielialue.pattern()))}",
                                "${it.paattymispaiva?.format(DateTimeFormatter.ofPattern(kielialue.pattern()))}"
                            ),
                            kielialue
                        ),
                        pvm = it.tallennettu
                    )
                )
            }
        }
    }

    fun lisaaValmistumispyynnonPalautus(avoimetAsiat: MutableList<AvoinAsiaDTO>, opintooikeusId: Long, kielialue: Locale) {
        valmistumispyyntoRepository.findByOpintooikeusId(opintooikeusId)?.let {
            if (it.vastuuhenkiloOsaamisenArvioijaPalautusaika != null || it.virkailijanPalautusaika != null || it.vastuuhenkiloHyvaksyjaPalautusaika != null) {
                avoimetAsiat.add(AvoinAsiaDTO(it.id, tyyppi = AvoinAsiaTyyppiEnum.VALMISTUMISPYYNTO, asia = messageSource.getMessage("avoimetasiat.valmistumispyynto",
                            arrayOf(), kielialue), pvm = it.vastuuhenkiloOsaamisenArvioijaPalautusaika
                    ?: it.virkailijanPalautusaika ?: it.vastuuhenkiloHyvaksyjaPalautusaika
                ))
            }
        }
    }
}
