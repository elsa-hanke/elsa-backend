package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.enumeration.OpintooikeudenTila
import fi.elsapalvelu.elsa.repository.UserRepository
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.service.dto.KaytonAloitusDTO
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.convertObjectToJsonBytes
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper
import fi.elsapalvelu.elsa.web.rest.helpers.OpintooikeusHelper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import jakarta.persistence.EntityManager
import org.springframework.test.context.bean.override.mockito.MockitoBean

@AutoConfigureMockMvc
@SpringBootTest(classes = [ElsaBackendApp::class])
class ErikoistuvaLaakariKaytonAloitusResourceIT {

    @Autowired
    private lateinit var restKaytonAloitusMockMvc: MockMvc

    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var userRepository: UserRepository

    private lateinit var erikoistuvaLaakari: ErikoistuvaLaakari

    // 5 vuotta = 31.12.1974
    private val now = 157680000L

    @MockitoBean
    private lateinit var clock: Clock

    @BeforeEach
    fun setup() {
        `when`(clock.instant()).thenReturn(Instant.ofEpochSecond(now))
        `when`(clock.zone).thenReturn(ZoneId.systemDefault())
    }

    @Test
    @Transactional
    fun shouldAddEmailAddressHasOneOpintooikeus() {
        initTest()

        val kaytonAloitusDTO = KaytonAloitusDTO(sahkoposti = DEFAULT_EMAIL)

        restKaytonAloitusMockMvc.perform(
            put("/api/erikoistuva-laakari/kaytonaloitus")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kaytonAloitusDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val user = userRepository.findByIdOrNull(erikoistuvaLaakari.kayttaja?.user?.id)
        assertThat(user?.email).isEqualTo(DEFAULT_EMAIL)
    }

    @ParameterizedTest
    @MethodSource("validStates")
    @Transactional
    fun shouldAddEmailAddressAndOpintooikeusKaytossa(tila: OpintooikeudenTila) {
        initTest()

        val opintooikeus = OpintooikeusHelper.addOpintooikeusForErikoistuvaLaakari(
            em,
            erikoistuvaLaakari,
            LocalDate.ofEpochDay(0L),
            LocalDate.ofEpochDay(158680000L),
            tila
        )
        OpintooikeusHelper.addOpintooikeusForErikoistuvaLaakari(
            em,
            erikoistuvaLaakari,
            LocalDate.ofEpochDay(700L),
            LocalDate.ofEpochDay(159680000L),
            tila
        )

        val kaytonAloitusDTO = KaytonAloitusDTO(sahkoposti = DEFAULT_EMAIL, opintooikeusId = opintooikeus.id)

        restKaytonAloitusMockMvc.perform(
            put("/api/erikoistuva-laakari/kaytonaloitus")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kaytonAloitusDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val user = userRepository.findByIdOrNull(erikoistuvaLaakari.kayttaja?.user?.id)
        assertThat(user?.email).isEqualTo(DEFAULT_EMAIL)
        assertThat(erikoistuvaLaakari.getOpintooikeusKaytossa()).isEqualTo(opintooikeus)
    }

    @Test
    @Transactional
    fun shouldThrowExceptionIfEmailIsNull() {
        initTest()

        val kaytonAloitusDTO = KaytonAloitusDTO()

        restKaytonAloitusMockMvc.perform(
            put("/api/erikoistuva-laakari/kaytonaloitus")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kaytonAloitusDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)
    }

    @Test
    @Transactional
    fun shouldThrowExceptionIfEmailAlreadyExists() {
        initTest(DEFAULT_EMAIL)

        val kaytonAloitusDTO = KaytonAloitusDTO(sahkoposti = DEFAULT_EMAIL)

        restKaytonAloitusMockMvc.perform(
            put("/api/erikoistuva-laakari/kaytonaloitus")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kaytonAloitusDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)
    }

    @Test
    @Transactional
    fun shouldThrowExceptionIfOpintooikeusIdIsNullWhenMultipleOpintooikeusVoimassaExist() {
        initTest()

        OpintooikeusHelper.addOpintooikeusForErikoistuvaLaakari(
            em,
            erikoistuvaLaakari,
            LocalDate.ofEpochDay(0L),
            LocalDate.ofEpochDay(158680000L)
        )
        OpintooikeusHelper.addOpintooikeusForErikoistuvaLaakari(
            em,
            erikoistuvaLaakari,
            LocalDate.ofEpochDay(700L),
            LocalDate.ofEpochDay(159680000L)
        )

        OpintooikeusHelper.addOpintooikeusForErikoistuvaLaakari(em, erikoistuvaLaakari)

        val kaytonAloitusDTO = KaytonAloitusDTO(sahkoposti = DEFAULT_EMAIL)

        restKaytonAloitusMockMvc.perform(
            put("/api/erikoistuva-laakari/kaytonaloitus")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kaytonAloitusDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)
    }

    @ParameterizedTest
    @MethodSource("invalidStates")
    @Transactional
    fun shouldThrowExceptionIfOpintooikeusStateWithGivenIdNotValid(tila: OpintooikeudenTila) {
        initTest()

        val opintooikeus = OpintooikeusHelper.addOpintooikeusForErikoistuvaLaakari(
            em,
            erikoistuvaLaakari,
            LocalDate.ofEpochDay(0L),
            LocalDate.ofEpochDay(3650L),
            tila
        )
        OpintooikeusHelper.addOpintooikeusForErikoistuvaLaakari(
            em,
            erikoistuvaLaakari,
            LocalDate.ofEpochDay(700L),
            LocalDate.ofEpochDay(4350L),
            tila
        )

        val kaytonAloitusDTO = KaytonAloitusDTO(sahkoposti = DEFAULT_EMAIL, opintooikeusId = opintooikeus.id)

        restKaytonAloitusMockMvc.perform(
            put("/api/erikoistuva-laakari/kaytonaloitus")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kaytonAloitusDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)
    }

    @Test
    @Transactional
    fun shouldThrowExceptionIfOpintooikeusVoimassaNotFoundWithGivenId() {
        initTest()

        val opintooikeus = OpintooikeusHelper.addOpintooikeusForErikoistuvaLaakari(
            em,
            erikoistuvaLaakari,
            LocalDate.ofEpochDay(0L),
            LocalDate.ofEpochDay(730L),
            OpintooikeudenTila.VALMISTUNUT,
            LocalDate.ofEpochDay(910L)
        )

        val kaytonAloitusDTO = KaytonAloitusDTO(sahkoposti = DEFAULT_EMAIL, opintooikeusId = opintooikeus.id)

        restKaytonAloitusMockMvc.perform(
            put("/api/erikoistuva-laakari/kaytonaloitus")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kaytonAloitusDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)
    }

    @Test
    @Transactional
    fun shouldThrowExceptionIfOpintooikeusNotFoundWithGivenId() {
        initTest()

        OpintooikeusHelper.addOpintooikeusForErikoistuvaLaakari(
            em,
            erikoistuvaLaakari,
            LocalDate.ofEpochDay(0L),
            LocalDate.ofEpochDay(3650)
        )

        val kaytonAloitusDTO = KaytonAloitusDTO(sahkoposti = DEFAULT_EMAIL, opintooikeusId = 999999999)

        restKaytonAloitusMockMvc.perform(
            put("/api/erikoistuva-laakari/kaytonaloitus")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kaytonAloitusDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)
    }

    fun initTest(sahkoposti: String? = null) {
        val user = KayttajaResourceWithMockUserIT.createEntity(email = sahkoposti)
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

        erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, user)
    }

    companion object {

        private const val DEFAULT_EMAIL = "test@test.test"

        @JvmStatic
        fun invalidStates(): List<OpintooikeudenTila> =
            listOf(OpintooikeudenTila.VANHENTUNUT)

        @JvmStatic
        fun validStates(): List<OpintooikeudenTila> =
            listOf(
                OpintooikeudenTila.AKTIIVINEN,
                OpintooikeudenTila.AKTIIVINEN_EI_LASNA,
                OpintooikeudenTila.PASSIIVINEN,
                OpintooikeudenTila.PERUUTETTU,
                OpintooikeudenTila.VALMISTUNUT
            )

        @JvmStatic
        fun endedStates(): List<OpintooikeudenTila> = listOf(
            OpintooikeudenTila.PERUUTETTU,
            OpintooikeudenTila.VALMISTUNUT
        )

    }
}
