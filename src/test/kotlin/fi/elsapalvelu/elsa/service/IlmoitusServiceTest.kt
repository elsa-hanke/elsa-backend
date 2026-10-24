package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.domain.seuranta.Ilmoitus
import fi.elsapalvelu.elsa.repository.seuranta.IlmoitusRepository
import fi.elsapalvelu.elsa.service.dto.seuranta.IlmoitusDTO
import fi.elsapalvelu.elsa.service.impl.seuranta.IlmoitusServiceImpl
import fi.elsapalvelu.elsa.service.mapper.seuranta.IlmoitusMapper
import fi.elsapalvelu.elsa.web.rest.tekninenpaakayttaja.TekninenPaakayttajaIlmoituksetResource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.never
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestExceptionAdvice
import org.mapstruct.factory.Mappers
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.util.Optional

class IlmoitusServiceTest {
    private val repository: IlmoitusRepository = mock()
    private val service = IlmoitusServiceImpl(repository, Mappers.getMapper(IlmoitusMapper::class.java))
    private val malicious = "<p onclick='alert(1)'><strong>Hyvää</strong></p><script>alert(1)</script>"
    private val safe = "<p><strong>Hyvää</strong></p>"
    private var savedText: String? = null
    private val existing = Ilmoitus(id = 42, teksti = malicious)

    @BeforeEach
    fun setup() {
        whenever(repository.save(any<Ilmoitus>())).thenAnswer {
            val entity = it.getArgument<Ilmoitus>(0)
            savedText = entity.teksti
            entity
        }
        whenever(repository.findById(42)).thenReturn(Optional.of(existing))
    }

    @Test
    fun `create sanitizes before repository save`() {
        val result = service.create(IlmoitusDTO(teksti = malicious))
        assertSavedSafe()
        assertThat(result.teksti).isEqualTo(safe)
    }

    @Test
    fun `update sanitizes before repository save`() {
        val result = service.update(IlmoitusDTO(id = 42, teksti = malicious))
        assertSavedSafe()
        assertThat(result?.teksti).isEqualTo(safe)
    }

    @Test
    fun `reads protect historical DTOs without mutating persistent entities`() {
        whenever(repository.findAllByOrderByIdDesc()).thenReturn(listOf(existing))
        assertThat(service.findAll().single().teksti).isEqualTo(safe)
        assertThat(service.findOne(42)?.teksti).isEqualTo(safe)
        assertThat(existing.teksti).isEqualTo(malicious)
        verify(repository).findAllByOrderByIdDesc()
        verify(repository).findById(42)
        verifyNoMoreInteractions(repository)
    }

    @Test
    fun `raw JSON POST bypassing editor persists sanitized HTML`() {
        mvc().perform(post("/api/tekninen-paakayttaja/ilmoitukset")
            .contentType(MediaType.APPLICATION_JSON).content("""{"teksti":"$malicious"}"""))
            .andExpect(status().isOk)
        assertSavedSafe()
    }

    @Test
    fun `raw JSON PUT bypassing editor persists sanitized HTML`() {
        mvc().perform(put("/api/tekninen-paakayttaja/ilmoitukset")
            .contentType(MediaType.APPLICATION_JSON).content("""{"id":42,"teksti":"$malicious"}"""))
            .andExpect(status().isOk)
        assertSavedSafe()
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "<script>alert(1)</script>", "<img src=x>", "<p><br></p>", "<p>&nbsp; &#160;</p>"])
    fun `empty sanitized announcements are rejected for create and update`(html: String) {
        val json = com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(IlmoitusDTO(id = 42, teksti = html))
        mvc().perform(post("/api/tekninen-paakayttaja/ilmoitukset")
            .contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isBadRequest)
        mvc().perform(put("/api/tekninen-paakayttaja/ilmoitukset")
            .contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isBadRequest)
        verify(repository, never()).save(any<Ilmoitus>())
        assertThat(existing.teksti).isEqualTo(malicious)
    }

    @Test
    fun `unknown identifiers return no result and do not save`() {
        whenever(repository.findById(99)).thenReturn(Optional.empty())
        assertThat(service.findOne(99)).isNull()
        assertThat(service.update(IlmoitusDTO(id = 99, teksti = malicious))).isNull()
        verify(repository, never()).save(any<Ilmoitus>())
    }

    // Standalone MVC deliberately exercises deserialization/controller/service, not the security filter chain.
    private fun mvc() = MockMvcBuilders.standaloneSetup(TekninenPaakayttajaIlmoituksetResource(service))
        .setControllerAdvice(BadRequestExceptionAdvice()).build()

    private fun assertSavedSafe() {
        val captured = argumentCaptor<Ilmoitus>()
        verify(repository).save(captured.capture())
        assertThat(savedText).isEqualTo(safe)
        assertThat(captured.firstValue.teksti).isEqualTo(safe)
    }
}
