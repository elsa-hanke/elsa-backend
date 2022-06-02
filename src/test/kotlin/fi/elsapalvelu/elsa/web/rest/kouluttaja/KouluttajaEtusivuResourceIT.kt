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
import org.mockito.MockitoAnnotations
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.persistence.EntityManager

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
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var restEtusivuMockMvc: MockMvc

    private lateinit var user: User

    private lateinit var kouluttaja: Kayttaja

    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)
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
            .andExpect(jsonPath("$.erikoistujienEteneminen").value(hasSize<Int>(0)))
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

        restEtusivuMockMvc.perform(get("/api/kouluttaja/etusivu/erikoistujien-seuranta"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.erikoistujienEteneminen").value(hasSize<Int>(1)))
            .andExpect(
                jsonPath("$.erikoistujienEteneminen[0].opintooikeusId").value(
                    erikoistuvaLaakari.getOpintooikeusKaytossa()?.id
                )
            )
            .andExpect(
                jsonPath("$.erikoistujienEteneminen[0].erikoistuvaLaakariEtuNimi").value(
                    erikoistuvaLaakari.kayttaja?.user?.firstName
                )
            )
            .andExpect(
                jsonPath("$.erikoistujienEteneminen[0].erikoistuvaLaakariSukuNimi").value(
                    erikoistuvaLaakari.kayttaja?.user?.lastName
                )
            )
            .andExpect(
                jsonPath("$.erikoistujienEteneminen[0].erikoistuvaLaakariSyntymaaika").value(
                    erikoistuvaLaakari.syntymaaika.toString()
                )
            )
            .andExpect(
                jsonPath("$.erikoistujienEteneminen[0].tyoskentelyjaksoTilastot.tyoskentelyaikaYhteensa").value(
                    5
                )
            )
            .andExpect(jsonPath("$.erikoistujienEteneminen[0].arviointienKeskiarvo").value(3.5))
            .andExpect(jsonPath("$.erikoistujienEteneminen[0].arviointienLkm").value(2))
            .andExpect(
                jsonPath("$.erikoistujienEteneminen[0].arvioitavienKokonaisuuksienLkm").value(
                    2
                )
            )
            .andExpect(jsonPath("$.erikoistujienEteneminen[0].seurantajaksotLkm").value(2))
            .andExpect(jsonPath("$.erikoistujienEteneminen[0].seurantajaksonHuoletLkm").value(1))
            .andExpect(jsonPath("$.erikoistujienEteneminen[0].suoritemerkinnatLkm").value(1))
            .andExpect(jsonPath("$.erikoistujienEteneminen[0].koejaksoTila").value(KoejaksoTila.EI_AKTIIVINEN.toString()))
            .andExpect(
                jsonPath("$.erikoistujienEteneminen[0].opintooikeudenMyontamispaiva").value(
                    erikoistuvaLaakari.getOpintooikeusKaytossa()?.opintooikeudenMyontamispaiva?.format(
                        DateTimeFormatter.ofPattern("yyyy-MM-dd")
                    )
                )
            )
            .andExpect(
                jsonPath("$.erikoistujienEteneminen[0].opintooikeudenPaattymispaiva").value(
                    erikoistuvaLaakari.getOpintooikeusKaytossa()?.opintooikeudenPaattymispaiva?.format(
                        DateTimeFormatter.ofPattern("yyyy-MM-dd")
                    )
                )
            )
            .andExpect(jsonPath("$.erikoistujienEteneminen[0].asetus").value(erikoistuvaLaakari.getOpintooikeusKaytossa()?.asetus?.nimi))
            .andExpect(jsonPath("$.erikoistujienEteneminen[0].erikoisala").value(erikoistuvaLaakari.getOpintooikeusKaytossa()?.erikoisala?.nimi))
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

    fun initTest(createVastuuhenkilonArvio: Boolean? = true) {
        user = KayttajaResourceWithMockUserIT.createEntity()
        em.persist(user)
        em.flush()
        val userDetails = mapOf<String, List<Any>>(
        )
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
}
