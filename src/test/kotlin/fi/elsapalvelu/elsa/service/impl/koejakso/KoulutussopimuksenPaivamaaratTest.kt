package fi.elsapalvelu.elsa.service.impl.koejakso

import fi.elsapalvelu.elsa.domain.kayttaja.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.kayttaja.Kayttaja
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.koejakso.KoejaksonKoulutussopimus
import fi.elsapalvelu.elsa.domain.koejakso.KoulutussopimuksenKouluttaja
import fi.elsapalvelu.elsa.domain.perustiedot.Yliopisto
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.repository.kayttaja.AsiakirjaRepository
import fi.elsapalvelu.elsa.repository.kayttaja.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.kayttaja.KayttajaRepository
import fi.elsapalvelu.elsa.repository.kayttaja.OpintooikeusRepository
import fi.elsapalvelu.elsa.repository.koejakso.KoejaksonKoulutussopimusRepository
import fi.elsapalvelu.elsa.service.arkistointi.koejakso.KoejaksoArkistointiService
import fi.elsapalvelu.elsa.service.dto.koejakso.KoejaksonKoulutussopimusDTO
import fi.elsapalvelu.elsa.service.dto.koejakso.KoulutussopimuksenKouluttajaDTO
import fi.elsapalvelu.elsa.service.dto.koejakso.KoulutussopimuksenVastuuhenkiloDTO
import fi.elsapalvelu.elsa.service.mapper.koejakso.KoejaksonKoulutussopimusMapper
import java.time.LocalDate
import java.util.Optional
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

class KoulutussopimuksenPaivamaaratTest {
    private val sopimukset = mock<KoejaksonKoulutussopimusRepository>()
    private val sopimusMapper = mock<KoejaksonKoulutussopimusMapper>()
    private val opintooikeudet = mock<OpintooikeusRepository>()
    private val erikoistujat = mock<ErikoistuvaLaakariRepository>()
    private val kayttajat = mock<KayttajaRepository>()
    private val asiakirjat = mock<AsiakirjaRepository>()
    private val arkistointi = mock<KoejaksoArkistointiService>()
    private val erikoistuja = ErikoistuvaLaakari(id = 5L, kayttaja = Kayttaja(id = 6L, user = User(id = "erikoistuja")))
    private val kouluttaja = Kayttaja(id = 2L, user = User(id = "kouluttaja"))
    private val vastuuhenkilo = Kayttaja(id = 3L, user = User(id = "vastuuhenkilo"))
    private val aiempiPaiva: LocalDate = LocalDate.of(2019, 1, 1)
    private val lomake = KoejaksonKoulutussopimusDTO(id = 1L)
    private val muutokset = KoejaksonKoulutussopimus(id = 1L)

    @ParameterizedTest
    @MethodSource("paivamaaratapaukset")
    fun `lahetetyn sopimuksen luonti kayttaa kellon paivaa`(aika: KoejaksonPaivamaaratapaus) {
        valmisteleSopimus()
        muutokset.lahetetty = true

        luoPalvelu(aika).create(lomake, 11L)

        assertThat(muutokset.erikoistuvanAllekirjoitusaika).isEqualTo(aika.paiva)
        verify(sopimukset).save(muutokset)
    }

    @ParameterizedTest
    @MethodSource("paivamaaratapaukset")
    fun `luonnoksen luonti ei aseta allekirjoituspaivaa`(aika: KoejaksonPaivamaaratapaus) {
        valmisteleSopimus()

        luoPalvelu(aika).create(lomake, 11L)

        assertThat(muutokset.erikoistuvanAllekirjoitusaika).isNull()
    }

    @ParameterizedTest
    @MethodSource("paivamaaratapaukset")
    fun `uudelleenlahetys paivittaa allekirjoituspaivan`(aika: KoejaksonPaivamaaratapaus) {
        val sopimus = valmisteleSopimus()
        muutokset.lahetetty = true

        luoPalvelu(aika).update(lomake, "erikoistuja")

        assertThat(sopimus.erikoistuvanAllekirjoitusaika).isEqualTo(aika.paiva)
        assertThat(sopimus.korjausehdotus).isNull()
    }

    @ParameterizedTest
    @MethodSource("paivamaaratapaukset")
    fun `luonnoksen muokkaus sailyttaa aiemman allekirjoituspaivan`(aika: KoejaksonPaivamaaratapaus) {
        val sopimus = valmisteleSopimus()

        luoPalvelu(aika).update(lomake, "erikoistuja")

        assertThat(sopimus.erikoistuvanAllekirjoitusaika).isEqualTo(aiempiPaiva)
    }

    @ParameterizedTest
    @MethodSource("paivamaaratapaukset")
    fun `kouluttajan hyvaksynta kayttaa kellon paivaa`(aika: KoejaksonPaivamaaratapaus) {
        val sopimus = valmisteleSopimus()

        luoPalvelu(aika).update(lomake, "kouluttaja")

        assertThat(sopimus.kouluttajat!!.single().kuittausaika).isEqualTo(aika.paiva)
        assertThat(sopimus.kouluttajat!!.single().sopimusHyvaksytty).isTrue()
        assertThat(sopimus.erikoistuvanAllekirjoitusaika).isEqualTo(aiempiPaiva)
    }

    @ParameterizedTest
    @MethodSource("paivamaaratapaukset")
    fun `vastuuhenkilon hyvaksynta ja pdf kayttavat samaa kellon paivaa`(aika: KoejaksonPaivamaaratapaus) {
        val sopimus = valmisteleSopimus()
        lomake.vastuuhenkilo = KoulutussopimuksenVastuuhenkiloDTO(sopimusHyvaksytty = true)

        luoPalvelu(aika).update(lomake, "vastuuhenkilo")

        assertThat(sopimus.vastuuhenkilonKuittausaika).isEqualTo(aika.paiva)
        val asiakirja = argumentCaptor<Asiakirja>()
        verify(asiakirjat).save(asiakirja.capture())
        assertThat(asiakirja.firstValue.nimi).isEqualTo("koejakson_koulutussopimus_${aika.tiedostonPaiva}.pdf")
        assertThat(asiakirja.firstValue.lisattypvm).isEqualTo(aika.paikallinenAika())
        verify(arkistointi).archive(sopimus, asiakirja.firstValue)
    }

    @ParameterizedTest
    @MethodSource("palautustapaukset")
    fun `palautus poistaa allekirjoituksen ja kouluttajan kuittauksen`(tapaus: Palautustapaus) {
        val sopimus = valmisteleSopimus()
        sopimus.kouluttajat!!.single().apply {
            sopimusHyvaksytty = true
            kuittausaika = aiempiPaiva
        }
        muutokset.korjausehdotus = "Korjaa tiedot"

        luoPalvelu(tapaus.aika).update(lomake, tapaus.kasittelija)

        assertThat(sopimus.erikoistuvanAllekirjoitusaika).isNull()
        assertThat(sopimus.kouluttajat!!.single().kuittausaika).isNull()
        assertThat(sopimus.lahetetty).isFalse()
        assertThat(sopimus.vastuuhenkilonKuittausaika).isNull()
        verifyNoInteractions(asiakirjat)
    }

    private fun valmisteleSopimus(): KoejaksonKoulutussopimus {
        val opintooikeus = Opintooikeus(id = 11L, erikoistuvaLaakari = erikoistuja, yliopisto = Yliopisto(id = 7L, nimi = YliopistoEnum.TURUN_YLIOPISTO))
        val sopimus = KoejaksonKoulutussopimus(id = 1L, opintooikeus = opintooikeus, lahetetty = true,
            erikoistuvanAllekirjoitusaika = aiempiPaiva,
            kouluttajat = mutableSetOf(KoulutussopimuksenKouluttaja(id = 8L, kouluttaja = kouluttaja)))
        lomake.kouluttajat = mutableSetOf(KoulutussopimuksenKouluttajaDTO(id = 8L, kayttajaUserId = "kouluttaja"))
        muutokset.kouluttajat = mutableSetOf(KoulutussopimuksenKouluttaja(id = 8L, kouluttaja = kouluttaja))
        whenever(opintooikeudet.findById(11L)).thenReturn(Optional.of(opintooikeus))
        whenever(sopimukset.findById(1L)).thenReturn(Optional.of(sopimus))
        whenever(sopimukset.save(any<KoejaksonKoulutussopimus>())).thenAnswer { it.arguments[0] }
        whenever(sopimusMapper.toEntity(lomake)).thenReturn(muutokset)
        whenever(sopimusMapper.toDto(any<KoejaksonKoulutussopimus>())).thenReturn(lomake)
        whenever(erikoistujat.findOneByKayttajaUserId("erikoistuja")).thenReturn(erikoistuja)
        whenever(kayttajat.findOneByAuthoritiesYliopistoErikoisalaAndVastuuhenkilonTehtavatyyppi(any(), anyOrNull(), anyOrNull(), any()))
            .thenReturn(vastuuhenkilo)
        listOf(erikoistuja.kayttaja!!, kouluttaja).forEach { whenever(kayttajat.findById(it.id!!)).thenReturn(Optional.of(it)) }
        whenever(asiakirjat.save(any<Asiakirja>())).thenAnswer { it.arguments[0] }
        return sopimus
    }

    private fun luoPalvelu(aika: KoejaksonPaivamaaratapaus) = KoejaksonKoulutussopimusServiceImpl(
        erikoistuvaLaakariRepository = erikoistujat, koejaksonKoulutussopimusMapper = sopimusMapper,
        koejaksonKoulutussopimusRepository = sopimukset, mailService = mock(), kayttajaRepository = kayttajat,
        opintooikeusRepository = opintooikeudet, userRepository = mock(), asiakirjaRepository = asiakirjat,
        kouluttajavaltuutusService = mock(), pdfService = mock(), opintooikeusService = mock(), arkistointiService = arkistointi,
        koulutussopimusPdfTextValidator = mock(), clock = aika.kello()
    )

    data class Palautustapaus(val aika: KoejaksonPaivamaaratapaus, val kasittelija: String)

    companion object {
        @JvmStatic
        fun paivamaaratapaukset(): List<KoejaksonPaivamaaratapaus> = KoejaksonPaivamaaratapaus.tapaukset()
        @JvmStatic
        fun palautustapaukset(): List<Palautustapaus> = paivamaaratapaukset().flatMap { aika ->
            listOf("kouluttaja", "vastuuhenkilo").map { Palautustapaus(aika, it) }
        }
    }
}
