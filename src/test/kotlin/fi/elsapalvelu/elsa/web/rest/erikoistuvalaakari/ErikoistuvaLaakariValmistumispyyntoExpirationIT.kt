package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.koulutus.OpintosuoritusTyyppiEnum
import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.domain.perustiedot.ErikoisalaTyyppi
import fi.elsapalvelu.elsa.domain.tyoskentely.TyoskentelyjaksoTyyppi
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.web.rest.ResourceIntegrationTestBase
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoisalaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper
import fi.elsapalvelu.elsa.web.rest.helpers.OpintosuoritusHelper
import fi.elsapalvelu.elsa.web.rest.helpers.TyoskentelyjaksoHelper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private const val ENDPOINT =
    "/api/erikoistuva-laakari/valmistumispyynto-suoritusten-tila"

@SpringBootTest(classes = [ElsaBackendApp::class])
@Transactional
class ErikoistuvaLaakariValmistumispyyntoExpirationIT : ResourceIntegrationTestBase() {

    private lateinit var user: User

    @MockitoBean
    private lateinit var clock: Clock

    @BeforeEach
    fun setup() {
        `when`(clock.instant()).thenReturn(Instant.ofEpochSecond(347155200L))
        `when`(clock.zone).thenReturn(ZoneId.systemDefault())
        initValidStudyRecord()
    }

    @Test
    fun getValmistumispyyntoSuoritustenTilaVanhentunutValtakunnallinenKuulusteluExists() {
        persistNationalExam(LocalDate.now(clock).minusYears(4).minusDays(1))

        testMockMvc.perform(get(ENDPOINT))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.vanhojaTyoskentelyjaksojaOrSuorituksiaExists").value(false))
            .andExpect(jsonPath("$.kuulusteluVanhentunut").value(true))
    }

    @Test
    fun getValmistumispyyntoSuoritustenTilaValtakunnallinenKuulusteluAtExpirationBoundaryIsValid() {
        persistNationalExam(LocalDate.now(clock).minusYears(4))

        testMockMvc.perform(get(ENDPOINT))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.vanhojaTyoskentelyjaksojaOrSuorituksiaExists").value(false))
            .andExpect(jsonPath("$.kuulusteluVanhentunut").value(false))
    }

    private fun persistNationalExam(completionDate: LocalDate) {
        em.persist(
            OpintosuoritusHelper.createEntity(
                em,
                user,
                tyyppiEnum = OpintosuoritusTyyppiEnum.VALTAKUNNALLINEN_KUULUSTELU,
                suorituspaiva = completionDate
            )
        )
    }

    private fun initValidStudyRecord() {
        user = KayttajaResourceWithMockUserIT.createEntity()
        em.persist(user)
        em.flush()
        setSecurityContext(requireNotNull(user.id), ERIKOISTUVA_LAAKARI)

        val erikoisala = ErikoisalaHelper.createEntity(tyyppi = ErikoisalaTyyppi.LAAKETIEDE)
        em.persist(erikoisala)
        em.persist(ErikoistuvaLaakariHelper.createEntity(em, user, erikoisala = erikoisala))

        em.persist(
            TyoskentelyjaksoHelper.createEntity(
                em,
                user,
                alkamispaiva = LocalDate.ofEpochDay(2000L),
                paattymispaiva = LocalDate.ofEpochDay(2100L),
                tyoskentelyjaksoTyyppi = TyoskentelyjaksoTyyppi.KESKUSSAIRAALA
            )
        )
        em.persist(
            TyoskentelyjaksoHelper.createEntity(
                em,
                user,
                alkamispaiva = LocalDate.ofEpochDay(2500L),
                paattymispaiva = LocalDate.ofEpochDay(2600L),
                tyoskentelyjaksoTyyppi = TyoskentelyjaksoTyyppi.KESKUSSAIRAALA
            )
        )
        em.persist(
            OpintosuoritusHelper.createEntity(
                em,
                user,
                suorituspaiva = LocalDate.ofEpochDay(3500L)
            )
        )
    }
}
