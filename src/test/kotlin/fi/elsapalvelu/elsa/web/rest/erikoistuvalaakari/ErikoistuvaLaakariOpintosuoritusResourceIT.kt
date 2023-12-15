package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.Opintosuoritus
import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.domain.Yliopisto
import fi.elsapalvelu.elsa.domain.enumeration.OpintosuoritusTyyppiEnum
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.helpers.*
import fi.elsapalvelu.elsa.web.rest.helpers.KayttajaHelper.Companion.DEFAULT_ID
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
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
import jakarta.persistence.EntityManager

@AutoConfigureMockMvc
@SpringBootTest(classes = [ElsaBackendApp::class])
class ErikoistuvaLaakariOpintosuoritusResourceIT {

    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var restOpintosuorituksetMockMvc: MockMvc

    private lateinit var opintosuoritus: Opintosuoritus

    private lateinit var user: User

    private lateinit var defaultYliopisto: Yliopisto

    @Test
    @Transactional
    fun getOpintosuoritukset() {
        initTest()

        val opintosuoritusTyyppi = OpintosuoritusTyyppiEnum.JOHTAMISOPINTO
        val opintosuoritus2Tyyppi = OpintosuoritusTyyppiEnum.SATEILYSUOJAKOULUTUS

        opintosuoritus = OpintosuoritusHelper.createEntity(
            em,
            user,
            OPINTOSUORITUS1_KURSSIKOODI,
            opintosuoritusTyyppi
        )
        val opintosuoritusOsakokonaisuus = OpintosuoritusOsakokonaisuusHelper.createEntity(
            em,
            opintosuoritus,
            OPINTOSUORITUS_OSAKOKONAISUUS1_KURSSIKOODI
        )

        val opintosuoritus2 = OpintosuoritusHelper.createEntity(
            em,
            user,
            OPINTOSUORITUS2_KURSSIKOODI,
            opintosuoritus2Tyyppi
        )
        em.persist(opintosuoritus2)

        opintosuoritus.osakokonaisuudet?.add(opintosuoritusOsakokonaisuus)
        em.persist(opintosuoritus)

        restOpintosuorituksetMockMvc.perform(get("/api/erikoistuva-laakari/opintosuoritukset"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(
                jsonPath("$.johtamisopinnotSuoritettu").value
                    (OpintosuoritusHelper.DEFAULT_OPINTOPISTEET)
            )
            .andExpect(
                jsonPath("$.johtamisopinnotVaadittu").value(
                    OpintoopasHelper.DEFAULT_ERIKOISALAN_VAATIMA_JOHTAMISOPINTOJEN_VAHIMMAISMAARA
                )
            )
            .andExpect(
                jsonPath("$.sateilysuojakoulutuksetSuoritettu").value(
                    OpintosuoritusHelper.DEFAULT_OPINTOPISTEET
                )
            )
            .andExpect(
                jsonPath("$.sateilysuojakoulutuksetVaadittu").value(
                    OpintoopasHelper.DEFAULT_ERIKOISALAN_VAATIMA_SATEILYSUOJAKOULUTUSTEN_VAHIMMAISMAARA
                )
            )
            .andExpect(jsonPath("$.opintosuoritukset").value(Matchers.hasSize<Any>(2)))
            .andExpect(jsonPath("$.opintosuoritukset[0].id").value(opintosuoritus.id as Any))
            .andExpect(jsonPath("$.opintosuoritukset[0].nimi_fi").value(OpintosuoritusHelper.DEFAULT_NIMI_FI))
            .andExpect(jsonPath("$.opintosuoritukset[0].nimi_sv").value(OpintosuoritusHelper.DEFAULT_NIMI_SV))
            .andExpect(jsonPath("$.opintosuoritukset[0].tyyppi.nimi").value(opintosuoritusTyyppi.toString()))
            .andExpect(
                jsonPath("$.opintosuoritukset[0].suorituspaiva").value(
                    OpintosuoritusHelper.DEFAULT_SUORITUSPAIVA.toString()
                )
            )
            .andExpect(
                jsonPath("$.opintosuoritukset[0].opintopisteet").value(
                    OpintosuoritusHelper.DEFAULT_OPINTOPISTEET
                )
            )
            .andExpect(jsonPath("$.opintosuoritukset[0].hyvaksytty").value(true))
            .andExpect(
                jsonPath("$.opintosuoritukset[0].arvio_fi").value(
                    OpintosuoritusHelper.DEFAULT_ARVIO_FI
                )
            )
            .andExpect(
                jsonPath("$.opintosuoritukset[0].arvio_sv").value(
                    OpintosuoritusHelper.DEFAULT_ARVIO_SV
                )
            )
            .andExpect(
                jsonPath("$.opintosuoritukset[0].vanhenemispaiva").value(
                    OpintosuoritusHelper.DEFAULT_VANHENEMISPAIVA.toString()
                )
            )
            .andExpect(jsonPath("$.opintosuoritukset[0].osakokonaisuudet").value(Matchers.hasSize<Any>(1)))
            .andExpect(
                jsonPath("$.opintosuoritukset[0].osakokonaisuudet[0].id").value(
                    opintosuoritusOsakokonaisuus.id as Any
                )
            )
            .andExpect(
                jsonPath("$.opintosuoritukset[0].osakokonaisuudet[0].nimi_fi").value(
                    OpintosuoritusOsakokonaisuusHelper.DEFAULT_NIMI_FI
                )
            )
            .andExpect(
                jsonPath("$.opintosuoritukset[0].osakokonaisuudet[0].nimi_sv").value(
                    OpintosuoritusOsakokonaisuusHelper.DEFAULT_NIMI_SV
                )
            )
            .andExpect(
                jsonPath("$.opintosuoritukset[0].osakokonaisuudet[0].kurssikoodi").value(
                    OPINTOSUORITUS_OSAKOKONAISUUS1_KURSSIKOODI
                )
            )
            .andExpect(
                jsonPath("$.opintosuoritukset[0].osakokonaisuudet[0].suorituspaiva").value(
                    OpintosuoritusOsakokonaisuusHelper.DEFAULT_SUORITUSPAIVA.toString()
                )
            )
            .andExpect(
                jsonPath("$.opintosuoritukset[0].osakokonaisuudet[0].opintopisteet").value(
                    OpintosuoritusOsakokonaisuusHelper.DEFAULT_OPINTOPISTEET
                )
            )
            .andExpect(jsonPath("$.opintosuoritukset[0].osakokonaisuudet[0].hyvaksytty").value(true))
            .andExpect(
                jsonPath("$.opintosuoritukset[0].osakokonaisuudet[0].arvio_fi").value(
                    OpintosuoritusOsakokonaisuusHelper.DEFAULT_ARVIO_FI
                )
            )
            .andExpect(
                jsonPath("$.opintosuoritukset[0].osakokonaisuudet[0].arvio_sv").value(
                    OpintosuoritusOsakokonaisuusHelper.DEFAULT_ARVIO_SV
                )
            )
            .andExpect(
                jsonPath("$.opintosuoritukset[0].osakokonaisuudet[0].vanhenemispaiva").value(
                    OpintosuoritusOsakokonaisuusHelper.DEFAULT_VANHENEMISPAIVA.toString()
                )
            )
            .andExpect(jsonPath("$.opintosuoritukset[1].tyyppi.nimi").value(opintosuoritus2Tyyppi.toString()))
    }

    /* ELSA-328 changes this logic
    @Test
    @Transactional
    fun getOpintosuorituksetShouldNotReturnOpintosuoritusWithOsakokonaisuusKurssikoodi() {
        initTest()

        defaultYliopisto = Yliopisto(nimi = defaultYliopistoEnum)
        em.persist(defaultYliopisto)

        KurssikoodiHelper.createEntity(
            em,
            tunniste = OPINTOSUORITUS1_KURSSIKOODI,
            tyyppi = OpintosuoritusTyyppiEnum.JOHTAMISOPINTO,
            yliopisto = defaultYliopisto,
            isOsakokonaisuus = true
        )
        opintosuoritus = OpintosuoritusHelper.createEntity(
            em,
            user,
            kurssikoodi = OPINTOSUORITUS1_KURSSIKOODI
        )
        val opintosuoritusOsakokonaisuus = OpintosuoritusOsakokonaisuusHelper.createEntity(
            em,
            opintosuoritus,
            OPINTOSUORITUS_OSAKOKONAISUUS1_KURSSIKOODI,
        )

        opintosuoritus.osakokonaisuudet?.add(opintosuoritusOsakokonaisuus)
        em.persist(opintosuoritus)

        val opintosuoritusWithOsakokonaisuusKurssikoodi = OpintosuoritusHelper.createEntity(
            em,
            kurssikoodi = OPINTOSUORITUS_OSAKOKONAISUUS1_KURSSIKOODI
        )
        em.persist(opintosuoritusWithOsakokonaisuusKurssikoodi)

        restOpintosuorituksetMockMvc.perform(get("/api/erikoistuva-laakari/opintosuoritukset"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.opintosuoritukset").value(Matchers.hasSize<Any>(2)))
            .andExpect(jsonPath("$.opintosuoritukset[0].osakokonaisuudet").value(Matchers.hasSize<Any>(1)))
    }
    */

    @Test
    @Transactional
    fun getOpintosuorituksetShouldNotReturnItemsBelongingToAnotherOpintooikeus() {
        initTest()

        opintosuoritus = OpintosuoritusHelper.createEntity(
            em,
            user,
            kurssikoodi = OPINTOSUORITUS1_KURSSIKOODI
        )
        em.persist(opintosuoritus)

        val erikoistuvaLaakari = opintosuoritus.opintooikeus?.erikoistuvaLaakari
        requireNotNull(erikoistuvaLaakari)

        val anotherOpintooikeus = OpintooikeusHelper.addOpintooikeusForErikoistuvaLaakari(em, erikoistuvaLaakari)
        val opintosuoritusForAnotherOpintooikeus =
            OpintosuoritusHelper.createEntity(em, opintooikeus = anotherOpintooikeus)
        em.persist(opintosuoritusForAnotherOpintooikeus)

        restOpintosuorituksetMockMvc.perform(get("/api/erikoistuva-laakari/opintosuoritukset"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.opintosuoritukset").value(Matchers.hasSize<Any>(1)))
    }

    fun initTest(userId: String? = DEFAULT_ID) {
        user = KayttajaResourceWithMockUserIT.createEntity()
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
    }

    companion object {
        private const val OPINTOSUORITUS1_KURSSIKOODI = "ABCDEFG"
        private const val OPINTOSUORITUS2_KURSSIKOODI = "BCDEFGH"
        private const val OPINTOSUORITUS_OSAKOKONAISUUS1_KURSSIKOODI = "CDEFGHI"
        private val defaultYliopistoEnum = YliopistoEnum.HELSINGIN_YLIOPISTO
    }

}
