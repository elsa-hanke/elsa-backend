package fi.elsapalvelu.elsa.web.rest.kouluttaja

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.domain.enumeration.ErikoisalaTyyppi
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.repository.*
import fi.elsapalvelu.elsa.security.KOULUTTAJA
import fi.elsapalvelu.elsa.service.dto.enumeration.KoejaksoTila
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.helpers.*
import org.hamcrest.collection.IsCollectionWithSize.hasSize
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
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
import java.time.format.DateTimeFormatter
import jakarta.persistence.EntityManager

@AutoConfigureMockMvc
@SpringBootTest(classes = [ElsaBackendApp::class])
class KouluttajaEtusivuResourceIT {

    @Autowired
    private lateinit var kayttajaYliopistoErikoisalaRepository: KayttajaYliopistoErikoisalaRepository

    @Autowired
    private lateinit var yliopistoRepository: YliopistoRepository

    @Autowired
    private lateinit var erikoisalaRepository: ErikoisalaRepository

    @Autowired
    private lateinit var erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository

    @Autowired
    private lateinit var tyoskentelyjaksoRepository: TyoskentelyjaksoRepository

    @Autowired
    private lateinit var suoritusarviointiRepository: SuoritusarviointiRepository

    @Autowired
    private lateinit var arvioitavaKokonaisuusRepository: ArvioitavaKokonaisuusRepository

    @Autowired
    private lateinit var arvioitavanKokonaisuudenKategoriaRepository: ArvioitavanKokonaisuudenKategoriaRepository

    @Autowired
    private lateinit var suoritemerkintaRepository: SuoritemerkintaRepository

    @Autowired
    private lateinit var seurantajaksoRepository: SeurantajaksoRepository

    @Autowired
    private lateinit var kouluttajavaltuutusRepository: KouluttajavaltuutusRepository

    @Autowired
    private lateinit var aloituskeskusteluRepository: KoejaksonAloituskeskusteluRepository

    @Autowired
    private lateinit var valiarviointiRepository: KoejaksonValiarviointiRepository

    @Autowired
    private lateinit var kehittamistoimenpiteetRepository: KoejaksonKehittamistoimenpiteetRepository

    @Autowired
    private lateinit var loppukeskusteluRepository: KoejaksonLoppukeskusteluRepository

    @Autowired
    private lateinit var kayttajaRepository: KayttajaRepository

    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var restEtusivuMockMvc: MockMvc

    private lateinit var user: User

    private lateinit var kouluttaja: Kayttaja

    @MockBean
    private lateinit var clock: Clock

    // 1 kuukausi = 31.1.1970
    private val now = 2592000L

    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    @Transactional
    fun getErikoistujienSeurantaRajaimet() {
        initTest()

        val yliopisto1 =
            yliopistoRepository.save(Yliopisto(nimi = YliopistoEnum.HELSINGIN_YLIOPISTO))

        val erikoisala1 = erikoisalaRepository.save(
            Erikoisala(
                nimi = "Erikoisala 1",
                tyyppi = ErikoisalaTyyppi.LAAKETIEDE
            )
        )

        val yliopistoAndErikoisala = KayttajaYliopistoErikoisala(
            kayttaja = kouluttaja,
            yliopisto = yliopisto1,
            erikoisala = erikoisala1
        )
        kayttajaYliopistoErikoisalaRepository.save(yliopistoAndErikoisala)
        kouluttaja.yliopistotAndErikoisalat.add(yliopistoAndErikoisala)

        restEtusivuMockMvc.perform(get("/api/kouluttaja/etusivu/erikoistujien-seuranta-rajaimet"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.kayttajaYliopistoErikoisalat").value(hasSize<Int>(1)))
            .andExpect(jsonPath("$.kayttajaYliopistoErikoisalat[0].yliopistoNimi").value(yliopisto1.nimi.toString()))
            .andExpect(
                jsonPath("$.kayttajaYliopistoErikoisalat[0].erikoisalat").value(
                    hasSize<Int>(
                        1
                    )
                )
            )
            .andExpect(
                jsonPath("$.kayttajaYliopistoErikoisalat[0].erikoisalat[0]").value(
                    erikoisala1.nimi
                )
            )
            .andExpect(jsonPath("$.erikoisalat").value(hasSize<Int>(1)))
    }

    @Test
    @Transactional
    fun getErikoistujienSeurantaEmptyList() {
        initTest()

        val yliopisto1 =
            yliopistoRepository.save(Yliopisto(nimi = YliopistoEnum.HELSINGIN_YLIOPISTO))

        val erikoisala1 = erikoisalaRepository.save(
            Erikoisala(
                nimi = "Erikoisala 1",
                tyyppi = ErikoisalaTyyppi.LAAKETIEDE
            )
        )

        val yliopistoAndErikoisala = KayttajaYliopistoErikoisala(
            kayttaja = kouluttaja,
            yliopisto = yliopisto1,
            erikoisala = erikoisala1
        )
        kayttajaYliopistoErikoisalaRepository.save(yliopistoAndErikoisala)
        kouluttaja.yliopistotAndErikoisalat.add(yliopistoAndErikoisala)

        restEtusivuMockMvc.perform(get("/api/kouluttaja/etusivu/erikoistujien-seuranta"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content").value(hasSize<Int>(0)))
    }

    @Test
    @Transactional
    fun getErikoistujienSeuranta() {
        initTest()

        val yliopisto1 =
            yliopistoRepository.save(Yliopisto(nimi = YliopistoEnum.HELSINGIN_YLIOPISTO))

        val erikoisala1 = erikoisalaRepository.findById(1).get()

        val yliopistoAndErikoisala = KayttajaYliopistoErikoisala(
            kayttaja = kouluttaja,
            yliopisto = yliopisto1,
            erikoisala = erikoisala1
        )
        kayttajaYliopistoErikoisalaRepository.save(yliopistoAndErikoisala)
        kouluttaja.yliopistotAndErikoisalat.add(yliopistoAndErikoisala)

        val erikoistuvaLaakari =
            ErikoistuvaLaakariHelper.createEntity(
                em,
                opintooikeudenPaattymispaiva = LocalDate.now().plusYears(5)
            )
        erikoistuvaLaakariRepository.save(erikoistuvaLaakari)

        kouluttajavaltuutusRepository.save(
            Kouluttajavaltuutus(
                alkamispaiva = LocalDate.now().minusMonths(6),
                paattymispaiva = LocalDate.now().plusMonths(6),
                valtuuttajaOpintooikeus = erikoistuvaLaakari.getOpintooikeusKaytossa(),
                valtuutettu = kouluttaja,
                valtuutuksenLuontiaika = Instant.now(),
                valtuutuksenMuokkausaika = Instant.now()
            )
        )

        tyoskentelyjaksoRepository.saveAndFlush(
            TyoskentelyjaksoHelper.createEntity(
                em,
                erikoistuvaLaakari.kayttaja?.user
            )
        )

        val arvioitavanKokonaisuudenKategoria =
            ArvioitavanKokonaisuudenKategoriaHelper.createEntity(em, erikoisala1)
        arvioitavanKokonaisuudenKategoriaRepository.save(arvioitavanKokonaisuudenKategoria)

        val arvioitavaKokonaisuus1 = ArvioitavaKokonaisuusHelper.createEntity(em)
        arvioitavaKokonaisuusRepository.save(arvioitavaKokonaisuus1)

        val arvioitavaKokonaisuus2 = ArvioitavaKokonaisuusHelper.createEntity(em)
        arvioitavaKokonaisuusRepository.save(arvioitavaKokonaisuus2)

        arvioitavanKokonaisuudenKategoria.arvioitavatKokonaisuudet.add(arvioitavaKokonaisuus1)
        arvioitavanKokonaisuudenKategoria.arvioitavatKokonaisuudet.add(arvioitavaKokonaisuus2)

        // Vain korkein arvosana lasketaan
        suoritusarviointiRepository.save(
            SuoritusarviointiHelper.createEntity(
                em,
                erikoistuvaLaakari.kayttaja?.user,
                arviointiasteikonTaso = 2,
                arvioitavaKokonaisuus = arvioitavaKokonaisuus1
            )
        )
        suoritusarviointiRepository.save(
            SuoritusarviointiHelper.createEntity(
                em,
                erikoistuvaLaakari.kayttaja?.user,
                arviointiasteikonTaso = 4,
                arvioitavaKokonaisuus = arvioitavaKokonaisuus1
            )
        )

        suoritusarviointiRepository.save(
            SuoritusarviointiHelper.createEntity(
                em,
                erikoistuvaLaakari.kayttaja?.user,
                arviointiasteikonTaso = 3,
                arvioitavaKokonaisuus = arvioitavaKokonaisuus2
            )
        )

        suoritemerkintaRepository.save(SuoritemerkintaHelper.createEntity(em))

        seurantajaksoRepository.save(
            SeurantajaksoHelper.createEntity(
                erikoistuvaLaakari,
                kouluttaja
            )
        )

        val seurantajakso = SeurantajaksoHelper.createEntity(erikoistuvaLaakari, kouluttaja)
        seurantajakso.huolenaiheet = "huolenaiheet"
        seurantajaksoRepository.save(seurantajakso)

        val query = "?page=0&size=20&sort=opintooikeudenPaattymispaiva,asc&naytaPaattyneet=false"
        restEtusivuMockMvc.perform(get("/api/kouluttaja/etusivu/erikoistujien-seuranta" + query))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content").value(hasSize<Int>(1)))
            .andExpect(
                jsonPath("$.content[0].opintooikeusId").value(
                    erikoistuvaLaakari.getOpintooikeusKaytossa()?.id
                )
            )
            .andExpect(
                jsonPath("$.content[0].erikoistuvaLaakariEtuNimi").value(
                    erikoistuvaLaakari.kayttaja?.user?.firstName
                )
            )
            .andExpect(
                jsonPath("$.content[0].erikoistuvaLaakariSukuNimi").value(
                    erikoistuvaLaakari.kayttaja?.user?.lastName
                )
            )
            .andExpect(
                jsonPath("$.content[0].erikoistuvaLaakariSyntymaaika").value(
                    erikoistuvaLaakari.syntymaaika.toString()
                )
            )
            .andExpect(
                jsonPath("$.content[0].tyoskentelyjaksoTilastot.tyoskentelyaikaYhteensa").value(
                    5
                )
            )
            .andExpect(jsonPath("$.content[0].arviointienKeskiarvo").value(3.5))
            .andExpect(jsonPath("$.content[0].arviointienLkm").value(2))
            .andExpect(
                jsonPath("$.content[0].arvioitavienKokonaisuuksienLkm").value(
                    2
                )
            )
            .andExpect(jsonPath("$.content[0].seurantajaksotLkm").value(2))
            .andExpect(jsonPath("$.content[0].seurantajaksonHuoletLkm").value(1))
            .andExpect(jsonPath("$.content[0].suoritemerkinnatLkm").value(1))
            .andExpect(jsonPath("$.content[0].koejaksoTila").value(KoejaksoTila.EI_AKTIIVINEN.toString()))
            .andExpect(
                jsonPath("$.content[0].opintooikeudenMyontamispaiva").value(
                    erikoistuvaLaakari.getOpintooikeusKaytossa()?.opintooikeudenMyontamispaiva?.format(
                        DateTimeFormatter.ofPattern("yyyy-MM-dd")
                    )
                )
            )
            .andExpect(
                jsonPath("$.content[0].opintooikeudenPaattymispaiva").value(
                    erikoistuvaLaakari.getOpintooikeusKaytossa()?.opintooikeudenPaattymispaiva?.format(
                        DateTimeFormatter.ofPattern("yyyy-MM-dd")
                    )
                )
            )
            .andExpect(jsonPath("$.content[0].asetus").value(erikoistuvaLaakari.getOpintooikeusKaytossa()?.asetus?.nimi))
            .andExpect(jsonPath("$.content[0].erikoisala").value(erikoistuvaLaakari.getOpintooikeusKaytossa()?.erikoisala?.nimi))
    }

    @Test
    @Transactional
    fun testImpersonation() {
        initTest()

        val yliopisto1 =
            yliopistoRepository.save(Yliopisto(nimi = YliopistoEnum.HELSINGIN_YLIOPISTO))

        val erikoisala1 = erikoisalaRepository.findById(1).get()

        val yliopistoAndErikoisala = KayttajaYliopistoErikoisala(
            kayttaja = kouluttaja,
            yliopisto = yliopisto1,
            erikoisala = erikoisala1
        )
        kayttajaYliopistoErikoisalaRepository.save(yliopistoAndErikoisala)
        kouluttaja.yliopistotAndErikoisalat.add(yliopistoAndErikoisala)

        val erikoistuvaLaakari =
            ErikoistuvaLaakariHelper.createEntity(
                em,
                opintooikeudenPaattymispaiva = LocalDate.now().plusYears(5)
            )
        erikoistuvaLaakariRepository.save(erikoistuvaLaakari)

        kouluttajavaltuutusRepository.save(
            Kouluttajavaltuutus(
                alkamispaiva = LocalDate.now().minusMonths(6),
                paattymispaiva = LocalDate.now().plusMonths(6),
                valtuuttajaOpintooikeus = erikoistuvaLaakari.getOpintooikeusKaytossa(),
                valtuutettu = kouluttaja,
                valtuutuksenLuontiaika = Instant.now(),
                valtuutuksenMuokkausaika = Instant.now()
            )
        )

        restEtusivuMockMvc.perform(
            get("/api/login/impersonate?opintooikeusId=${erikoistuvaLaakari.getOpintooikeusKaytossa()?.id}")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isFound)
    }

    @Test
    @Transactional
    fun getKoejaksotEmptyList() {
        initTest()

        restEtusivuMockMvc.perform(
            get("/api/kouluttaja/etusivu/koejaksot")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").value(hasSize<Int>(0)))
    }

    @Test
    @Transactional
    fun getKoejaksotList() {
        initTest()

        val erikoistuvaLaakari1 =
            ErikoistuvaLaakariHelper.createEntity(
                em,
                opintooikeudenPaattymispaiva = LocalDate.now().plusYears(5)
            )
        erikoistuvaLaakariRepository.save(erikoistuvaLaakari1)

        val esimies = KayttajaHelper.createEntity(em)
        kayttajaRepository.save(esimies)

        aloituskeskusteluRepository.save(
            KoejaksonVaiheetHelper.createAloituskeskustelu(
                erikoistuvaLaakari1,
                kouluttaja,
                esimies
            )
        )

        val erikoistuvaLaakari2 =
            ErikoistuvaLaakariHelper.createEntity(
                em,
                opintooikeudenPaattymispaiva = LocalDate.now().plusYears(5)
            )
        erikoistuvaLaakariRepository.save(erikoistuvaLaakari2)

        valiarviointiRepository.save(
            KoejaksonVaiheetHelper.createValiarviointi(
                erikoistuvaLaakari2,
                kouluttaja,
                esimies
            )
        )

        val erikoistuvaLaakari3 =
            ErikoistuvaLaakariHelper.createEntity(
                em,
                opintooikeudenPaattymispaiva = LocalDate.now().plusYears(5)
            )
        erikoistuvaLaakariRepository.save(erikoistuvaLaakari3)

        kehittamistoimenpiteetRepository.save(
            KoejaksonVaiheetHelper.createKehittamistoimenpiteet(
                erikoistuvaLaakari3,
                kouluttaja,
                esimies
            )
        )

        val erikoistuvaLaakari4 =
            ErikoistuvaLaakariHelper.createEntity(
                em,
                opintooikeudenPaattymispaiva = LocalDate.now().plusYears(5)
            )
        erikoistuvaLaakariRepository.save(erikoistuvaLaakari4)

        loppukeskusteluRepository.save(
            KoejaksonVaiheetHelper.createLoppukeskustelu(
                erikoistuvaLaakari4,
                kouluttaja,
                esimies
            )
        )

        restEtusivuMockMvc.perform(
            get("/api/kouluttaja/etusivu/koejaksot")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").value(hasSize<Int>(4)))
    }

    @Test
    @Transactional
    fun getKoejaksotListHyvaksytty() {
        initTest()

        val erikoistuvaLaakari =
            ErikoistuvaLaakariHelper.createEntity(
                em,
                opintooikeudenPaattymispaiva = LocalDate.now().plusYears(5)
            )
        erikoistuvaLaakariRepository.save(erikoistuvaLaakari)

        val esimies = KayttajaHelper.createEntity(em)
        kayttajaRepository.save(esimies)

        val aloituskeskustelu = KoejaksonVaiheetHelper.createAloituskeskustelu(
            erikoistuvaLaakari,
            kouluttaja,
            esimies
        )
        aloituskeskustelu.lahikouluttajaHyvaksynyt = true
        aloituskeskustelu.lahikouluttajanKuittausaika = LocalDate.now()

        aloituskeskusteluRepository.save(aloituskeskustelu)

        restEtusivuMockMvc.perform(
            get("/api/kouluttaja/etusivu/koejaksot")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").value(hasSize<Int>(0)))
    }

    @Test
    @Transactional
    fun testImpersonationNotAllowed() {
        initTest()

        val yliopisto1 =
            yliopistoRepository.save(Yliopisto(nimi = YliopistoEnum.HELSINGIN_YLIOPISTO))

        val erikoisala1 = erikoisalaRepository.findById(1).get()

        val yliopistoAndErikoisala = KayttajaYliopistoErikoisala(
            kayttaja = kouluttaja,
            yliopisto = yliopisto1,
            erikoisala = erikoisala1
        )
        kayttajaYliopistoErikoisalaRepository.save(yliopistoAndErikoisala)
        kouluttaja.yliopistotAndErikoisalat.add(yliopistoAndErikoisala)

        val erikoistuvaLaakari =
            ErikoistuvaLaakariHelper.createEntity(
                em,
                opintooikeudenPaattymispaiva = LocalDate.now().plusYears(5)
            )
        erikoistuvaLaakariRepository.save(erikoistuvaLaakari)

        restEtusivuMockMvc.perform(
            get("/api/login/impersonate?opintooikeusId=${erikoistuvaLaakari.getOpintooikeusKaytossa()?.id}")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    @Transactional
    fun getSeurantajaksot() {
        initTest()

        val erikoistuvaLaakari =
            ErikoistuvaLaakariHelper.createEntity(
                em,
                opintooikeudenPaattymispaiva = LocalDate.now().plusYears(5)
            )
        erikoistuvaLaakariRepository.save(erikoistuvaLaakari)

        val seurantajakso1 = SeurantajaksoHelper.createEntity(erikoistuvaLaakari, kouluttaja)
        seurantajaksoRepository.save(seurantajakso1)

        val seurantajakso2 = SeurantajaksoHelper.createEntity(erikoistuvaLaakari, kouluttaja)
        seurantajaksoRepository.save(seurantajakso2)

        restEtusivuMockMvc.perform(
            get("/api/kouluttaja/etusivu/seurantajaksot")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").value(hasSize<Int>(2)))
    }

    @Test
    @Transactional
    fun getVanhenevatKatseluoikeudet() {
        initTest()
        initMockTime()

        // Katseluoikeus päättyy 28.2.1970
        val katseluoikeus = KouluttajavaltuutusHelper.createEntity(em, paattymispaiva = LocalDate.ofEpochDay(58L))
        em.persist(katseluoikeus)

        restEtusivuMockMvc.perform(
            get("/api/kouluttaja/etusivu/vanhenevat-katseluoikeudet")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").value(hasSize<Int>(1)))
    }

    @Test
    @Transactional
    fun testKatseluoikeusEndingOverOneMonth() {
        initTest()
        initMockTime()

        // Katseluoikeus päättyy 1.3.1970
        val katseluoikeus = KouluttajavaltuutusHelper.createEntity(em, paattymispaiva = LocalDate.ofEpochDay(59L))
        em.persist(katseluoikeus)

        restEtusivuMockMvc.perform(
            get("/api/kouluttaja/etusivu/vanhenevat-katseluoikeudet")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").value(hasSize<Int>(0)))
    }

    @Test
    @Transactional
    fun testKatseluoikeusAlreadyEnded() {
        initTest()
        initMockTime()

        // Katseluoikeus päättyy 1.3.1970
        val katseluoikeus = KouluttajavaltuutusHelper.createEntity(em, paattymispaiva = LocalDate.ofEpochDay(29L))
        em.persist(katseluoikeus)

        restEtusivuMockMvc.perform(
            get("/api/kouluttaja/etusivu/vanhenevat-katseluoikeudet")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").value(hasSize<Int>(0)))
    }

    @Test
    @Transactional
    fun testKatseluoikeusForAnotherKouluttajaNotReturned() {
        initTest()
        initMockTime()

        val anotherKouluttajaUser =
            KayttajaResourceWithMockUserIT.createEntity(authority = Authority(name = KOULUTTAJA))
        em.persist(anotherKouluttajaUser)
        val anotherKouluttaja = KayttajaHelper.createEntity(em, anotherKouluttajaUser)
        em.persist(anotherKouluttaja)

        // Katseluoikeus päättyy 28.2.1970
        val katseluoikeus = KouluttajavaltuutusHelper.createEntity(
            em,
            paattymispaiva = LocalDate.ofEpochDay(58L),
            valtuutettu = anotherKouluttaja
        )
        em.persist(katseluoikeus)

        restEtusivuMockMvc.perform(
            get("/api/kouluttaja/etusivu/vanhenevat-katseluoikeudet")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").value(hasSize<Int>(0)))
    }

    private fun initTest() {
        user = KayttajaResourceWithMockUserIT.createEntity()
        em.persist(user)
        em.flush()
        val userDetails = mapOf<String, List<Any>>()
        val authorities = listOf(SimpleGrantedAuthority(KOULUTTAJA))
        val authentication = Saml2Authentication(
            DefaultSaml2AuthenticatedPrincipal(user.id, userDetails),
            "test",
            authorities
        )
        TestSecurityContextHolder.getContext().authentication = authentication

        kouluttaja = KayttajaHelper.createEntity(em, user)
        em.persist(kouluttaja)
    }

    private fun initMockTime() {
        `when`(clock.instant()).thenReturn(Instant.ofEpochSecond(now))
        `when`(clock.zone).thenReturn(ZoneId.systemDefault())
    }
}
