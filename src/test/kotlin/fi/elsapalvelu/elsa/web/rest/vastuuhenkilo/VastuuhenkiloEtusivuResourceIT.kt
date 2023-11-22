package fi.elsapalvelu.elsa.web.rest.vastuuhenkilo

import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.domain.enumeration.ErikoisalaTyyppi
import fi.elsapalvelu.elsa.domain.enumeration.VastuuhenkilonTehtavatyyppiEnum
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.repository.*
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI_IMPERSONATED
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.service.dto.enumeration.KoejaksoTila
import fi.elsapalvelu.elsa.service.mapper.TyoskentelyjaksoMapper
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.findAll
import fi.elsapalvelu.elsa.web.rest.helpers.*
import org.hamcrest.collection.IsCollectionWithSize.hasSize
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.MockitoAnnotations
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.security.web.authentication.switchuser.SwitchUserGrantedAuthority
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import jakarta.persistence.EntityManager

@AutoConfigureMockMvc
@SpringBootTest(classes = [ElsaBackendApp::class])
class VastuuhenkiloEtusivuResourceIT {

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
    private lateinit var koejaksonAloituskeskusteluRepository: KoejaksonAloituskeskusteluRepository

    @Autowired
    private lateinit var koejaksonVastuuhenkilonArvioRepository: KoejaksonVastuuhenkilonArvioRepository

    @Autowired
    private lateinit var paivakirjamerkintaRepository: PaivakirjamerkintaRepository

    @Autowired
    private lateinit var koulutussuunnitelmaRepository: KoulutussuunnitelmaRepository

    @Autowired
    private lateinit var koulutussopimusRepository: KoejaksonKoulutussopimusRepository

    @Autowired
    private lateinit var vastuuhenkilonArvioRepository: KoejaksonVastuuhenkilonArvioRepository

    @Autowired
    private lateinit var kayttajaRepository: KayttajaRepository

    @Autowired
    private lateinit var tyoskentelyjaksoMapper: TyoskentelyjaksoMapper

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var restEtusivuMockMvc: MockMvc

    private lateinit var user: User

    private lateinit var vastuuhenkilo: Kayttaja

    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    @Transactional
    fun getErikoistujienSeurantaEmptyList() {
        initTest()

        val yliopisto1 =
            yliopistoRepository.save(Yliopisto(nimi = YliopistoEnum.TAMPEREEN_YLIOPISTO))

        val erikoisala1 = erikoisalaRepository.save(
            Erikoisala(
                nimi = "Erikoisala 1",
                tyyppi = ErikoisalaTyyppi.LAAKETIEDE
            )
        )

        val yliopistoAndErikoisala = KayttajaYliopistoErikoisala(
            kayttaja = vastuuhenkilo,
            yliopisto = yliopisto1,
            erikoisala = erikoisala1
        )
        kayttajaYliopistoErikoisalaRepository.save(yliopistoAndErikoisala)
        vastuuhenkilo.yliopistotAndErikoisalat.add(yliopistoAndErikoisala)

        restEtusivuMockMvc.perform(get("/api/vastuuhenkilo/etusivu/erikoistujien-seuranta-rajaimet"))
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
            yliopistoRepository.save(Yliopisto(nimi = YliopistoEnum.TAMPEREEN_YLIOPISTO))

        val erikoisala1 = erikoisalaRepository.findById(1).get()

        val yliopistoAndErikoisala = KayttajaYliopistoErikoisala(
            kayttaja = vastuuhenkilo,
            yliopisto = yliopisto1,
            erikoisala = erikoisala1
        )
        kayttajaYliopistoErikoisalaRepository.save(yliopistoAndErikoisala)
        vastuuhenkilo.yliopistotAndErikoisalat.add(yliopistoAndErikoisala)

        val erikoistuvaLaakari =
            ErikoistuvaLaakariHelper.createEntity(
                em,
                erikoisala = erikoisala1,
                opintooikeudenPaattymispaiva = LocalDate.now().plusYears(5)
            )
        erikoistuvaLaakariRepository.save(erikoistuvaLaakari)

        tyoskentelyjaksoRepository.saveAndFlush(
            TyoskentelyjaksoHelper.createEntity(
                em,
                erikoistuvaLaakari.kayttaja?.user
            )
        )

        val arvioitavanKokonaisuudenKategoria =
            ArvioitavanKokonaisuudenKategoriaHelper.createEntity(em, erikoisala1)
        arvioitavanKokonaisuudenKategoriaRepository.save(arvioitavanKokonaisuudenKategoria)

        val arvioitavaKokonaisuus1 =
            ArvioitavaKokonaisuusHelper.createEntity(
                em,
                existingKategoria = arvioitavanKokonaisuudenKategoria
            )
        arvioitavaKokonaisuusRepository.save(arvioitavaKokonaisuus1)

        val arvioitavaKokonaisuus2 =
            ArvioitavaKokonaisuusHelper.createEntity(
                em,
                existingKategoria = arvioitavanKokonaisuudenKategoria
            )
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
                vastuuhenkilo
            )
        )

        val seurantajakso = SeurantajaksoHelper.createEntity(erikoistuvaLaakari, vastuuhenkilo)
        seurantajakso.huolenaiheet = "huolenaiheet"
        seurantajaksoRepository.save(seurantajakso)

        restEtusivuMockMvc.perform(get("/api/vastuuhenkilo/etusivu/erikoistujien-seuranta"))
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
    fun getErikoistujienSeurantaKoejaksoAloitettu() {
        initTest()

        val yliopisto1 =
            yliopistoRepository.save(Yliopisto(nimi = YliopistoEnum.TAMPEREEN_YLIOPISTO))

        val erikoisala1 = erikoisalaRepository.findById(1).get()

        val yliopistoAndErikoisala = KayttajaYliopistoErikoisala(
            kayttaja = vastuuhenkilo,
            yliopisto = yliopisto1,
            erikoisala = erikoisala1
        )
        kayttajaYliopistoErikoisalaRepository.save(yliopistoAndErikoisala)
        vastuuhenkilo.yliopistotAndErikoisalat.add(yliopistoAndErikoisala)

        val erikoistuvaLaakari =
            ErikoistuvaLaakariHelper.createEntity(
                em,
                opintooikeudenPaattymispaiva = LocalDate.now().plusYears(5)
            )
        erikoistuvaLaakariRepository.save(erikoistuvaLaakari)

        val kayttaja = KayttajaHelper.createEntity(em)
        em.persist(kayttaja)

        koejaksonAloituskeskusteluRepository.save(
            KoejaksonVaiheetHelper.createAloituskeskustelu(
                erikoistuvaLaakari,
                kayttaja,
                vastuuhenkilo
            )
        )

        restEtusivuMockMvc.perform(get("/api/vastuuhenkilo/etusivu/erikoistujien-seuranta"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content").value(hasSize<Int>(1)))
            .andExpect(jsonPath("$.content[0].koejaksoTila").value(KoejaksoTila.ODOTTAA_HYVAKSYNTAA.toString()))
    }

    @Test
    @Transactional
    fun getErikoistujienSeurantaKoejaksoHyvaksytty() {
        initTest()

        val yliopisto1 =
            yliopistoRepository.save(Yliopisto(nimi = YliopistoEnum.TAMPEREEN_YLIOPISTO))

        val erikoisala1 = erikoisalaRepository.findById(1).get()

        val yliopistoAndErikoisala = KayttajaYliopistoErikoisala(
            kayttaja = vastuuhenkilo,
            yliopisto = yliopisto1,
            erikoisala = erikoisala1
        )
        kayttajaYliopistoErikoisalaRepository.save(yliopistoAndErikoisala)
        vastuuhenkilo.yliopistotAndErikoisalat.add(yliopistoAndErikoisala)

        val erikoistuvaLaakari =
            ErikoistuvaLaakariHelper.createEntity(
                em,
                opintooikeudenPaattymispaiva = LocalDate.now().plusYears(5)
            )
        erikoistuvaLaakariRepository.save(erikoistuvaLaakari)

        koejaksonVastuuhenkilonArvioRepository.save(
            KoejaksonVastuuhenkilonArvio(
                opintooikeus = erikoistuvaLaakari.getOpintooikeusKaytossa(),
                muokkauspaiva = LocalDate.now(),
                vastuuhenkilo = vastuuhenkilo,
                koejaksoHyvaksytty = true
            )
        )

        restEtusivuMockMvc.perform(get("/api/vastuuhenkilo/etusivu/erikoistujien-seuranta"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content").value(hasSize<Int>(1)))
            .andExpect(jsonPath("$.content[0].koejaksoTila").value(KoejaksoTila.HYVAKSYTTY.toString()))
    }

    @Test
    @Transactional
    fun getErikoistujienSeurantaOpintooikeudenVoimassaolo() {
        initTest()

        val yliopisto1 =
            yliopistoRepository.save(Yliopisto(nimi = YliopistoEnum.TAMPEREEN_YLIOPISTO))

        val erikoisala1 = erikoisalaRepository.findById(1).get()

        val yliopistoAndErikoisala = KayttajaYliopistoErikoisala(
            kayttaja = vastuuhenkilo,
            yliopisto = yliopisto1,
            erikoisala = erikoisala1
        )
        kayttajaYliopistoErikoisalaRepository.save(yliopistoAndErikoisala)
        vastuuhenkilo.yliopistotAndErikoisalat.add(yliopistoAndErikoisala)

        val erikoistuvaLaakari =
            ErikoistuvaLaakariHelper.createEntity(
                em,
                opintooikeudenPaattymispaiva = LocalDate.now().plusYears(5)
            )
        erikoistuvaLaakariRepository.save(erikoistuvaLaakari)

        erikoistuvaLaakariRepository.save(
            ErikoistuvaLaakariHelper.createEntity(
                em,
                opintooikeudenPaattymispaiva = LocalDate.now().minusDays(1)
            )
        )

        erikoistuvaLaakariRepository.save(
            ErikoistuvaLaakariHelper.createEntity(
                em,
                opintooikeudenAlkamispaiva = LocalDate.now().plusDays(1),
                opintooikeudenPaattymispaiva = LocalDate.now().plusYears(5)
            )
        )

        restEtusivuMockMvc.perform(get("/api/vastuuhenkilo/etusivu/erikoistujien-seuranta"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content").value(hasSize<Int>(2)))
    }

    @Test
    @Transactional
    fun getErikoistujienSeurantaUseaErikoisala() {
        initTest()

        val yliopisto1 =
            yliopistoRepository.save(Yliopisto(nimi = YliopistoEnum.TAMPEREEN_YLIOPISTO))

        val erikoisala1 = erikoisalaRepository.findById(1).get()
        val erikoisala2 = erikoisalaRepository.findById(2).get()

        val yliopistoAndErikoisala1 = KayttajaYliopistoErikoisala(
            kayttaja = vastuuhenkilo,
            yliopisto = yliopisto1,
            erikoisala = erikoisala1
        )
        kayttajaYliopistoErikoisalaRepository.save(yliopistoAndErikoisala1)
        vastuuhenkilo.yliopistotAndErikoisalat.add(yliopistoAndErikoisala1)
        val yliopistoAndErikoisala2 = KayttajaYliopistoErikoisala(
            kayttaja = vastuuhenkilo,
            yliopisto = yliopisto1,
            erikoisala = erikoisala2
        )
        kayttajaYliopistoErikoisalaRepository.save(yliopistoAndErikoisala2)
        vastuuhenkilo.yliopistotAndErikoisalat.add(yliopistoAndErikoisala2)

        val erikoistuvaLaakari =
            ErikoistuvaLaakariHelper.createEntity(
                em,
                opintooikeudenPaattymispaiva = LocalDate.now().plusYears(5)
            )
        erikoistuvaLaakariRepository.save(erikoistuvaLaakari)

        erikoistuvaLaakariRepository.save(
            ErikoistuvaLaakariHelper.createEntity(
                em,
                opintooikeudenPaattymispaiva = LocalDate.now().plusYears(3),
                erikoisala = erikoisala2
            )
        )

        val query = "?page=0&size=20&sort=opintooikeudenPaattymispaiva,asc"
        restEtusivuMockMvc.perform(get("/api/vastuuhenkilo/etusivu/erikoistujien-seuranta" + query))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content").value(hasSize<Int>(2)))
    }

    @Test
    @Transactional
    fun testImpersonation() {
        initTest()

        val yliopisto1 =
            yliopistoRepository.save(Yliopisto(nimi = YliopistoEnum.TAMPEREEN_YLIOPISTO))

        val erikoisala1 = erikoisalaRepository.findById(1).get()

        val yliopistoAndErikoisala = KayttajaYliopistoErikoisala(
            kayttaja = vastuuhenkilo,
            yliopisto = yliopisto1,
            erikoisala = erikoisala1
        )
        kayttajaYliopistoErikoisalaRepository.save(yliopistoAndErikoisala)
        vastuuhenkilo.yliopistotAndErikoisalat.add(yliopistoAndErikoisala)

        val erikoistuvaLaakari =
            ErikoistuvaLaakariHelper.createEntity(
                em,
                opintooikeudenPaattymispaiva = LocalDate.now().plusYears(5)
            )
        erikoistuvaLaakariRepository.save(erikoistuvaLaakari)

        val tyoskentelyjakso =
            TyoskentelyjaksoHelper.createEntity(em, erikoistuvaLaakari.kayttaja?.user)
        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        restEtusivuMockMvc.perform(
            get("/api/login/impersonate?opintooikeusId=${erikoistuvaLaakari.getOpintooikeusKaytossa()?.id}")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isFound)

        // Päivitetään Security contextiin impersonoitu käyttäjä
        val currentAuthentication: Authentication =
            TestSecurityContextHolder.getContext().authentication
        val switchAuthority: GrantedAuthority = SwitchUserGrantedAuthority(
            ERIKOISTUVA_LAAKARI_IMPERSONATED, currentAuthentication
        )
        val currentPrincipal = currentAuthentication.principal as Saml2AuthenticatedPrincipal
        val newPrincipal = DefaultSaml2AuthenticatedPrincipal(
            erikoistuvaLaakari.kayttaja?.user?.id,
            mapOf(
                "urn:oid:2.5.4.42" to listOf(erikoistuvaLaakari.kayttaja?.user?.firstName),
                "urn:oid:2.5.4.4" to listOf(erikoistuvaLaakari.kayttaja?.user?.lastName),
                "nameID" to currentPrincipal.attributes["nameID"],
                "nameIDFormat" to currentPrincipal.attributes["nameIDFormat"],
                "nameIDQualifier" to currentPrincipal.attributes["nameIDQualifier"],
                "nameIDSPQualifier" to currentPrincipal.attributes["nameIDSPQualifier"],
                "opintooikeusId" to listOf(erikoistuvaLaakari.getOpintooikeusKaytossa()?.id)
            )
        )
        val context = TestSecurityContextHolder.getContext()
        context.authentication = Saml2Authentication(
            newPrincipal,
            (currentAuthentication as Saml2Authentication).saml2Response,
            listOf(switchAuthority)
        )
        TestSecurityContextHolder.setContext(context)

        // GET kutsut sallittuja
        restEtusivuMockMvc.perform(get("/api/erikoistuva-laakari/tyoskentelyjaksot"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").value(hasSize<Int>(1)))

        val tyoskentelyjaksoDTO = tyoskentelyjaksoMapper.toDto(tyoskentelyjakso)
        val updatedTyoskentelyjaksoJson = objectMapper.writeValueAsString(tyoskentelyjaksoDTO)

        // Muut kutsut estetty
        restEtusivuMockMvc.perform(
            MockMvcRequestBuilders.put("/api/erikoistuva-laakari/tyoskentelyjaksot")
                .param("tyoskentelyjaksoJson", updatedTyoskentelyjaksoJson)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andExpect(status().isForbidden)

        // Yksityisiä päiväkirjamerkintöjä ei palauteta
        val paivakirjamerkinta1 =
            PaivakirjamerkintaHelper.createEntity(em, erikoistuvaLaakari.kayttaja?.user)
        paivakirjamerkintaRepository.saveAndFlush(paivakirjamerkinta1)
        val paivakirjamerkinta2 =
            PaivakirjamerkintaHelper.createEntity(em, erikoistuvaLaakari.kayttaja?.user)
        paivakirjamerkinta2.yksityinen = true
        paivakirjamerkintaRepository.saveAndFlush(paivakirjamerkinta2)

        restEtusivuMockMvc.perform(get("/api/erikoistuva-laakari/paivakirjamerkinnat"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content").value(hasSize<Int>(1)))
            .andExpect(jsonPath("$.content[0].yksityinen").value(false))

        // Yksityisiä koulutussuunnitelman kenttiä ei palauteta
        val koulutussuunnitelma =
            KoulutussuunnitelmaHelper.createEntity(em, erikoistuvaLaakari.kayttaja?.user)
        koulutussuunnitelmaRepository.saveAndFlush(koulutussuunnitelma)

        restEtusivuMockMvc.perform(get("/api/erikoistuva-laakari/koulutussuunnitelma"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.motivaatiokirje").value(KoulutussuunnitelmaHelper.DEFAULT_MOTIVAATIOKIRJE))
            .andExpect(
                jsonPath("$.motivaatiokirjeYksityinen").value(
                    KoulutussuunnitelmaHelper.DEFAULT_MOTIVAATIOKIRJE_YKSITYINEN
                )
            )
            .andExpect(jsonPath("$.opiskeluJaTyohistoria").value(KoulutussuunnitelmaHelper.DEFAULT_OPISKELU_JA_TYOHISTORIA))
            .andExpect(
                jsonPath("$.opiskeluJaTyohistoriaYksityinen").value(
                    KoulutussuunnitelmaHelper.DEFAULT_OPISKELU_JA_TYOHISTORIA_YKSITYINEN
                )
            )
            .andExpect(jsonPath("$.vahvuudet").value(KoulutussuunnitelmaHelper.DEFAULT_VAHVUUDET))
            .andExpect(jsonPath("$.vahvuudetYksityinen").value(KoulutussuunnitelmaHelper.DEFAULT_VAHVUUDET_YKSITYINEN))
            .andExpect(jsonPath("$.tulevaisuudenVisiointi").value(KoulutussuunnitelmaHelper.DEFAULT_TULEVAISUUDEN_VISIOINTI))
            .andExpect(
                jsonPath("$.tulevaisuudenVisiointiYksityinen").value(
                    KoulutussuunnitelmaHelper.DEFAULT_TULEVAISUUDEN_VISIOINTI_YKSITYINEN
                )
            )
            .andExpect(jsonPath("$.osaamisenKartuttaminen").value(KoulutussuunnitelmaHelper.DEFAULT_OSAAMISEN_KARTUTTAMINEN))
            .andExpect(
                jsonPath("$.osaamisenKartuttaminenYksityinen").value(
                    KoulutussuunnitelmaHelper.DEFAULT_OSAAMISEN_KARTUTTAMINEN_YKSITYINEN
                )
            )
            .andExpect(jsonPath("$.elamankentta").value(KoulutussuunnitelmaHelper.DEFAULT_ELAMANKENTTA))
            .andExpect(jsonPath("$.elamankenttaYksityinen").value(KoulutussuunnitelmaHelper.DEFAULT_ELAMANKENTTA_YKSITYINEN))

        koulutussuunnitelma.elamankenttaYksityinen = true
        koulutussuunnitelma.vahvuudetYksityinen = true
        koulutussuunnitelma.opiskeluJaTyohistoriaYksityinen = true
        koulutussuunnitelma.osaamisenKartuttaminenYksityinen = true
        koulutussuunnitelma.tulevaisuudenVisiointiYksityinen = true
        koulutussuunnitelmaRepository.saveAndFlush(koulutussuunnitelma)

        restEtusivuMockMvc.perform(get("/api/erikoistuva-laakari/koulutussuunnitelma"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.opiskeluJaTyohistoria").isEmpty)
            .andExpect(jsonPath("$.vahvuudet").isEmpty)
            .andExpect(jsonPath("$.tulevaisuudenVisiointi").isEmpty)
            .andExpect(jsonPath("$.osaamisenKartuttaminen").isEmpty)
            .andExpect(jsonPath("$.elamankentta").isEmpty)
    }

    @Test
    @Transactional
    fun testImpersonationNotAllowed() {
        initTest()

        val yliopisto1 =
            yliopistoRepository.save(Yliopisto(nimi = YliopistoEnum.TAMPEREEN_YLIOPISTO))

        // Eri erikoisala
        val erikoisala1 = erikoisalaRepository.findById(2).get()

        val yliopistoAndErikoisala = KayttajaYliopistoErikoisala(
            kayttaja = vastuuhenkilo,
            yliopisto = yliopisto1,
            erikoisala = erikoisala1
        )
        kayttajaYliopistoErikoisalaRepository.save(yliopistoAndErikoisala)
        vastuuhenkilo.yliopistotAndErikoisalat.add(yliopistoAndErikoisala)

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
    fun getKoejaksotEmptyList() {
        initTest()

        restEtusivuMockMvc.perform(
            get("/api/vastuuhenkilo/etusivu/koejaksot")
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

        val erikoistuvaLaakari =
            ErikoistuvaLaakariHelper.createEntity(
                em,
                opintooikeudenPaattymispaiva = LocalDate.now().plusYears(5)
            )
        erikoistuvaLaakariRepository.save(erikoistuvaLaakari)

        val kouluttaja = KayttajaHelper.createEntity(em)
        kayttajaRepository.save(kouluttaja)

        val vastuualue = em.findAll(VastuuhenkilonTehtavatyyppi::class)
            .first { it.nimi == VastuuhenkilonTehtavatyyppiEnum.KOEJAKSOSOPIMUSTEN_JA_KOEJAKSOJEN_HYVAKSYMINEN }
        val yliopistoAndErikoisala = KayttajaYliopistoErikoisala(
            kayttaja = vastuuhenkilo,
            yliopisto = erikoistuvaLaakari.opintooikeudet.first().yliopisto,
            erikoisala = erikoistuvaLaakari.opintooikeudet.first().erikoisala,
            vastuuhenkilonTehtavat = mutableSetOf(vastuualue)
        )
        kayttajaYliopistoErikoisalaRepository.save(yliopistoAndErikoisala)
        vastuuhenkilo.yliopistotAndErikoisalat.add(yliopistoAndErikoisala)

        val koulutussopimus =
            KoejaksonVaiheetHelper.createKoulutussopimus(erikoistuvaLaakari, vastuuhenkilo)
        koulutussopimus.lahetetty = true
        koulutussopimus.kouluttajat =
            mutableSetOf(
                KoejaksonVaiheetHelper.createKoulutussopimuksenKouluttaja(
                    koulutussopimus,
                    kouluttaja
                )
            )
        koulutussopimusRepository.save(koulutussopimus)

        val vastuuhenkilonArvio =
            KoejaksonVaiheetHelper.createVastuuhenkilonArvio(erikoistuvaLaakari, vastuuhenkilo)
        vastuuhenkilonArvio.virkailijaHyvaksynyt = true
        vastuuhenkilonArvio.virkailijanKuittausaika = LocalDate.now()
        vastuuhenkilonArvioRepository.save(vastuuhenkilonArvio)

        restEtusivuMockMvc.perform(
            get("/api/vastuuhenkilo/etusivu/koejaksot")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").value(hasSize<Int>(2)))
    }

    @Test
    @Transactional
    fun getKoejaksotListWrongState() {
        initTest()

        val erikoistuvaLaakari =
            ErikoistuvaLaakariHelper.createEntity(
                em,
                opintooikeudenPaattymispaiva = LocalDate.now().plusYears(5)
            )
        erikoistuvaLaakariRepository.save(erikoistuvaLaakari)

        val kouluttaja = KayttajaHelper.createEntity(em)
        kayttajaRepository.save(kouluttaja)

        val koulutussopimus =
            KoejaksonVaiheetHelper.createKoulutussopimus(erikoistuvaLaakari, vastuuhenkilo)
        koulutussopimus.lahetetty = true
        koulutussopimus.kouluttajat =
            mutableSetOf(
                KoejaksonVaiheetHelper.createKoulutussopimuksenKouluttaja(
                    koulutussopimus,
                    kouluttaja
                )
            )
        koulutussopimus.vastuuhenkiloHyvaksynyt = true
        koulutussopimus.vastuuhenkilonKuittausaika = LocalDate.now()
        koulutussopimusRepository.save(koulutussopimus)

        val vastuuhenkilonArvio =
            KoejaksonVaiheetHelper.createVastuuhenkilonArvio(erikoistuvaLaakari, vastuuhenkilo)
        vastuuhenkilonArvio.virkailijaHyvaksynyt = false
        vastuuhenkilonArvioRepository.save(vastuuhenkilonArvio)

        restEtusivuMockMvc.perform(
            get("/api/vastuuhenkilo/etusivu/koejaksot")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").value(hasSize<Int>(0)))
    }

    fun initTest(createVastuuhenkilonArvio: Boolean? = true) {
        user = KayttajaResourceWithMockUserIT.createEntity(authority = Authority(VASTUUHENKILO))
        em.persist(user)
        em.flush()
        val userDetails = mapOf<String, List<Any>>(
        )
        val authorities = listOf(SimpleGrantedAuthority(VASTUUHENKILO))
        val authentication = Saml2Authentication(
            DefaultSaml2AuthenticatedPrincipal(user.id, userDetails),
            "test",
            authorities
        )
        TestSecurityContextHolder.getContext().authentication = authentication

        vastuuhenkilo = KayttajaHelper.createEntity(em, user)
        em.persist(vastuuhenkilo)
    }
}
