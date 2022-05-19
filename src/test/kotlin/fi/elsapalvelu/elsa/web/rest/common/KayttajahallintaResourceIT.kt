package fi.elsapalvelu.elsa.web.rest.common

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.repository.ErikoisalaRepository
import fi.elsapalvelu.elsa.security.OPINTOHALLINNON_VIRKAILIJA
import fi.elsapalvelu.elsa.security.TEKNINEN_PAAKAYTTAJA
import fi.elsapalvelu.elsa.service.dto.kayttajahallinta.KayttajahallintaErikoistuvaLaakariDTO
import fi.elsapalvelu.elsa.web.rest.convertObjectToJsonBytes
import fi.elsapalvelu.elsa.web.rest.findAll
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoisalaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper.Companion.DEFAULT_YLIOPISTO
import fi.elsapalvelu.elsa.web.rest.helpers.KayttajaHelper
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.LocalDate
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import javax.persistence.EntityManager

private const val tekninenPaakayttaja = "tekninen-paakayttaja"
private const val virkailija = "virkailija"

@AutoConfigureMockMvc
@SpringBootTest(classes = [ElsaBackendApp::class])
class KayttajahallintaResourceIT {

    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var erikoisalaRepository: ErikoisalaRepository

    @Autowired
    private lateinit var restMockMvc: MockMvc

    private lateinit var yliopisto: Yliopisto

    @BeforeEach
    fun setup() {
        yliopisto = Yliopisto(nimi = DEFAULT_YLIOPISTO)
        em.persist(yliopisto)
    }

    @ParameterizedTest
    @ValueSource(strings = [tekninenPaakayttaja, virkailija])
    @Transactional
    fun getKayttaja(rolePath: String) {
        initTest(getRole(rolePath))

        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, yliopisto = yliopisto)
        em.persist(erikoistuvaLaakari)
        em.flush()

        val id = erikoistuvaLaakari.kayttaja?.id

        restMockMvc.perform(get("/api/$rolePath/kayttajat/$id"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.user.id").exists())
            .andExpect(jsonPath("$.kayttaja.id").value(id))
            .andExpect(jsonPath("$.erikoistuvaLaakari.id").exists())
    }

    @ParameterizedTest
    @ValueSource(strings = [tekninenPaakayttaja, virkailija])
    @Transactional
    fun getNonExistingKayttaja(rolePath: String) {
        initTest(getRole(rolePath))

        val id = count.incrementAndGet()

        restMockMvc.perform(get("/api/$rolePath/kayttajat/$id"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.user").isEmpty)
            .andExpect(jsonPath("$.kayttaja").isEmpty)
            .andExpect(jsonPath("$.erikoistuvaLaakari").isEmpty)
    }

    @ParameterizedTest
    @ValueSource(strings = [tekninenPaakayttaja, virkailija])
    @Transactional
    fun getKayttajaForm(rolePath: String) {
        initTest(getRole(rolePath))

        val erikoisalatCountByLiittynytElsaan = erikoisalaRepository.findAllByLiittynytElsaanTrue().count()

        restMockMvc.perform(get("/api/$rolePath/kayttaja-lomake"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.yliopistot").value(Matchers.hasSize<Any>(1)))
            .andExpect(jsonPath("$.erikoisalat").value(Matchers.hasSize<Any>(erikoisalatCountByLiittynytElsaan)))
    }

    @ParameterizedTest
    @ValueSource(strings = [tekninenPaakayttaja, virkailija])
    @Transactional
    fun getErikoistuvatLaakarit(rolePath: String) {
        initTest(getRole(rolePath))

        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, yliopisto = yliopisto)
        em.persist(erikoistuvaLaakari)
        em.flush()

        val yliopisto2 = Yliopisto(nimi = YliopistoEnum.HELSINGIN_YLIOPISTO)
        em.persist(yliopisto2)

        val erikoistuvaLaakari2 = ErikoistuvaLaakariHelper.createEntity(em, yliopisto = yliopisto2)
        em.persist(erikoistuvaLaakari2)
        em.flush()

        // Virkailijalle listataan vain saman yliopiston erikoistujat.
        val expectedSize = if (rolePath == tekninenPaakayttaja) 2 else 1

        restMockMvc.perform(get("/api/$rolePath/erikoistuvat-laakarit"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content").value(Matchers.hasSize<Any>(expectedSize)))
            .andExpect(jsonPath("$.content[0].kayttajaId").exists())
    }

    @ParameterizedTest
    @ValueSource(strings = [tekninenPaakayttaja, virkailija])
    @Transactional
    fun postErikoistuvaLaakari(rolePath: String) {
        initTest(getRole(rolePath))

        val yliopisto = Yliopisto(nimi = DEFAULT_YLIOPISTO)
        em.persist(yliopisto)

        val erikoisala = ErikoisalaHelper.createEntity()
        em.persist(erikoisala)

        val asetus = em.findAll(Asetus::class).get(0)

        val opintoopas = em.findAll(Opintoopas::class).get(0)

        val kayttajahallintaErikoistuvaLaakariDTO = createKayttajahallintaErikoistuvaLaakariDTO()
        kayttajahallintaErikoistuvaLaakariDTO.yliopistoId = yliopisto.id
        kayttajahallintaErikoistuvaLaakariDTO.erikoisalaId = erikoisala.id
        kayttajahallintaErikoistuvaLaakariDTO.asetusId = asetus.id
        kayttajahallintaErikoistuvaLaakariDTO.opintoopasId = opintoopas.id

        restMockMvc.perform(
            post("/api/$rolePath/erikoistuvat-laakarit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaErikoistuvaLaakariDTO))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andExpect(status().isCreated)
    }

    @ParameterizedTest
    @ValueSource(strings = [tekninenPaakayttaja, virkailija])
    @Transactional
    fun postErikoistuvaLaakariWithExistingEmail(rolePath: String) {
        initTest(getRole(rolePath))

        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, yliopisto = yliopisto)
        em.persist(erikoistuvaLaakari)
        em.flush()

        val yliopisto = Yliopisto(nimi = DEFAULT_YLIOPISTO)
        em.persist(yliopisto)

        val erikoisala = ErikoisalaHelper.createEntity()
        em.persist(erikoisala)

        val kayttajahallintaErikoistuvaLaakariDTO = createKayttajahallintaErikoistuvaLaakariDTO()
        kayttajahallintaErikoistuvaLaakariDTO.yliopistoId = yliopisto.id
        kayttajahallintaErikoistuvaLaakariDTO.erikoisalaId = erikoisala.id
        kayttajahallintaErikoistuvaLaakariDTO.sahkopostiosoite = erikoistuvaLaakari.kayttaja?.user?.email

        restMockMvc.perform(
            post("/api/$rolePath/erikoistuvat-laakarit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaErikoistuvaLaakariDTO))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andExpect(status().isBadRequest)
    }

    @ParameterizedTest
    @ValueSource(strings = [tekninenPaakayttaja, virkailija])
    @Transactional
    fun postErikoistuvaLaakariWithoutValidYliopisto(rolePath: String) {
        initTest(getRole(rolePath))

        val erikoisala = ErikoisalaHelper.createEntity()
        em.persist(erikoisala)

        val kayttajahallintaErikoistuvaLaakariDTO = createKayttajahallintaErikoistuvaLaakariDTO()
        kayttajahallintaErikoistuvaLaakariDTO.yliopistoId = count.incrementAndGet()
        kayttajahallintaErikoistuvaLaakariDTO.erikoisalaId = erikoisala.id

        restMockMvc.perform(
            post("/api/$rolePath/erikoistuvat-laakarit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaErikoistuvaLaakariDTO))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andExpect(status().isBadRequest)
    }

    @ParameterizedTest
    @ValueSource(strings = [tekninenPaakayttaja, virkailija])
    @Transactional
    fun postErikoistuvaLaakariWithoutValidErikoisala(rolePath: String) {
        initTest(getRole(rolePath))

        val yliopisto = Yliopisto(nimi = DEFAULT_YLIOPISTO)
        em.persist(yliopisto)

        val kayttajahallintaErikoistuvaLaakariDTO = createKayttajahallintaErikoistuvaLaakariDTO()
        kayttajahallintaErikoistuvaLaakariDTO.yliopistoId = yliopisto.id
        kayttajahallintaErikoistuvaLaakariDTO.erikoisalaId = count.incrementAndGet()

        restMockMvc.perform(
            post("/api/$rolePath/erikoistuvat-laakarit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaErikoistuvaLaakariDTO))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andExpect(status().isBadRequest)
    }

    @ParameterizedTest
    @ValueSource(strings = [tekninenPaakayttaja, virkailija])
    @Transactional
    fun resendErikoistuvaLaakariInvitation(rolePath: String) {
        initTest(getRole(rolePath))

        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, yliopisto = yliopisto)
        em.persist(erikoistuvaLaakari)
        em.flush()

        val id = erikoistuvaLaakari.id

        val verificationToken = VerificationToken(
            user = User(id = erikoistuvaLaakari.kayttaja?.user?.id)
        )
        em.persist(verificationToken)

        restMockMvc.perform(
            put("/api/$rolePath/erikoistuvat-laakarit/$id/kutsu")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andExpect(status().isNoContent)
    }

    fun getRole(rolePath: String): String {
        return if (rolePath == tekninenPaakayttaja) {
            TEKNINEN_PAAKAYTTAJA
        } else OPINTOHALLINNON_VIRKAILIJA
    }

    fun initTest(role: String) {
        val user = KayttajaResourceWithMockUserIT.createEntity()
        em.persist(user)
        em.flush()
        val userDetails = mapOf<String, List<Any>>()
        val authorities = listOf(SimpleGrantedAuthority(role))
        val authentication = Saml2Authentication(
            DefaultSaml2AuthenticatedPrincipal(user.id, userDetails),
            "test",
            authorities
        )

        val kayttaja = KayttajaHelper.createEntity(em, user)
        if (role == OPINTOHALLINNON_VIRKAILIJA) {
            kayttaja.yliopistot.add(yliopisto)
        }
        em.persist(kayttaja)

        TestSecurityContextHolder.getContext().authentication = authentication
    }

    companion object {
        private val random: Random = SecureRandom()
        private val count: AtomicLong = AtomicLong(random.nextInt().toLong() + (2L * Integer.MAX_VALUE))

        @JvmStatic
        fun createKayttajahallintaErikoistuvaLaakariDTO(): KayttajahallintaErikoistuvaLaakariDTO {
            return KayttajahallintaErikoistuvaLaakariDTO(
                etunimi = "John",
                sukunimi = "Doe",
                sahkopostiosoite = "john.doe@example.com",
                opiskelijatunnus = "123456",
                opintooikeusAlkaa = LocalDate.ofEpochDay(0L),
                opintooikeusPaattyy = LocalDate.ofEpochDay(30L),
                osaamisenArvioinninOppaanPvm = LocalDate.ofEpochDay(0L)
            )
        }
    }
}
