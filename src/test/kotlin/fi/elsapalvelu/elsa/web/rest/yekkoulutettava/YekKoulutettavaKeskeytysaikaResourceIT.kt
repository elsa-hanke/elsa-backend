package fi.elsapalvelu.elsa.web.rest.yekkoulutettava

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.tyoskentely.Keskeytysaika
import fi.elsapalvelu.elsa.domain.tyoskentely.Tyoskentelyjakso
import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.repository.tyoskentely.KeskeytysaikaRepository
import fi.elsapalvelu.elsa.repository.tyoskentely.TyoskentelyjaksoRepository
import fi.elsapalvelu.elsa.security.YEK_KOULUTETTAVA
import fi.elsapalvelu.elsa.service.mapper.tyoskentely.KeskeytysaikaMapper
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.convertObjectToJsonBytes
import fi.elsapalvelu.elsa.web.rest.helpers.KeskeytysaikaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.YekKoulutettavaTyoskentelyjaksoHelper
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import kotlin.test.assertNotNull

private const val API_TYOSKENTELYJAKSOT = "/api/yek-koulutettava/tyoskentelyjaksot"

@AutoConfigureMockMvc
@SpringBootTest(classes = [ElsaBackendApp::class])
class YekKoulutettavaKeskeytysaikaResourceIT {

    @Autowired private lateinit var tyoskentelyjaksoRepository: TyoskentelyjaksoRepository
    @Autowired private lateinit var keskeytysaikaRepository: KeskeytysaikaRepository
    @Autowired private lateinit var keskeytysaikaMapper: KeskeytysaikaMapper
    @Autowired private lateinit var em: EntityManager
    @Autowired private lateinit var restKeskeytysaikaMockMvc: MockMvc

    private lateinit var tyoskentelyjakso: Tyoskentelyjakso
    private lateinit var keskeytysaika: Keskeytysaika
    private lateinit var user: User

    @Test
    @Transactional
    fun createKeskeytysaika() {
        initTest()

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)
        keskeytysaika = KeskeytysaikaHelper.createEntity(em, tyoskentelyjakso)
        val tyoskentelyjaksoTableSizeBeforeCreate = keskeytysaikaRepository.findAll().size
        val keskeytysaikaDTO = keskeytysaikaMapper.toDto(keskeytysaika)
        keskeytysaikaDTO.tyoskentelyjaksoId = tyoskentelyjakso.id
        restKeskeytysaikaMockMvc.perform(post("$API_TYOSKENTELYJAKSOT/poissaolot").contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(keskeytysaikaDTO)).with(csrf())).andExpect(status().isCreated)

        val keskeytysaikaList = keskeytysaikaRepository.findAll()
        assertThat(keskeytysaikaList).hasSize(tyoskentelyjaksoTableSizeBeforeCreate + 1)
        val testKeskeytysaika = keskeytysaikaList[keskeytysaikaList.size - 1]
        assertThat(testKeskeytysaika.alkamispaiva).isEqualTo(KeskeytysaikaHelper.DEFAULT_ALKAMISPAIVA)
        assertThat(testKeskeytysaika.paattymispaiva).isEqualTo(KeskeytysaikaHelper.DEFAULT_PAATTYMISPAIVA)
        assertThat(testKeskeytysaika.poissaoloprosentti).isEqualTo(KeskeytysaikaHelper.DEFAULT_POISSAOLOPROSENTTI)
    }

    @Test
    @Transactional
    fun createKeskeytysaikaWithExistingId() {
        initTest()

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)
        keskeytysaika = KeskeytysaikaHelper.createEntity(em, tyoskentelyjakso)
        val tyoskentelyjaksoTableSizeBeforeCreate = keskeytysaikaRepository.findAll().size

        keskeytysaika.id = 1L
        val keskeytysaikaDTO = keskeytysaikaMapper.toDto(keskeytysaika)
        keskeytysaikaDTO.tyoskentelyjaksoId = tyoskentelyjakso.id
        restKeskeytysaikaMockMvc.perform(post("$API_TYOSKENTELYJAKSOT/poissaolot").contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(keskeytysaikaDTO)).with(csrf())).andExpect(status().isBadRequest)

        val keskeytysaikaList = keskeytysaikaRepository.findAll()
        assertThat(keskeytysaikaList).hasSize(tyoskentelyjaksoTableSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun createKeskeytysaikaWithInvalidDates() {
        initTest()

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)
        keskeytysaika = KeskeytysaikaHelper.createEntity(em, tyoskentelyjakso)
        keskeytysaika.alkamispaiva = LocalDate.of(2020, 1, 1)
        keskeytysaika.paattymispaiva = LocalDate.of(2020, 12, 1)

        val tyoskentelyjaksoTableSizeBeforeCreate = keskeytysaikaRepository.findAll().size
        var keskeytysaikaDTO = keskeytysaikaMapper.toDto(keskeytysaika)
        keskeytysaikaDTO.tyoskentelyjaksoId = tyoskentelyjakso.id
        restKeskeytysaikaMockMvc.perform(post("$API_TYOSKENTELYJAKSOT/poissaolot").contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(keskeytysaikaDTO)).with(csrf())).andExpect(status().isBadRequest)

        var keskeytysaikaList = keskeytysaikaRepository.findAll()
        assertThat(keskeytysaikaList).hasSize(tyoskentelyjaksoTableSizeBeforeCreate)

        keskeytysaika.alkamispaiva = LocalDate.of(2019, 12, 1)
        keskeytysaika.paattymispaiva = LocalDate.of(2020, 1, 10)

        keskeytysaikaDTO = keskeytysaikaMapper.toDto(keskeytysaika)
        keskeytysaikaDTO.tyoskentelyjaksoId = tyoskentelyjakso.id
        restKeskeytysaikaMockMvc.perform(post("$API_TYOSKENTELYJAKSOT/poissaolot").contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(keskeytysaikaDTO)).with(csrf())).andExpect(status().isBadRequest)

        keskeytysaikaList = keskeytysaikaRepository.findAll()
        assertThat(keskeytysaikaList).hasSize(tyoskentelyjaksoTableSizeBeforeCreate)

        keskeytysaika.alkamispaiva = LocalDate.of(2020, 1, 15)
        keskeytysaika.paattymispaiva = LocalDate.of(2020, 1, 10)

        keskeytysaikaDTO = keskeytysaikaMapper.toDto(keskeytysaika)
        keskeytysaikaDTO.tyoskentelyjaksoId = tyoskentelyjakso.id
        restKeskeytysaikaMockMvc.perform(post("$API_TYOSKENTELYJAKSOT/poissaolot").contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(keskeytysaikaDTO)).with(csrf())).andExpect(status().isBadRequest)

        keskeytysaikaList = keskeytysaikaRepository.findAll()
        assertThat(keskeytysaikaList).hasSize(tyoskentelyjaksoTableSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun updateKeskeytysaika() {
        initTest()

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        keskeytysaika = KeskeytysaikaHelper.createEntity(em, tyoskentelyjakso)
        keskeytysaikaRepository.saveAndFlush(keskeytysaika)

        val tyoskentelyjaksoTableSizeBeforeUpdate = keskeytysaikaRepository.findAll().size
        val id = keskeytysaika.id
        assertNotNull(id)

        val updatedKeskeytysaika = keskeytysaikaRepository.findById(id).get()
        em.detach(updatedKeskeytysaika)
        updatedKeskeytysaika.alkamispaiva = KeskeytysaikaHelper.UPDATED_ALKAMISPAIVA
        updatedKeskeytysaika.paattymispaiva = KeskeytysaikaHelper.UPDATED_PAATTYMISPAIVA
        updatedKeskeytysaika.poissaoloprosentti = KeskeytysaikaHelper.UPDATED_POISSAOLOPROSENTTI
        val keskeytysaikaDTO = keskeytysaikaMapper.toDto(updatedKeskeytysaika)

        restKeskeytysaikaMockMvc.perform(put("$API_TYOSKENTELYJAKSOT/poissaolot").contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(keskeytysaikaDTO)).with(csrf())).andExpect(status().isOk)

        val keskeytysaikaList = keskeytysaikaRepository.findAll()
        assertThat(keskeytysaikaList).hasSize(tyoskentelyjaksoTableSizeBeforeUpdate)
        val testKeskeytysaika = keskeytysaikaList[keskeytysaikaList.size - 1]
        assertThat(testKeskeytysaika.alkamispaiva).isEqualTo(KeskeytysaikaHelper.UPDATED_ALKAMISPAIVA)
        assertThat(testKeskeytysaika.paattymispaiva).isEqualTo(KeskeytysaikaHelper.UPDATED_PAATTYMISPAIVA)
        assertThat(testKeskeytysaika.poissaoloprosentti).isEqualTo(KeskeytysaikaHelper.UPDATED_POISSAOLOPROSENTTI)
    }

    @Test
    @Transactional
    fun updateKeskeytysaikaWithoutId() {
        initTest()

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        keskeytysaika = KeskeytysaikaHelper.createEntity(em, tyoskentelyjakso)
        keskeytysaikaRepository.saveAndFlush(keskeytysaika)

        val tyoskentelyjaksoTableSizeBeforeUpdate = keskeytysaikaRepository.findAll().size

        val id = keskeytysaika.id
        assertNotNull(id)

        val updatedKeskeytysaika = keskeytysaikaRepository.findById(id).get()
        em.detach(updatedKeskeytysaika)
        updatedKeskeytysaika.id = null
        updatedKeskeytysaika.alkamispaiva = KeskeytysaikaHelper.UPDATED_ALKAMISPAIVA
        updatedKeskeytysaika.paattymispaiva = KeskeytysaikaHelper.UPDATED_PAATTYMISPAIVA
        updatedKeskeytysaika.poissaoloprosentti = KeskeytysaikaHelper.UPDATED_POISSAOLOPROSENTTI
        val keskeytysaikaDTO = keskeytysaikaMapper.toDto(updatedKeskeytysaika)

        restKeskeytysaikaMockMvc.perform(put("$API_TYOSKENTELYJAKSOT/poissaolot").contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(keskeytysaikaDTO)).with(csrf())).andExpect(status().isBadRequest)

        val keskeytysaikaList = keskeytysaikaRepository.findAll()
        assertThat(keskeytysaikaList).hasSize(tyoskentelyjaksoTableSizeBeforeUpdate)
    }

    @Test
    @Transactional
    fun deleteKeskeytysaika() {
        initTest()

        keskeytysaika = KeskeytysaikaHelper.createEntity(em, tyoskentelyjakso)
        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)
        keskeytysaikaRepository.saveAndFlush(keskeytysaika)

        val keskeytysaikaTableSizeBeforeDelete = keskeytysaikaRepository.findAll().size

        restKeskeytysaikaMockMvc.perform(delete("$API_TYOSKENTELYJAKSOT/poissaolot/{id}", keskeytysaika.id).accept(MediaType.APPLICATION_JSON)
            .with(csrf())).andExpect(status().isNoContent)

        val keskeytysaikaList = keskeytysaikaRepository.findAll()
        assertThat(keskeytysaikaList).hasSize(keskeytysaikaTableSizeBeforeDelete - 1)
    }

    fun initTest(userId: String? = null) {
        user = KayttajaResourceWithMockUserIT.createEntity()
        em.persist(user)
        em.flush()
        val userDetails = mapOf<String, List<Any>>()
        val authorities = listOf(SimpleGrantedAuthority(YEK_KOULUTETTAVA))
        val authentication = Saml2Authentication(DefaultSaml2AuthenticatedPrincipal(userId ?: user.id, userDetails), "test", authorities)
        TestSecurityContextHolder.getContext().authentication = authentication
        tyoskentelyjakso = YekKoulutettavaTyoskentelyjaksoHelper.createEntity(em, user)
    }
}

