package fi.elsapalvelu.elsa.service.kayttaja.etusivu

import fi.elsapalvelu.elsa.domain.kayttaja.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.domain.koulutus.Opintoopas
import fi.elsapalvelu.elsa.domain.perustiedot.Erikoisala
import fi.elsapalvelu.elsa.repository.arviointi.ArvioitavanKokonaisuudenKategoriaRepository
import fi.elsapalvelu.elsa.repository.arviointi.SuoritusarviointiRepository
import fi.elsapalvelu.elsa.repository.kayttaja.KayttajaRepository
import fi.elsapalvelu.elsa.repository.kayttaja.KouluttajavaltuutusRepository
import fi.elsapalvelu.elsa.repository.kayttaja.OpintooikeusRepository
import fi.elsapalvelu.elsa.repository.kayttaja.UserRepository
import fi.elsapalvelu.elsa.repository.koejakso.KoejaksonAloituskeskusteluRepository
import fi.elsapalvelu.elsa.repository.koejakso.KoejaksonKoulutussopimusRepository
import fi.elsapalvelu.elsa.repository.koejakso.KoejaksonVastuuhenkilonArvioRepository
import fi.elsapalvelu.elsa.repository.koulutus.OpintosuoritusRepository
import fi.elsapalvelu.elsa.repository.koulutus.TeoriakoulutusRepository
import fi.elsapalvelu.elsa.repository.perustiedot.ErikoisalaRepository
import fi.elsapalvelu.elsa.repository.seuranta.SeurantajaksoRepository
import fi.elsapalvelu.elsa.repository.suoritteet.SuoritemerkintaRepository
import fi.elsapalvelu.elsa.repository.suoritteet.SuoritteenKategoriaRepository
import fi.elsapalvelu.elsa.repository.valmistuminen.TerveyskeskuskoulutusjaksonHyvaksyntaRepository
import fi.elsapalvelu.elsa.repository.valmistuminen.ValmistumispyyntoRepository
import fi.elsapalvelu.elsa.service.arviointi.ErikoistujienSeurantaQueryService
import fi.elsapalvelu.elsa.service.impl.kayttaja.EtusivuServiceImpl
import fi.elsapalvelu.elsa.service.kayttaja.EtusivuService
import fi.elsapalvelu.elsa.service.kayttaja.OpintooikeusService
import fi.elsapalvelu.elsa.service.koulutus.OpintosuoritusService
import fi.elsapalvelu.elsa.service.mapper.arviointi.ArviointiasteikkoMapper
import fi.elsapalvelu.elsa.service.tyoskentely.TyoskentelyjaksoService
import fi.elsapalvelu.elsa.service.valmistuminen.TerveyskeskuskoulutusjaksonHyvaksyntaService
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Optional
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.context.MessageSource

internal class EtusivunTestitiedot {
    val kayttajaRepository = mock<KayttajaRepository>()
    val userRepository = mock<UserRepository>()
    val opintooikeusRepository = mock<OpintooikeusRepository>()
    val tyoskentelyjaksoService = mock<TyoskentelyjaksoService>()
    val suoritusarviointiRepository = mock<SuoritusarviointiRepository>()
    val seurantajaksoRepository = mock<SeurantajaksoRepository>()
    val suoritemerkintaRepository = mock<SuoritemerkintaRepository>()
    val koejaksonKoulutussopimusRepository = mock<KoejaksonKoulutussopimusRepository>()
    val koejaksonAloituskeskusteluRepository = mock<KoejaksonAloituskeskusteluRepository>()
    val koejaksonVastuuhenkilonArvioRepository = mock<KoejaksonVastuuhenkilonArvioRepository>()
    val arvioitavanKokonaisuudenKategoriaRepository = mock<ArvioitavanKokonaisuudenKategoriaRepository>()
    val suoritteenKategoriaRepository = mock<SuoritteenKategoriaRepository>()
    val erikoistujienSeurantaQueryService = mock<ErikoistujienSeurantaQueryService>()
    val teoriakoulutusRepository = mock<TeoriakoulutusRepository>()
    val opintosuoritusRepository = mock<OpintosuoritusRepository>()
    val arviointiasteikkoMapper = mock<ArviointiasteikkoMapper>()
    val opintooikeusService = mock<OpintooikeusService>()
    val kouluttajavaltuutusRepository = mock<KouluttajavaltuutusRepository>()
    val terveyskeskuskoulutusjaksonHyvaksyntaService = mock<TerveyskeskuskoulutusjaksonHyvaksyntaService>()
    val terveyskeskuskoulutusjaksonHyvaksyntaRepository = mock<TerveyskeskuskoulutusjaksonHyvaksyntaRepository>()
    val valmistumispyyntoRepository = mock<ValmistumispyyntoRepository>()
    val opintosuoritusService = mock<OpintosuoritusService>()
    val clock: Clock = Clock.fixed(Instant.parse("2020-01-31T12:00:00Z"), ZoneOffset.UTC)
    val messageSource = mock<MessageSource>()
    val erikoisalaRepository = mock<ErikoisalaRepository>()
    val opintooikeus = Opintooikeus(
        id = 11L,
        erikoisala = Erikoisala(id = 2L),
        erikoistuvaLaakari = ErikoistuvaLaakari(id = 21L),
        osaamisenArvioinninOppaanPvm = LocalDate.of(2020, 1, 1),
        opintoopas = Opintoopas(erikoisalanVaatimaTeoriakoulutustenVahimmaismaara = 60.0)
    )

    init {
        whenever(opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId("opiskelija")).thenReturn(11L)
        whenever(opintooikeusRepository.findById(11L)).thenReturn(Optional.of(opintooikeus))
        whenever(userRepository.findById("opiskelija")).thenReturn(Optional.of(User(id = "opiskelija", langKey = "sv")))
        whenever(erikoisalaRepository.findById(2L)).thenReturn(Optional.of(opintooikeus.erikoisala!!))
    }

    fun luoPalvelu(): EtusivuService {
        val osaaminen = OsaamisenYhteenvetopalvelu(
            suoritusarviointiRepository,
            arvioitavanKokonaisuudenKategoriaRepository,
            suoritemerkintaRepository,
            suoritteenKategoriaRepository
        )
        val koejakso = EtusivunKoejaksopalvelu(
            koejaksonKoulutussopimusRepository,
            koejaksonVastuuhenkilonArvioRepository,
            koejaksonAloituskeskusteluRepository
        )
        val edistyminen = EtusivunEdistymispalvelu(
            tyoskentelyjaksoService,
            seurantajaksoRepository,
            opintosuoritusRepository,
            teoriakoulutusRepository,
            arviointiasteikkoMapper,
            opintooikeusRepository,
            opintooikeusService,
            osaaminen,
            koejakso
        )
        val seuranta = EtusivunSeurantapalvelu(
            kayttajaRepository,
            erikoistujienSeurantaQueryService,
            edistyminen
        )
        val palautukset = EtusivunPalautuspalvelu(
            koejaksonKoulutussopimusRepository,
            koejaksonAloituskeskusteluRepository,
            koejaksonVastuuhenkilonArvioRepository,
            seurantajaksoRepository,
            valmistumispyyntoRepository,
            messageSource
        )
        val terveyskeskuskoulutus = EtusivunTerveyskeskuskoulutuspalvelu(
            opintooikeusRepository,
            erikoisalaRepository,
            opintosuoritusService,
            terveyskeskuskoulutusjaksonHyvaksyntaRepository,
            terveyskeskuskoulutusjaksonHyvaksyntaService,
            messageSource
        )
        val avoimetAsiat = EtusivunAvoimienAsioidenPalvelu(
            userRepository,
            opintooikeusService,
            kouluttajavaltuutusRepository,
            clock,
            messageSource,
            palautukset,
            terveyskeskuskoulutus
        )
        return EtusivuServiceImpl(seuranta, edistyminen, avoimetAsiat)
    }
}
