package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.Keskeytysaika
import fi.elsapalvelu.elsa.domain.Tyoskentelyjakso
import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.domain.enumeration.KaytannonKoulutusTyyppi
import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.KeskeytysaikaRepository
import fi.elsapalvelu.elsa.repository.TyoskentelyjaksoRepository
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.service.mapper.KeskeytysaikaMapper
import fi.elsapalvelu.elsa.web.rest.ResourceIntegrationTestBase
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.convertObjectToJsonBytes
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariTyoskentelyjaksoHelper
import fi.elsapalvelu.elsa.web.rest.helpers.KeskeytysaikaHelper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import kotlin.test.assertNotNull

private const val API_TYOSKENTELYJAKSOT = "/api/erikoistuva-laakari/tyoskentelyjaksot"

@SpringBootTest(classes = [ElsaBackendApp::class])
@Transactional
class ErikoistuvaLaakariKeskeytysaikaResourceIT: ResourceIntegrationTestBase() {

    @Autowired private lateinit var tyoskentelyjaksoRepository: TyoskentelyjaksoRepository
    @Autowired private lateinit var keskeytysaikaRepository: KeskeytysaikaRepository
    @Autowired private lateinit var keskeytysaikaMapper: KeskeytysaikaMapper
    @Autowired private lateinit var erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository

    private lateinit var tyoskentelyjakso: Tyoskentelyjakso
    private lateinit var keskeytysaika: Keskeytysaika
    private lateinit var user: User

    @Test
    fun createKeskeytysaika() {
        initTest()

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        keskeytysaika = KeskeytysaikaHelper.createEntity(em, tyoskentelyjakso)

        val tyoskentelyjaksoTableSizeBeforeCreate = keskeytysaikaRepository.findAll().size

        val keskeytysaikaDTO = keskeytysaikaMapper.toDto(keskeytysaika)
        keskeytysaikaDTO.tyoskentelyjaksoId = tyoskentelyjakso.id
        testMockMvc.perform(post("$API_TYOSKENTELYJAKSOT/poissaolot").contentType(APPLICATION_JSON)
            .content(convertObjectToJsonBytes(keskeytysaikaDTO)).with(csrf())).andExpect(status().isCreated)

        val keskeytysaikaList = keskeytysaikaRepository.findAll()
        assertThat(keskeytysaikaList).hasSize(tyoskentelyjaksoTableSizeBeforeCreate + 1)
        val testKeskeytysaika = keskeytysaikaList[keskeytysaikaList.size - 1]
        assertThat(testKeskeytysaika.alkamispaiva).isEqualTo(KeskeytysaikaHelper.DEFAULT_ALKAMISPAIVA)
        assertThat(testKeskeytysaika.paattymispaiva).isEqualTo(KeskeytysaikaHelper.DEFAULT_PAATTYMISPAIVA)
        assertThat(testKeskeytysaika.poissaoloprosentti).isEqualTo(KeskeytysaikaHelper.DEFAULT_POISSAOLOPROSENTTI)
    }

    @Test
    fun createKeskeytysaikaWithExistingId() {
        initTest()

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        keskeytysaika = KeskeytysaikaHelper.createEntity(em, tyoskentelyjakso)

        val tyoskentelyjaksoTableSizeBeforeCreate = keskeytysaikaRepository.findAll().size

        keskeytysaika.id = 1L
        val keskeytysaikaDTO = keskeytysaikaMapper.toDto(keskeytysaika)
        keskeytysaikaDTO.tyoskentelyjaksoId = tyoskentelyjakso.id
        testMockMvc.perform(post("$API_TYOSKENTELYJAKSOT/poissaolot").contentType(APPLICATION_JSON)
            .content(convertObjectToJsonBytes(keskeytysaikaDTO)).with(csrf())).andExpect(status().isBadRequest)

        assertThat(keskeytysaikaRepository.findAll()).hasSize(tyoskentelyjaksoTableSizeBeforeCreate)
    }

    @Test
    fun createKeskeytysaikaForAnotherUser() {
        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em)
        erikoistuvaLaakariRepository.saveAndFlush(erikoistuvaLaakari)

        initTest(erikoistuvaLaakari.kayttaja?.user?.id)

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        keskeytysaika = KeskeytysaikaHelper.createEntity(em, tyoskentelyjakso)

        val tyoskentelyjaksoTableSizeBeforeCreate = keskeytysaikaRepository.findAll().size

        val keskeytysaikaDTO = keskeytysaikaMapper.toDto(keskeytysaika)
        keskeytysaikaDTO.tyoskentelyjaksoId = tyoskentelyjakso.id
        testMockMvc.perform(post("$API_TYOSKENTELYJAKSOT/poissaolot").contentType(APPLICATION_JSON)
            .content(convertObjectToJsonBytes(keskeytysaikaDTO)).with(csrf())).andExpect(status().isBadRequest)

        assertThat(keskeytysaikaRepository.findAll()).hasSize(tyoskentelyjaksoTableSizeBeforeCreate)
    }

    @Test
    fun createKeskeytysaikaWithInvalidDates() {
        initTest()

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        keskeytysaika = KeskeytysaikaHelper.createEntity(em, tyoskentelyjakso)
        keskeytysaika.alkamispaiva = LocalDate.of(2020, 1, 1)
        keskeytysaika.paattymispaiva = LocalDate.of(2020, 12, 1)

        val tyoskentelyjaksoTableSizeBeforeCreate = keskeytysaikaRepository.findAll().size

        var keskeytysaikaDTO = keskeytysaikaMapper.toDto(keskeytysaika)
        keskeytysaikaDTO.tyoskentelyjaksoId = tyoskentelyjakso.id
        testMockMvc.perform(post("$API_TYOSKENTELYJAKSOT/poissaolot").contentType(APPLICATION_JSON)
            .content(convertObjectToJsonBytes(keskeytysaikaDTO)).with(csrf())).andExpect(status().isBadRequest)

        assertThat(keskeytysaikaRepository.findAll()).hasSize(tyoskentelyjaksoTableSizeBeforeCreate)

        keskeytysaika.alkamispaiva = LocalDate.of(2019, 12, 1)
        keskeytysaika.paattymispaiva = LocalDate.of(2020, 1, 10)

        keskeytysaikaDTO = keskeytysaikaMapper.toDto(keskeytysaika)
        keskeytysaikaDTO.tyoskentelyjaksoId = tyoskentelyjakso.id
        testMockMvc.perform(post("$API_TYOSKENTELYJAKSOT/poissaolot").contentType(APPLICATION_JSON)
            .content(convertObjectToJsonBytes(keskeytysaikaDTO)).with(csrf())).andExpect(status().isBadRequest)

        assertThat(keskeytysaikaRepository.findAll()).hasSize(tyoskentelyjaksoTableSizeBeforeCreate)

        keskeytysaika.alkamispaiva = LocalDate.of(2020, 1, 15)
        keskeytysaika.paattymispaiva = LocalDate.of(2020, 1, 10)

        keskeytysaikaDTO = keskeytysaikaMapper.toDto(keskeytysaika)
        keskeytysaikaDTO.tyoskentelyjaksoId = tyoskentelyjakso.id
        testMockMvc.perform(post("$API_TYOSKENTELYJAKSOT/poissaolot").contentType(APPLICATION_JSON)
            .content(convertObjectToJsonBytes(keskeytysaikaDTO)).with(csrf())).andExpect(status().isBadRequest)

        assertThat(keskeytysaikaRepository.findAll()).hasSize(tyoskentelyjaksoTableSizeBeforeCreate)
    }

    @Test
    fun updateKeskeytysaika() {
        initTest()

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        keskeytysaika = KeskeytysaikaHelper.createEntity(em, tyoskentelyjakso)
        keskeytysaikaRepository.saveAndFlush(keskeytysaika)

        val tyoskentelyjaksoTableSizeBeforeUpdate = keskeytysaikaRepository.findAll().size

        assertNotNull(keskeytysaika.id)

        val updatedKeskeytysaika = keskeytysaikaRepository.findById(keskeytysaika.id).get()
        em.detach(updatedKeskeytysaika)
        updatedKeskeytysaika.alkamispaiva = KeskeytysaikaHelper.UPDATED_ALKAMISPAIVA
        updatedKeskeytysaika.paattymispaiva = KeskeytysaikaHelper.UPDATED_PAATTYMISPAIVA
        updatedKeskeytysaika.poissaoloprosentti = KeskeytysaikaHelper.UPDATED_POISSAOLOPROSENTTI
        val keskeytysaikaDTO = keskeytysaikaMapper.toDto(updatedKeskeytysaika)

        testMockMvc.perform(put("$API_TYOSKENTELYJAKSOT/poissaolot").contentType(APPLICATION_JSON)
            .content(convertObjectToJsonBytes(keskeytysaikaDTO)).with(csrf())).andExpect(status().isOk)

        val keskeytysaikaList = keskeytysaikaRepository.findAll()
        assertThat(keskeytysaikaList).hasSize(tyoskentelyjaksoTableSizeBeforeUpdate)
        val testKeskeytysaika = keskeytysaikaList[keskeytysaikaList.size - 1]
        assertThat(testKeskeytysaika.alkamispaiva).isEqualTo(KeskeytysaikaHelper.UPDATED_ALKAMISPAIVA)
        assertThat(testKeskeytysaika.paattymispaiva).isEqualTo(KeskeytysaikaHelper.UPDATED_PAATTYMISPAIVA)
        assertThat(testKeskeytysaika.poissaoloprosentti).isEqualTo(KeskeytysaikaHelper.UPDATED_POISSAOLOPROSENTTI)
    }

    @Test
    fun updateAnotherUserKeskeytysaika() {
        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em)
        erikoistuvaLaakariRepository.saveAndFlush(erikoistuvaLaakari)

        initTest(erikoistuvaLaakari.kayttaja?.user?.id)

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        keskeytysaika = KeskeytysaikaHelper.createEntity(em, tyoskentelyjakso)
        keskeytysaikaRepository.saveAndFlush(keskeytysaika)

        val tyoskentelyjaksoTableSizeBeforeUpdate = keskeytysaikaRepository.findAll().size

        assertNotNull(keskeytysaika.id)

        val updatedKeskeytysaika = keskeytysaikaRepository.findById(keskeytysaika.id).get()
        em.detach(updatedKeskeytysaika)
        updatedKeskeytysaika.alkamispaiva = KeskeytysaikaHelper.UPDATED_ALKAMISPAIVA
        updatedKeskeytysaika.paattymispaiva = KeskeytysaikaHelper.UPDATED_PAATTYMISPAIVA
        updatedKeskeytysaika.poissaoloprosentti = KeskeytysaikaHelper.UPDATED_POISSAOLOPROSENTTI
        val keskeytysaikaDTO = keskeytysaikaMapper.toDto(updatedKeskeytysaika)

        testMockMvc.perform(put("$API_TYOSKENTELYJAKSOT/poissaolot").contentType(APPLICATION_JSON)
            .content(convertObjectToJsonBytes(keskeytysaikaDTO)).with(csrf())).andExpect(status().isBadRequest)

        assertThat(keskeytysaikaRepository.findAll()).hasSize(tyoskentelyjaksoTableSizeBeforeUpdate)
    }

    @Test
    fun updateKeskeytysaikaWithoutId() {
        initTest()

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        keskeytysaika = KeskeytysaikaHelper.createEntity(em, tyoskentelyjakso)
        keskeytysaikaRepository.saveAndFlush(keskeytysaika)

        val tyoskentelyjaksoTableSizeBeforeUpdate = keskeytysaikaRepository.findAll().size

        assertNotNull(keskeytysaika.id)

        val updatedKeskeytysaika = keskeytysaikaRepository.findById(keskeytysaika.id).get()
        em.detach(updatedKeskeytysaika)
        updatedKeskeytysaika.id = null
        updatedKeskeytysaika.alkamispaiva = KeskeytysaikaHelper.UPDATED_ALKAMISPAIVA
        updatedKeskeytysaika.paattymispaiva = KeskeytysaikaHelper.UPDATED_PAATTYMISPAIVA
        updatedKeskeytysaika.poissaoloprosentti = KeskeytysaikaHelper.UPDATED_POISSAOLOPROSENTTI
        val keskeytysaikaDTO = keskeytysaikaMapper.toDto(updatedKeskeytysaika)

        testMockMvc.perform(put("$API_TYOSKENTELYJAKSOT/poissaolot").contentType(APPLICATION_JSON)
            .content(convertObjectToJsonBytes(keskeytysaikaDTO)).with(csrf())).andExpect(status().isBadRequest)

        val keskeytysaikaList = keskeytysaikaRepository.findAll()
        assertThat(keskeytysaikaList).hasSize(tyoskentelyjaksoTableSizeBeforeUpdate)
    }

    @Test
    fun deleteKeskeytysaika() {
        initTest()

        keskeytysaika = KeskeytysaikaHelper.createEntity(em, tyoskentelyjakso)
        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)
        keskeytysaikaRepository.saveAndFlush(keskeytysaika)

        val keskeytysaikaTableSizeBeforeDelete = keskeytysaikaRepository.findAll().size

        testMockMvc.perform(delete("$API_TYOSKENTELYJAKSOT/poissaolot/{id}", keskeytysaika.id).accept(APPLICATION_JSON).with(csrf())).andExpect(status().isNoContent)

        val keskeytysaikaList = keskeytysaikaRepository.findAll()
        assertThat(keskeytysaikaList).hasSize(keskeytysaikaTableSizeBeforeDelete - 1)
    }

    @Test
    fun deleteAnotherUserKeskeytysaika() {
        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em)
        erikoistuvaLaakariRepository.saveAndFlush(erikoistuvaLaakari)

        initTest(erikoistuvaLaakari.kayttaja?.user?.id)

        keskeytysaika = KeskeytysaikaHelper.createEntity(em, tyoskentelyjakso)
        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)
        keskeytysaikaRepository.saveAndFlush(keskeytysaika)

        val keskeytysaikaTableSizeBeforeDelete = keskeytysaikaRepository.findAll().size

        testMockMvc.perform(delete("$API_TYOSKENTELYJAKSOT/poissaolot/{id}", keskeytysaika.id).accept(APPLICATION_JSON).with(csrf())).andExpect(status().isBadRequest)

        val keskeytysaikaList = keskeytysaikaRepository.findAll()
        assertThat(keskeytysaikaList).hasSize(keskeytysaikaTableSizeBeforeDelete)
    }

    fun initTest(userId: String? = null, kaytannonKoulutus: KaytannonKoulutusTyyppi? = ErikoistuvaLaakariTyoskentelyjaksoHelper.DEFAULT_KAYTANNON_KOULUTUS) {
        user = KayttajaResourceWithMockUserIT.createEntity()
        persistAndFlush(user)
        TestSecurityContextHolder.getContext().authentication = Saml2Authentication(DefaultSaml2AuthenticatedPrincipal(userId ?: user.id, mapOf<String, List<Any>>()),
            "test", listOf(SimpleGrantedAuthority(ERIKOISTUVA_LAAKARI)))
        tyoskentelyjakso = ErikoistuvaLaakariTyoskentelyjaksoHelper.createEntity(em, user, kaytannonKoulutus = kaytannonKoulutus)
    }

}
