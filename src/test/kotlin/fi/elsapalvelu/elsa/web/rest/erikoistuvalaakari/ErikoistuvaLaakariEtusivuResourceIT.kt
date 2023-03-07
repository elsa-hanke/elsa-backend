package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.domain.enumeration.ArviointiasteikkoTyyppi
import fi.elsapalvelu.elsa.domain.enumeration.AvoinAsiaTyyppiEnum
import fi.elsapalvelu.elsa.domain.enumeration.OpintosuoritusTyyppiEnum
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.service.dto.enumeration.KoejaksoTila
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.helpers.*
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import jakarta.persistence.EntityManager

private const val ENDPOINT_URL: String = "/api/erikoistuva-laakari/etusivu"

@AutoConfigureMockMvc
@SpringBootTest(classes = [ElsaBackendApp::class])
class ErikoistuvaLaakariEtusivuResourceIT {

    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var restEtusivuMockMvc: MockMvc

    private lateinit var erikoistuvaLaakari: ErikoistuvaLaakari

    private lateinit var arvioitavaKokonaisuus1: ArvioitavaKokonaisuus

    private lateinit var arvioitavaKokonaisuus2: ArvioitavaKokonaisuus

    private lateinit var arvioitavanKokonaisuudenKategoria: ArvioitavanKokonaisuudenKategoria

    private lateinit var suorite1: Suorite

    private lateinit var suorite2: Suorite

    private lateinit var suoritemerkinta: Suoritemerkinta

    private lateinit var suoritteenKategoria: SuoritteenKategoria

    @MockBean
    private lateinit var clock: Clock

    // 1 kuukausi = 31.1.1970
    private val now = 2592000L

    @BeforeEach
    fun setup() {
        val user =
            KayttajaResourceWithMockUserIT.createEntity(authority = Authority(ERIKOISTUVA_LAAKARI))
        em.persist(user)
        em.flush()
        val userDetails = mapOf<String, List<Any>>()
        val authorities = listOf(SimpleGrantedAuthority(ERIKOISTUVA_LAAKARI))
        val authentication = Saml2Authentication(
            DefaultSaml2AuthenticatedPrincipal(user.id, userDetails),
            "test",
            authorities
        )
        TestSecurityContextHolder.getContext().authentication = authentication

        val erikoisala = ErikoisalaHelper.createEntity()
        em.persist(erikoisala)

        erikoistuvaLaakari =
            ErikoistuvaLaakariHelper.createEntity(em, user, erikoisala = erikoisala)

        em.persist(
            TyoskentelyjaksoHelper.createEntity(
                em,
                erikoistuvaLaakari.kayttaja?.user
            )
        )

        arvioitavanKokonaisuudenKategoria =
            ArvioitavanKokonaisuudenKategoriaHelper.createEntity(em, erikoisala)
        em.persist(arvioitavanKokonaisuudenKategoria)

        arvioitavaKokonaisuus1 =
            ArvioitavaKokonaisuusHelper.createEntity(
                em,
                existingKategoria = arvioitavanKokonaisuudenKategoria
            )
        em.persist(arvioitavaKokonaisuus1)

        arvioitavaKokonaisuus2 =
            ArvioitavaKokonaisuusHelper.createEntity(
                em,
                existingKategoria = arvioitavanKokonaisuudenKategoria
            )
        em.persist(arvioitavaKokonaisuus2)

        val suoritusarviointi1 = SuoritusarviointiHelper.createEntity(
            em,
            erikoistuvaLaakari.kayttaja?.user,
            arviointiasteikonTaso = 2,
            arvioitavaKokonaisuus = arvioitavaKokonaisuus1
        )
        em.persist(suoritusarviointi1)

        val suoritusarviointi2 = SuoritusarviointiHelper.createEntity(
            em,
            erikoistuvaLaakari.kayttaja?.user,
            arviointiasteikonTaso = 3,
            arvioitavaKokonaisuus = arvioitavaKokonaisuus2
        )
        em.persist(suoritusarviointi2)

        suoritteenKategoria = SuoritteenKategoriaHelper.createEntity(em, erikoisala)
        em.persist(suoritteenKategoria)

        suorite1 =
            SuoriteHelper.createEntity(
                em,
                existingKategoria = suoritteenKategoria,
                vaadittuLkm = 1
            )
        em.persist(suorite1)

        suorite2 =
            SuoriteHelper.createEntity(
                em,
                existingKategoria = suoritteenKategoria,
                vaadittuLkm = 5
            )
        em.persist(suorite2)

        suoritemerkinta = SuoritemerkintaHelper.createEntity(
            em,
            erikoisala,
            erikoistuvaLaakari.kayttaja?.user,
            existingSuorite = suorite1
        )
        em.persist(suoritemerkinta)

        em.persist(
            SuoritemerkintaHelper.createEntity(
                em,
                erikoisala,
                erikoistuvaLaakari.kayttaja?.user,
                existingSuorite = suorite2
            )
        )

        em.persist(
            TeoriakoulutusHelper.createEntity(
                em,
                erikoistuvaLaakari.kayttaja?.user,
                LocalDate.ofEpochDay(0),
                LocalDate.ofEpochDay(5),
                37.5
            )
        )

        em.persist(
            TeoriakoulutusHelper.createEntity(
                em,
                erikoistuvaLaakari.kayttaja?.user,
                LocalDate.ofEpochDay(5),
                LocalDate.ofEpochDay(10),
                20.0

            )
        )

        val johtamisopinnotTyyppi =
            OpintosuoritusTyyppi(nimi = OpintosuoritusTyyppiEnum.JOHTAMISOPINTO)
        em.persist(johtamisopinnotTyyppi)

        val sateilysuojakoulutusTyyppi =
            OpintosuoritusTyyppi(nimi = OpintosuoritusTyyppiEnum.SATEILYSUOJAKOULUTUS)
        em.persist(sateilysuojakoulutusTyyppi)

        val kuulusteluTyyppi =
            OpintosuoritusTyyppi(nimi = OpintosuoritusTyyppiEnum.VALTAKUNNALLINEN_KUULUSTELU)
        em.persist(kuulusteluTyyppi)

        em.persist(
            Opintosuoritus(
                nimi_fi = "Johtamisopinto 1",
                kurssikoodi = "JOHT-1",
                suorituspaiva = LocalDate.ofEpochDay(3L),
                opintopisteet = 3.0,
                hyvaksytty = true,
                opintooikeus = erikoistuvaLaakari.getOpintooikeusKaytossa(),
                tyyppi = johtamisopinnotTyyppi
            )
        )

        em.persist(
            Opintosuoritus(
                nimi_fi = "Johtamisopinto 2",
                kurssikoodi = "JOHT-2",
                suorituspaiva = LocalDate.ofEpochDay(4L),
                opintopisteet = 2.0,
                hyvaksytty = true,
                opintooikeus = erikoistuvaLaakari.getOpintooikeusKaytossa(),
                tyyppi = johtamisopinnotTyyppi
            )
        )

        em.persist(
            Opintosuoritus(
                nimi_fi = "Säteilysuojakoulutus 1",
                kurssikoodi = "SÄT-1",
                suorituspaiva = LocalDate.ofEpochDay(3L),
                opintopisteet = 1.0,
                hyvaksytty = true,
                opintooikeus = erikoistuvaLaakari.getOpintooikeusKaytossa(),
                tyyppi = sateilysuojakoulutusTyyppi
            )
        )

        em.persist(
            Opintosuoritus(
                nimi_fi = "Säteilysuojakoulutus 2",
                kurssikoodi = "SÄT-2",
                suorituspaiva = LocalDate.ofEpochDay(4L),
                opintopisteet = 1.0,
                hyvaksytty = true,
                opintooikeus = erikoistuvaLaakari.getOpintooikeusKaytossa(),
                tyyppi = sateilysuojakoulutusTyyppi
            )
        )

        em.persist(
            Opintosuoritus(
                nimi_fi = "Kuulustelu 1",
                kurssikoodi = "TENTTI-1",
                suorituspaiva = LocalDate.ofEpochDay(3L),
                hyvaksytty = true,
                opintooikeus = erikoistuvaLaakari.getOpintooikeusKaytossa(),
                tyyppi = kuulusteluTyyppi
            )
        )

        em.persist(
            Opintosuoritus(
                nimi_fi = "Kuulustelu 2",
                kurssikoodi = "TENTTI-2",
                suorituspaiva = LocalDate.ofEpochDay(4L),
                hyvaksytty = true,
                opintooikeus = erikoistuvaLaakari.getOpintooikeusKaytossa(),
                tyyppi = kuulusteluTyyppi
            )
        )
    }

    @Test
    @Transactional
    fun getErikoistumisenEdistyminen() {
        flushAndClear()

        restEtusivuMockMvc.perform(get("$ENDPOINT_URL/erikoistumisen-edistyminen"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.arviointienKeskiarvo").value(2.5))
            .andExpect(jsonPath("$.arvioitavatKokonaisuudetVahintaanYksiArvioLkm").value(2))
            .andExpect(jsonPath("$.arvioitavienKokonaisuuksienLkm").value(2))
            .andExpect(jsonPath("$.arviointiasteikko.nimi").value(ArviointiasteikkoTyyppi.EPA.toString()))
            .andExpect(jsonPath("$.suoritemerkinnatLkm").value(2))
            .andExpect(jsonPath("$.vaaditutSuoritemerkinnatLkm").value(6))
            .andExpect(jsonPath("$.osaalueetSuoritettuLkm").value(1))
            .andExpect(jsonPath("$.osaalueetVaadittuLkm").value(2))
            .andExpect(
                jsonPath("$.tyoskentelyjaksoTilastot.tyoskentelyaikaYhteensa").value(
                    5
                )
            )
            .andExpect(jsonPath("$.teoriakoulutuksetSuoritettu").value(57.5))
            .andExpect(
                jsonPath("$.teoriakoulutuksetVaadittu").value(
                    OpintoopasHelper.DEFAULT_ERIKOISALAN_VAATIMA_TEORIAKOULUTUSTEN_VAHIMMAISMAARA
                )
            )
            .andExpect(jsonPath("$.johtamisopinnotSuoritettu").value(5.0))
            .andExpect(
                jsonPath("$.johtamisopinnotVaadittu").value(
                    OpintoopasHelper.DEFAULT_ERIKOISALAN_VAATIMA_JOHTAMISOPINTOJEN_VAHIMMAISMAARA
                )
            )
            .andExpect(jsonPath("$.sateilysuojakoulutuksetSuoritettu").value(2.0))
            .andExpect(
                jsonPath("$.sateilysuojakoulutuksetVaadittu").value(
                    OpintoopasHelper.DEFAULT_ERIKOISALAN_VAATIMA_SATEILYSUOJAKOULUTUSTEN_VAHIMMAISMAARA
                )
            )
            .andExpect(jsonPath("$.koejaksoTila").value(KoejaksoTila.EI_AKTIIVINEN.toString()))
            .andExpect(jsonPath("$.valtakunnallisetKuulustelutSuoritettuLkm").value(2))
            .andExpect(
                jsonPath("$.opintooikeudenMyontamispaiva").value(
                    erikoistuvaLaakari.getOpintooikeusKaytossa()?.opintooikeudenMyontamispaiva.toString()
                )
            )
            .andExpect(
                jsonPath("$.opintooikeudenPaattymispaiva").value(
                    erikoistuvaLaakari.getOpintooikeusKaytossa()?.opintooikeudenPaattymispaiva.toString()
                )
            )
    }

    @Test
    @Transactional
    fun `test that only highest value of arviointiasteikonTaso is calculated into average`() {
        em.persist(
            SuoritusarviointiHelper.createEntity(
                em,
                erikoistuvaLaakari.kayttaja?.user,
                arviointiasteikonTaso = 5,
                arvioitavaKokonaisuus = arvioitavaKokonaisuus1
            )
        )

        em.persist(
            SuoritusarviointiHelper.createEntity(
                em,
                erikoistuvaLaakari.kayttaja?.user,
                arviointiasteikonTaso = 5,
                arvioitavaKokonaisuus = arvioitavaKokonaisuus2
            )
        )

        flushAndClear()

        restEtusivuMockMvc.perform(get("$ENDPOINT_URL/erikoistumisen-edistyminen"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.arviointienKeskiarvo").value(5.0))
    }

    @Test
    @Transactional
    fun `test that arvioitavaKokonaisuus not valid is calculated into amount of suoritettu if has arviointi`() {
        arvioitavaKokonaisuus1.voimassaoloLoppuu = LocalDate.ofEpochDay(5L)
        flushAndClear()

        restEtusivuMockMvc.perform(get("$ENDPOINT_URL/erikoistumisen-edistyminen"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.arvioitavatKokonaisuudetVahintaanYksiArvioLkm").value(2))
    }

    @Test
    @Transactional
    fun `test that arvioitavaKokonaisuus is calculated into amount of suoritettu if has arviointi and kategoria not valid`() {
        arvioitavaKokonaisuus1.voimassaoloLoppuu = LocalDate.ofEpochDay(5L)
        arvioitavaKokonaisuus2.voimassaoloLoppuu = LocalDate.ofEpochDay(5L)
        flushAndClear()

        restEtusivuMockMvc.perform(get("$ENDPOINT_URL/erikoistumisen-edistyminen"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.arvioitavatKokonaisuudetVahintaanYksiArvioLkm").value(2))
    }

    @Test
    @Transactional
    fun `test that arvioitavaKokonaisuus not valid is calculated into total amount if has arviointi`() {
        arvioitavaKokonaisuus1.voimassaoloLoppuu = LocalDate.ofEpochDay(5L)
        flushAndClear()

        restEtusivuMockMvc.perform(get("$ENDPOINT_URL/erikoistumisen-edistyminen"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.arvioitavienKokonaisuuksienLkm").value(2))
    }

    @Test
    @Transactional
    fun `test that arvioitavaKokonaisuus is calculated into total amount if has arviointi and kategoria not valid`() {
        arvioitavaKokonaisuus1.voimassaoloLoppuu = LocalDate.ofEpochDay(5L)
        arvioitavaKokonaisuus2.voimassaoloLoppuu = LocalDate.ofEpochDay(5L)
        flushAndClear()

        restEtusivuMockMvc.perform(get("$ENDPOINT_URL/erikoistumisen-edistyminen"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.arvioitavienKokonaisuuksienLkm").value(2))
    }

    @Test
    @Transactional
    fun `test that suorite not valid is calculated into required amount if has suoritemerkinta`() {
        suorite1.voimassaolonPaattymispaiva = LocalDate.ofEpochDay(5L)
        flushAndClear()

        restEtusivuMockMvc.perform(get("$ENDPOINT_URL/erikoistumisen-edistyminen"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.suoritemerkinnatLkm").value(2))
    }

    @Test
    @Transactional
    fun `test that suorite not valid is not calculated into required amount if not having suoritemerkinta`() {
        suorite1.voimassaolonPaattymispaiva = LocalDate.ofEpochDay(5L)
        em.remove(suoritemerkinta)
        flushAndClear()

        restEtusivuMockMvc.perform(get("$ENDPOINT_URL/erikoistumisen-edistyminen"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.suoritemerkinnatLkm").value(1))
    }

    @Test
    @Transactional
    fun `test that suorite is calculated into required amount if has suoritemerkinta and kategoria not valid`() {
        suorite1.voimassaolonPaattymispaiva = LocalDate.ofEpochDay(5L)
        suorite2.voimassaolonPaattymispaiva = LocalDate.ofEpochDay(5L)
        flushAndClear()

        restEtusivuMockMvc.perform(get("$ENDPOINT_URL/erikoistumisen-edistyminen"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.suoritemerkinnatLkm").value(2))
    }

    @Test
    @Transactional
    fun `test that suorite is not calculated into required amount if not having suoritemerkinta and kategoria not valid`() {
        suorite1.voimassaolonPaattymispaiva = LocalDate.ofEpochDay(5L)
        suorite2.voimassaolonPaattymispaiva = LocalDate.ofEpochDay(5L)
        em.remove(suoritemerkinta)
        flushAndClear()

        restEtusivuMockMvc.perform(get("$ENDPOINT_URL/erikoistumisen-edistyminen"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.suoritemerkinnatLkm").value(1))
    }

    @Test
    @Transactional
    fun `test that suorite not valid is calculated only once instead of vaadittulkm if has suoritemerkinta`() {
        suorite1.voimassaolonPaattymispaiva = LocalDate.ofEpochDay(5L)
        flushAndClear()

        restEtusivuMockMvc.perform(get("$ENDPOINT_URL/erikoistumisen-edistyminen"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.vaaditutSuoritemerkinnatLkm").value(6))
    }

    @Test
    @Transactional
    fun `test that suorite is calculated only once instead of vaadittulkm if has suoritemerkinta and kategoria not valid`() {
        suorite1.voimassaolonPaattymispaiva = LocalDate.ofEpochDay(5L)
        suorite2.voimassaolonPaattymispaiva = LocalDate.ofEpochDay(5L)
        flushAndClear()

        restEtusivuMockMvc.perform(get("$ENDPOINT_URL/erikoistumisen-edistyminen"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.vaaditutSuoritemerkinnatLkm").value(2))
    }

    @Test
    @Transactional
    fun `test that suorite not valid is calculated into completed osaalueet if has suoritemerkinta`() {
        suorite1.voimassaolonPaattymispaiva = LocalDate.ofEpochDay(5L)
        flushAndClear()

        restEtusivuMockMvc.perform(get("$ENDPOINT_URL/erikoistumisen-edistyminen"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.osaalueetSuoritettuLkm").value(1))
    }

    @Test
    @Transactional
    fun `test that suorite is calculated into completed osaalueet if has suoritemerkinta and kategoria not valid`() {
        suorite1.voimassaolonPaattymispaiva = LocalDate.ofEpochDay(5L)
        suorite2.voimassaolonPaattymispaiva = LocalDate.ofEpochDay(5L)
        flushAndClear()

        restEtusivuMockMvc.perform(get("$ENDPOINT_URL/erikoistumisen-edistyminen"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            // Koska kummallekin suoritteelle käytetty kategoria ei ole enää voimassa, lasketaan myös toinen osa-alue
            // (suorite 2, jolle asetettu vaadittulkm = 5) suoritetuksi vaikka itse suorite vielä onkin voimassa.
            .andExpect(jsonPath("$.osaalueetSuoritettuLkm").value(2))
    }

    @Test
    @Transactional
    fun `test that suorite not valid is calculated into osaalueet required if has suoritemerkinta`() {
        suorite1.voimassaolonPaattymispaiva = LocalDate.ofEpochDay(5L)
        flushAndClear()

        restEtusivuMockMvc.perform(get("$ENDPOINT_URL/erikoistumisen-edistyminen"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.osaalueetVaadittuLkm").value(2))
    }

    @Test
    @Transactional
    fun `test that suorite not valid is not calculated into osaalueet required if not having suoritemerkinta`() {
        suorite1.voimassaolonPaattymispaiva = LocalDate.ofEpochDay(5L)
        em.remove(suoritemerkinta)
        flushAndClear()

        restEtusivuMockMvc.perform(get("$ENDPOINT_URL/erikoistumisen-edistyminen"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.osaalueetVaadittuLkm").value(1))
    }

    @Test
    @Transactional
    fun `test that suorite is calculated into osaalueet required if has suoritemerkinta and kategoria not valid`() {
        suorite1.voimassaolonPaattymispaiva = LocalDate.ofEpochDay(5L)
        suorite2.voimassaolonPaattymispaiva = LocalDate.ofEpochDay(5L)
        flushAndClear()

        restEtusivuMockMvc.perform(get("$ENDPOINT_URL/erikoistumisen-edistyminen"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.osaalueetVaadittuLkm").value(2))
    }

    @Test
    @Transactional
    fun `test that suorite is not calculated into osaalueet required if not having suoritemerkinta and kategoria not valid`() {
        suorite1.voimassaolonPaattymispaiva = LocalDate.ofEpochDay(5L)
        suorite2.voimassaolonPaattymispaiva = LocalDate.ofEpochDay(5L)
        em.remove(suoritemerkinta)
        flushAndClear()

        restEtusivuMockMvc.perform(get("$ENDPOINT_URL/erikoistumisen-edistyminen"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.osaalueetVaadittuLkm").value(1))
    }

    @Test
    @Transactional
    fun getVanhenevatKatseluoikeudet() {
        initMockTime()

        // Katseluoikeus päättyy 28.2.1970
        val katseluoikeus =
            KouluttajavaltuutusHelper.createEntity(em, paattymispaiva = LocalDate.ofEpochDay(58L))
        em.persist(katseluoikeus)

        restEtusivuMockMvc.perform(get("$ENDPOINT_URL/avoimet-asiat"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").value(hasSize<Int>(1)))
            .andExpect(jsonPath("$[0].tyyppi").value(AvoinAsiaTyyppiEnum.KOULUTTAJAVALTUUTUS.toString()))
    }

    @Test
    @Transactional
    fun testKatseluoikeusEndingOverOneMonth() {
        initMockTime()

        // Katseluoikeus päättyy 1.3.1970
        val katseluoikeus =
            KouluttajavaltuutusHelper.createEntity(em, paattymispaiva = LocalDate.ofEpochDay(59L))
        em.persist(katseluoikeus)

        restEtusivuMockMvc.perform(get("$ENDPOINT_URL/avoimet-asiat"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").value(hasSize<Int>(0)))
    }

    @Test
    @Transactional
    fun testKatseluoikeusAlreadyEnded() {
        initMockTime()

        // Katseluoikeus päättyy 1.3.1970
        val katseluoikeus =
            KouluttajavaltuutusHelper.createEntity(em, paattymispaiva = LocalDate.ofEpochDay(29L))
        em.persist(katseluoikeus)

        restEtusivuMockMvc.perform(get("$ENDPOINT_URL/avoimet-asiat"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").value(hasSize<Int>(0)))
    }

    @Test
    @Transactional
    fun testKatseluoikeusForAnotherOpintooikeusNotReturned() {
        initMockTime()

        val anotherOpintooikeus =
            OpintooikeusHelper.addOpintooikeusForErikoistuvaLaakari(em, erikoistuvaLaakari)

        // Katseluoikeus päättyy 28.2.1970
        val katseluoikeus = KouluttajavaltuutusHelper.createEntity(
            em,
            paattymispaiva = LocalDate.ofEpochDay(58L),
            opintooikeus = anotherOpintooikeus
        )
        em.persist(katseluoikeus)

        restEtusivuMockMvc.perform(get("$ENDPOINT_URL/avoimet-asiat"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").value(hasSize<Int>(0)))
    }

    private fun initMockTime() {
        `when`(clock.instant()).thenReturn(Instant.ofEpochSecond(now))
        `when`(clock.zone).thenReturn(ZoneId.systemDefault())
    }

    private fun flushAndClear() {
        em.flush()
        em.clear()
    }
}
