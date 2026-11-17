package fi.elsapalvelu.elsa.service.kayttaja.etusivu

import fi.elsapalvelu.elsa.domain.kayttaja.AvoinAsiaTyyppiEnum
import fi.elsapalvelu.elsa.repository.kayttaja.KouluttajavaltuutusRepository
import fi.elsapalvelu.elsa.repository.kayttaja.UserRepository
import fi.elsapalvelu.elsa.service.constants.KAYTTAJA_NOT_FOUND_ERROR
import fi.elsapalvelu.elsa.service.dto.kayttaja.AvoinAsiaDTO
import fi.elsapalvelu.elsa.service.dto.kayttaja.KatseluoikeusDTO
import fi.elsapalvelu.elsa.service.kayttaja.OpintooikeusService
import jakarta.persistence.EntityNotFoundException
import java.time.Clock
import java.time.LocalDate
import java.util.Locale
import org.springframework.context.MessageSource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class EtusivunAvoimienAsioidenPalvelu(
    private val userRepository: UserRepository,
    private val opintooikeusService: OpintooikeusService,
    private val kouluttajavaltuutusRepository: KouluttajavaltuutusRepository,
    private val kello: Clock,
    private val messageSource: MessageSource,
    private val palautuspalvelu: EtusivunPalautuspalvelu,
    private val terveyskeskuskoulutuspalvelu: EtusivunTerveyskeskuskoulutuspalvelu
) {

    fun haeAvoimetAsiat(kayttajatunnus: String, yek: Boolean = false): List<AvoinAsiaDTO>? {
        val avoimetAsiat = mutableListOf<AvoinAsiaDTO>()
        val opintooikeusId = opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(kayttajatunnus)
        val kayttajatili = userRepository.findById(kayttajatunnus).orElseThrow { EntityNotFoundException(KAYTTAJA_NOT_FOUND_ERROR) }
        val kielialue = Locale.forLanguageTag(kayttajatili.langKey ?: "fi")

        lisaaVanhenevatKatseluoikeudet(avoimetAsiat, opintooikeusId, kielialue)
        if (!yek) palautuspalvelu.lisaaKoejaksonJaSeurantajaksojenPalautukset(avoimetAsiat, opintooikeusId, kielialue)
        terveyskeskuskoulutuspalvelu.lisaaHyvaksynnanAvoinAsia(avoimetAsiat, opintooikeusId, kielialue, yek)
        palautuspalvelu.lisaaValmistumispyynnonPalautus(avoimetAsiat, opintooikeusId, kielialue)

        return avoimetAsiat.sortedBy { it.pvm }
    }

    fun haeKouluttajanVanhenevatKatseluoikeudet(kayttajatunnus: String): List<KatseluoikeusDTO>? {
        return kouluttajavaltuutusRepository.findAllByValtuutettuUserIdAndPaattymispaivaBeforeAndPaattymispaivaAfter(
            kayttajatunnus,
            vanhenemisenYlaraja(),
            vanhenemisenAlaraja()
        ).map {
            KatseluoikeusDTO(it.valtuuttajaOpintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi(), it.paattymispaiva)
        }
    }

    private fun lisaaVanhenevatKatseluoikeudet(avoimetAsiat: MutableList<AvoinAsiaDTO>, opintooikeusId: Long, kielialue: Locale) {
        kouluttajavaltuutusRepository.findAllByValtuuttajaOpintooikeusIdAndPaattymispaivaBeforeAndPaattymispaivaAfter(
            opintooikeusId,
            vanhenemisenYlaraja(),
            vanhenemisenAlaraja()
        ).map {
            val avoinAsia = AvoinAsiaDTO(
                it.id,
                AvoinAsiaTyyppiEnum.KOULUTTAJAVALTUUTUS,
                messageSource.getMessage("avoimetasiat.kouluttajavaltuutus", arrayOf("${it.valtuutettu?.user?.firstName} ${it.valtuutettu?.user?.lastName}"), kielialue),
                it.paattymispaiva
            )
            avoimetAsiat.add(avoinAsia)
        }
    }

    private fun vanhenemisenYlaraja() = LocalDate.now(kello).plusMonths(1).plusDays(1)

    private fun vanhenemisenAlaraja() = LocalDate.now(kello).minusDays(1)
}
