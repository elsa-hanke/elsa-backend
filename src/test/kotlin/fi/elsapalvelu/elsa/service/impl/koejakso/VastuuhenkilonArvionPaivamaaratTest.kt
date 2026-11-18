package fi.elsapalvelu.elsa.service.impl.koejakso

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.kayttaja.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.kayttaja.Kayttaja
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.domain.koejakso.KoejaksonVastuuhenkilonArvio
import fi.elsapalvelu.elsa.domain.perustiedot.Yliopisto
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.repository.kayttaja.AsiakirjaRepository
import fi.elsapalvelu.elsa.repository.kayttaja.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.kayttaja.KayttajaRepository
import fi.elsapalvelu.elsa.repository.kayttaja.OpintooikeusRepository
import fi.elsapalvelu.elsa.repository.koejakso.KoejaksonVastuuhenkilonArvioRepository
import fi.elsapalvelu.elsa.service.arkistointi.koejakso.KoejaksoArkistointiService
import fi.elsapalvelu.elsa.service.dto.kayttaja.AsiakirjaDTO
import fi.elsapalvelu.elsa.service.dto.koejakso.KoejaksonAloituskeskusteluDTO
import fi.elsapalvelu.elsa.service.dto.koejakso.KoejaksonVastuuhenkilonArvioDTO
import fi.elsapalvelu.elsa.service.koejakso.KoejaksonAloituskeskusteluService
import fi.elsapalvelu.elsa.service.mapper.kayttaja.AsiakirjaMapper
import fi.elsapalvelu.elsa.service.mapper.koejakso.KoejaksonVastuuhenkilonArvioMapper
import java.time.LocalDate
import java.util.Optional
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication

class VastuuhenkilonArvionPaivamaaratTest {
    private val arviot = mock<KoejaksonVastuuhenkilonArvioRepository>()
    private val arvioMapper = mock<KoejaksonVastuuhenkilonArvioMapper>()
    private val opintooikeudet = mock<OpintooikeusRepository>()
    private val erikoistujat = mock<ErikoistuvaLaakariRepository>()
    private val kayttajat = mock<KayttajaRepository>()
    private val asiakirjat = mock<AsiakirjaRepository>()
    private val asiakirjaMapper = mock<AsiakirjaMapper>()
    private val arkistointi = mock<KoejaksoArkistointiService>()
    private val aloituskeskustelut = mock<KoejaksonAloituskeskusteluService>()
    private val yliopisto = Yliopisto(id = 7L, nimi = YliopistoEnum.TURUN_YLIOPISTO)
    private val erikoistuja = ErikoistuvaLaakari(id = 5L, kayttaja = Kayttaja(id = 6L, user = User(id = "erikoistuja")))
    private val virkailija = Kayttaja(id = 2L, user = User(id = "virkailija"), yliopistot = mutableSetOf(yliopisto))
    private val vastuuhenkilo = Kayttaja(id = 3L, user = User(id = "vastuuhenkilo"))
    private val aiempiPaiva: LocalDate = LocalDate.of(2019, 1, 1)
    private val lomake = KoejaksonVastuuhenkilonArvioDTO(id = 1L)
    private val muutokset = KoejaksonVastuuhenkilonArvio(id = 1L)
    private val liiteDto = AsiakirjaDTO(nimi = "todistus.pdf")
    private val liite = Asiakirja(nimi = "todistus.pdf")

    @AfterEach
    fun tyhjennaKirjautuminen() = SecurityContextHolder.clearContext()

    @ParameterizedTest
    @MethodSource("paivamaaratapaukset")
    fun `arvion luonti asettaa erikoistujan ja liitteen paivamaarat kellosta`(aika: KoejaksonPaivamaaratapaus) {
        valmisteleArvio()

        luoPalvelu(aika).create(lomake, 11L, setOf(liiteDto))

        assertThat(muutokset.erikoistuvanKuittausaika).isEqualTo(aika.paiva)
        assertThat(liite.lisattypvm).isEqualTo(aika.paikallinenAika())
        assertThat(liite.koejaksonVastuuhenkilonArvio).isSameAs(muutokset)
        verify(arviot).save(muutokset)
    }

    @ParameterizedTest
    @MethodSource("paivamaaratapaukset")
    fun `uudelleenlahetys paivittaa erikoistujan ja uuden liitteen paivamaarat`(aika: KoejaksonPaivamaaratapaus) {
        val arvio = valmisteleArvio()

        luoPalvelu(aika).update(lomake, "erikoistuja", setOf(liiteDto), null)

        assertThat(arvio.erikoistuvanKuittausaika).isEqualTo(aika.paiva)
        assertThat(liite.lisattypvm).isEqualTo(aika.paikallinenAika())
        assertThat(liite.koejaksonVastuuhenkilonArvio).isSameAs(arvio)
    }

    @ParameterizedTest
    @MethodSource("paivamaaratapaukset")
    fun `virkailijan hyvaksynta kayttaa kellon paivaa ja sailyttaa erikoistujan kuittauksen`(aika: KoejaksonPaivamaaratapaus) {
        val arvio = valmisteleArvio()

        luoPalvelu(aika).update(lomake, "virkailija", null, null)

        assertThat(arvio.virkailijanKuittausaika).isEqualTo(aika.paiva)
        assertThat(arvio.erikoistuvanKuittausaika).isEqualTo(aiempiPaiva)
        assertThat(arvio.virkailijaHyvaksynyt).isTrue()
    }

    @ParameterizedTest
    @MethodSource("ratkaisutapaukset")
    fun `vastuuhenkilon ratkaisu ja pdf kayttavat kellon paivaa myos hylattaessa`(tapaus: Ratkaisutapaus) {
        val arvio = valmisteleArvio()
        muutokset.koejaksoHyvaksytty = tapaus.hyvaksytty

        luoPalvelu(tapaus.aika).update(lomake, "vastuuhenkilo", null, null)

        assertThat(arvio.vastuuhenkilonKuittausaika).isEqualTo(tapaus.aika.paiva)
        assertThat(arvio.koejaksoHyvaksytty).isEqualTo(tapaus.hyvaksytty)
        assertThat(arvio.virkailijanKuittausaika).isEqualTo(aiempiPaiva)
        val asiakirja = argumentCaptor<Asiakirja>()
        verify(asiakirjat).save(asiakirja.capture())
        assertThat(asiakirja.firstValue.nimi).isEqualTo("koejakson_vastuuhenkilon_arvio_${tapaus.aika.tiedostonPaiva}.pdf")
        assertThat(asiakirja.firstValue.lisattypvm).isEqualTo(tapaus.aika.paikallinenAika())
        verify(arkistointi).archive(arvio, asiakirja.firstValue)
    }

    @ParameterizedTest
    @MethodSource("palautustapaukset")
    fun `palautus tyhjentaa aiemmat kuittauspaivat luomatta pdf tiedostoa`(tapaus: Palautustapaus) {
        val arvio = valmisteleArvio()
        if (tapaus.kasittelija == "virkailija") muutokset.virkailijanKorjausehdotus = "Korjaa tiedot"
        else muutokset.vastuuhenkilonKorjausehdotus = "Korjaa tiedot"

        luoPalvelu(tapaus.aika).update(lomake, tapaus.kasittelija, null, null)

        assertThat(arvio.erikoistuvanKuittausaika).isNull()
        assertThat(arvio.virkailijanKuittausaika).isNull()
        assertThat(arvio.vastuuhenkilonKuittausaika).isNull()
        verifyNoInteractions(asiakirjat)
    }

    private fun valmisteleArvio(): KoejaksonVastuuhenkilonArvio {
        val opintooikeus = Opintooikeus(id = 11L, erikoistuvaLaakari = erikoistuja, yliopisto = yliopisto)
        val arvio = KoejaksonVastuuhenkilonArvio(id = 1L, opintooikeus = opintooikeus,
            erikoistuvanKuittausaika = aiempiPaiva, virkailijanKuittausaika = aiempiPaiva, virkailijaHyvaksynyt = true)
        whenever(opintooikeudet.findById(11L)).thenReturn(Optional.of(opintooikeus))
        whenever(arviot.findById(1L)).thenReturn(Optional.of(arvio))
        whenever(arviot.save(any<KoejaksonVastuuhenkilonArvio>())).thenAnswer { it.arguments[0] }
        whenever(arvioMapper.toEntity(lomake)).thenReturn(muutokset)
        whenever(arvioMapper.toDto(any<KoejaksonVastuuhenkilonArvio>())).thenReturn(lomake)
        whenever(erikoistujat.findOneByKayttajaUserId("erikoistuja")).thenReturn(erikoistuja)
        whenever(kayttajat.findOneByUserId("virkailija")).thenReturn(Optional.of(virkailija))
        whenever(kayttajat.findById(6L)).thenReturn(Optional.of(erikoistuja.kayttaja!!))
        whenever(kayttajat.findOneByAuthoritiesYliopistoErikoisalaAndVastuuhenkilonTehtavatyyppi(any(), anyOrNull(), anyOrNull(), any()))
            .thenReturn(vastuuhenkilo)
        whenever(asiakirjaMapper.toEntity(liiteDto)).thenReturn(liite)
        whenever(asiakirjat.save(any<Asiakirja>())).thenAnswer { it.arguments[0] }
        whenever(aloituskeskustelut.findByOpintooikeusId(11L)).thenReturn(Optional.of(
            KoejaksonAloituskeskusteluDTO(koejaksonAlkamispaiva = aiempiPaiva, koejaksonPaattymispaiva = aiempiPaiva.plusMonths(6))))
        SecurityContextHolder.getContext().authentication = mock<Saml2Authentication>()
        return arvio
    }

    private fun luoPalvelu(aika: KoejaksonPaivamaaratapaus) = KoejaksonVastuuhenkilonArvioServiceImpl(
        koejaksonVastuuhenkilonArvioRepository = arviot, koejaksonVastuuhenkilonArvioMapper = arvioMapper,
        mailService = mock(), kayttajaRepository = kayttajat, userRepository = mock(), opintooikeusRepository = opintooikeudet,
        tyoskentelyjaksoService = mock(), koejaksonAloituskeskusteluService = aloituskeskustelut,
        koejaksonValiarviointiService = mock(), koejaksonKehittamistoimenpiteetService = mock(), koejaksonLoppukeskusteluService = mock(),
        opintooikeusService = mock(), koulutussopimusRepository = mock(), erikoistuvaLaakariRepository = erikoistujat,
        asiakirjaRepository = asiakirjat, arkistointiConfigurationProvider = mock(), koejaksoArkistointiService = arkistointi,
        keskeytysaikaService = mock(), pdfService = mock(), asiakirjaMapper = asiakirjaMapper, applicationProperties = ApplicationProperties(),
        vastuuhenkilonArvioPdfTextValidator = mock(), clock = aika.kello()
    )

    data class Ratkaisutapaus(val aika: KoejaksonPaivamaaratapaus, val hyvaksytty: Boolean)
    data class Palautustapaus(val aika: KoejaksonPaivamaaratapaus, val kasittelija: String)

    companion object {
        @JvmStatic
        fun paivamaaratapaukset(): List<KoejaksonPaivamaaratapaus> = KoejaksonPaivamaaratapaus.tapaukset()
        @JvmStatic
        fun ratkaisutapaukset(): List<Ratkaisutapaus> = paivamaaratapaukset().flatMap { aika ->
            listOf(false, true).map { Ratkaisutapaus(aika, it) }
        }
        @JvmStatic
        fun palautustapaukset(): List<Palautustapaus> = paivamaaratapaukset().flatMap { aika ->
            listOf("virkailija", "vastuuhenkilo").map { Palautustapaus(aika, it) }
        }
    }
}
