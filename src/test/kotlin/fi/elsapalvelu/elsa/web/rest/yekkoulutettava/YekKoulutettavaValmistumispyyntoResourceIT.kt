package fi.elsapalvelu.elsa.web.rest.yekkoulutettava

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.config.YEK_ERIKOISALA_ID
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto
import fi.elsapalvelu.elsa.repository.valmistuminen.ValmistumispyyntoRepository
import fi.elsapalvelu.elsa.security.YEK_KOULUTETTAVA
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper
import fi.elsapalvelu.elsa.web.rest.helpers.OpintooikeusHelper
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@AutoConfigureMockMvc
@SpringBootTest(classes = [ElsaBackendApp::class])
class YekKoulutettavaValmistumispyyntoResourceIT {

    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var valmistumispyyntoRepository: ValmistumispyyntoRepository

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    @Transactional
    fun createYekGraduationRequestPersistsSentRequest() {
        val opintooikeus = initYekKoulutettava()
        val sizeBefore = valmistumispyyntoRepository.count()

        mockMvc.perform(
            multipart(VALMISTUMISPYYNTO_ENDPOINT)
                .param("erikoistujanSahkoposti", "yek@example.com")
                .param("erikoistujanPuhelinnumero", "+358401234567")
                .with(csrf())
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.opintooikeusId").value(opintooikeus.id))

        assertThat(valmistumispyyntoRepository.count()).isEqualTo(sizeBefore + 1)
        val saved = valmistumispyyntoRepository.findByOpintooikeusId(opintooikeus.id!!)
        assertThat(saved).isNotNull
        assertThat(saved?.erikoistujanKuittausaika).isEqualTo(LocalDate.now())
        assertThat(saved?.opintooikeus?.erikoisala?.id).isEqualTo(YEK_ERIKOISALA_ID)
    }

    @Test
    @Transactional
    fun updateAlreadySentRequestShouldPreserveExistingYekBehavior() {
        val opintooikeus = initYekKoulutettava()
        val valmistumispyynto = Valmistumispyynto(
            opintooikeus = opintooikeus,
            erikoistujanKuittausaika = LocalDate.now(),
            selvitysVanhentuneistaSuorituksista = ORIGINAL_EXPLANATION
        )
        em.persist(valmistumispyynto)
        em.flush()

        mockMvc.perform(
            multipart(VALMISTUMISPYYNTO_ENDPOINT)
                .with { request -> request.method = "PUT"; request }
                .param("selvitysVanhentuneistaSuorituksista", UPDATED_EXPLANATION)
                .with(csrf())
        ).andExpect(status().isOk)

        em.flush()
        em.clear()
        val updated = valmistumispyyntoRepository.findById(valmistumispyynto.id!!).orElseThrow()
        assertThat(updated.selvitysVanhentuneistaSuorituksista).isEqualTo(UPDATED_EXPLANATION)
    }

    @Test
    @Transactional
    fun validationShouldPreserveExistingYekErrorEntityName() {
        val opintooikeus = initYekKoulutettava()
        opintooikeus.erikoistuvaLaakari!!.apply {
            laillistamispaiva = null
            laillistamistodistus = null
            laillistamispaivanLiitetiedostonNimi = null
            laillistamispaivanLiitetiedostonTyyppi = null
        }
        em.flush()

        mockMvc.perform(
            multipart(VALMISTUMISPYYNTO_ENDPOINT).with(csrf())
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.params").value(YEK_VALMISTUMISPYYNTO_ENTITY_NAME))
    }

    private fun initYekKoulutettava(): Opintooikeus {
        val user = KayttajaResourceWithMockUserIT.createEntity()
        em.persist(user)
        em.flush()
        authenticate(user)

        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, user)
        em.persist(erikoistuvaLaakari)
        em.flush()

        return OpintooikeusHelper.addOpintooikeusForYekKoulutettava(em, erikoistuvaLaakari)
            .also {
                OpintooikeusHelper.setOpintooikeusKaytossa(erikoistuvaLaakari, it)
                em.flush()
            }
    }

    private fun authenticate(user: User) {
        val authentication = Saml2Authentication(
            DefaultSaml2AuthenticatedPrincipal(user.id, emptyMap<String, List<Any>>()),
            "test",
            listOf(SimpleGrantedAuthority(YEK_KOULUTETTAVA))
        )
        TestSecurityContextHolder.getContext().authentication = authentication
    }

    companion object {
        private const val VALMISTUMISPYYNTO_ENDPOINT = "/api/yek-koulutettava/valmistumispyynto"
        private const val ORIGINAL_EXPLANATION = "original explanation"
        private const val UPDATED_EXPLANATION = "updated explanation"
        private const val YEK_VALMISTUMISPYYNTO_ENTITY_NAME = "valmistumispyyntö"
    }
}
