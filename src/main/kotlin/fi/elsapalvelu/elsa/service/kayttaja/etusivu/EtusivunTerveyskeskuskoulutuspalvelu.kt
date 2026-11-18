package fi.elsapalvelu.elsa.service.kayttaja.etusivu

import fi.elsapalvelu.elsa.domain.kayttaja.AvoinAsiaTyyppiEnum
import fi.elsapalvelu.elsa.domain.perustiedot.ErikoisalaTyyppi
import fi.elsapalvelu.elsa.repository.kayttaja.OpintooikeusRepository
import fi.elsapalvelu.elsa.repository.perustiedot.ErikoisalaRepository
import fi.elsapalvelu.elsa.repository.valmistuminen.TerveyskeskuskoulutusjaksonHyvaksyntaRepository
import fi.elsapalvelu.elsa.required
import fi.elsapalvelu.elsa.service.dto.kayttaja.AvoinAsiaDTO
import fi.elsapalvelu.elsa.service.koulutus.OpintosuoritusService
import fi.elsapalvelu.elsa.service.valmistuminen.TerveyskeskuskoulutusjaksonHyvaksyntaService
import java.time.LocalDate
import java.util.Locale
import kotlin.jvm.optionals.getOrNull
import org.springframework.context.MessageSource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class EtusivunTerveyskeskuskoulutuspalvelu(
    private val opintooikeusRepository: OpintooikeusRepository,
    private val erikoisalaRepository: ErikoisalaRepository,
    private val opintosuoritusService: OpintosuoritusService,
    private val terveyskeskuskoulutusjaksonHyvaksyntaRepository: TerveyskeskuskoulutusjaksonHyvaksyntaRepository,
    private val terveyskeskuskoulutusjaksonHyvaksyntaService: TerveyskeskuskoulutusjaksonHyvaksyntaService,
    private val messageSource: MessageSource
) {

    fun lisaaHyvaksynnanAvoinAsia(
        avoimetAsiat: MutableList<AvoinAsiaDTO>, opintooikeusId: Long, kielialue: Locale, yek: Boolean = false
    ) {
        val opintooikeus = opintooikeusRepository.findById(opintooikeusId).getOrNull()
        val erikoisala = erikoisalaRepository.findById(opintooikeus?.erikoisala?.id.required()).getOrNull()
        // Hammaslääketieteen opintooikeuteen ei kuulu terveyskeskuskoulutusjaksoa.
        if (erikoisala?.tyyppi == ErikoisalaTyyppi.HAMMASLAAKETIEDE) return
        if (opintosuoritusService.getTerveyskoulutusjaksoSuoritettu(
                opintooikeusId, requireNotNull(opintooikeus).erikoistuvaLaakari?.id.required()
            )) return

        val hyvaksynta = terveyskeskuskoulutusjaksonHyvaksyntaRepository.findByOpintooikeusId(opintooikeusId)
        if (hyvaksynta != null) {
            if (!hyvaksynta.virkailijanKorjausehdotus.isNullOrBlank() || !hyvaksynta.vastuuhenkilonKorjausehdotus.isNullOrBlank()) {
                avoimetAsiat.add(muodostaAvoinAsia(kielialue, hyvaksynta.id, hyvaksynta.muokkauspaiva))
            }
            return
        }

        val koulutusSuoritettu = if (yek) {
            terveyskeskuskoulutusjaksonHyvaksyntaService.getTerveyskoulutusjaksoSuoritettuYek(opintooikeusId)
        } else {
            terveyskeskuskoulutusjaksonHyvaksyntaService.getTerveyskoulutusjaksoSuoritettu(opintooikeusId)
        }
        if (koulutusSuoritettu) avoimetAsiat.add(muodostaAvoinAsia(kielialue))
    }

    private fun muodostaAvoinAsia(kielialue: Locale, tunnus: Long? = null, paiva: LocalDate? = null) = AvoinAsiaDTO(
        id = tunnus,
        tyyppi = AvoinAsiaTyyppiEnum.TERVEYSKESKUSKOULUTUSJAKSO,
        asia = messageSource.getMessage("avoimetasiat.terveyskeskuskoulutusjaksonhyvaksynta", arrayOf(), kielialue),
        pvm = paiva
    )
}
