package fi.elsapalvelu.elsa.config

import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.seuranta.Ilmoitus
import fi.elsapalvelu.elsa.repository.seuranta.IlmoitusRepository
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import org.assertj.core.api.Assertions.assertThat
import org.hibernate.SessionFactory
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@AutoConfigureMockMvc
@SpringBootTest(classes = [ElsaBackendApp::class])
class RichTextPublicReadIT {
    @Autowired private lateinit var mvc: MockMvc
    @Autowired private lateinit var repository: IlmoitusRepository
    @Autowired private lateinit var entityManager: EntityManager
    @Autowired private lateinit var entityManagerFactory: EntityManagerFactory
    @Autowired private lateinit var objectMapper: ObjectMapper

    @Test
    @Transactional
    fun `anonymous public reads clean historical HTML without updating stored data`() {
        val dirty = "<p onclick='alert(1)'>Hyvää</p><script>alert(1)</script>"
        val saved = repository.saveAndFlush(Ilmoitus(teksti = dirty))
        entityManager.clear()
        val stats = entityManagerFactory.unwrap(SessionFactory::class.java).statistics
        val enabled = stats.isStatisticsEnabled
        stats.isStatisticsEnabled = true
        stats.clear()
        try {
            val list = mvc.perform(get("/api/julkinen/ilmoitukset").with(anonymous()))
                .andExpect(status().isOk).andReturn().response.contentAsString
            val item = objectMapper.readTree(list).first { it["id"].asLong() == saved.id }
            assertThat(item["teksti"].asText()).isEqualTo("<p>Hyvää</p>")
            val detail = mvc.perform(get("/api/julkinen/ilmoitukset/${saved.id}").with(anonymous()))
                .andExpect(status().isOk).andReturn().response.contentAsString
            assertThat(objectMapper.readTree(detail)["teksti"].asText()).isEqualTo("<p>Hyvää</p>")
            entityManager.flush()
            assertThat(stats.entityUpdateCount).isZero()
            assertThat(stats.entityInsertCount).isZero()
            assertThat(stats.entityDeleteCount).isZero()
            entityManager.clear()
            assertThat(repository.findById(saved.id!!).orElseThrow().teksti).isEqualTo(dirty)
        } finally {
            stats.isStatisticsEnabled = enabled
        }
    }
}
