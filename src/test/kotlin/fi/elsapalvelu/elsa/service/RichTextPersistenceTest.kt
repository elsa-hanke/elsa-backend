package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.domain.arviointi.Arviointityokalu
import fi.elsapalvelu.elsa.domain.arviointi.ArvioitavaKokonaisuus
import fi.elsapalvelu.elsa.domain.arviointi.SuoritusarvioinninArvioitavaKokonaisuus
import fi.elsapalvelu.elsa.repository.arviointi.ArviointityokaluRepository
import fi.elsapalvelu.elsa.repository.arviointi.ArvioitavaKokonaisuusRepository
import fi.elsapalvelu.elsa.service.dto.arviointi.ArviointityokaluDTO
import fi.elsapalvelu.elsa.service.dto.arviointi.ArvioitavaKokonaisuusDTO
import fi.elsapalvelu.elsa.service.dto.kayttaja.UserDTO
import fi.elsapalvelu.elsa.service.impl.arviointi.ArviointityokaluServiceImpl
import fi.elsapalvelu.elsa.service.impl.arviointi.ArvioitavaKokonaisuusServiceImpl
import fi.elsapalvelu.elsa.service.mapper.arviointi.ArviointityokaluMapper
import fi.elsapalvelu.elsa.service.mapper.arviointi.ArvioitavaKokonaisuusMapper
import fi.elsapalvelu.elsa.service.mapper.arviointi.ArvioitavaKokonaisuusWithErikoisalaMapper
import fi.elsapalvelu.elsa.service.mapper.arviointi.SuoritusarvioinninArvioitavaKokonaisuusMapper
import fi.elsapalvelu.elsa.web.rest.tekninenpaakayttaja.TekninenPaakayttajaOpetussuunnitelmatResource
import fi.elsapalvelu.elsa.web.rest.tekninenpaakayttaja.TekninenPaakayttajaArviointityokalutResource
import fi.elsapalvelu.elsa.service.kayttaja.UserService
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.mockito.kotlin.anyOrNull
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import fi.elsapalvelu.elsa.service.mapper.arviointi.SuoritusarviointiMapper
import fi.elsapalvelu.elsa.domain.arviointi.Suoritusarviointi
import java.time.LocalDate
import java.util.Optional

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RichTextPersistenceTest {
    // Real generated mappers, including the mappers used for nested DTOs. No database is involved.
    private val context = AnnotationConfigApplicationContext("fi.elsapalvelu.elsa.service.mapper")
    private val blockMapper = context.getBean(ArvioitavaKokonaisuusMapper::class.java)
    private val detailMapper = context.getBean(ArvioitavaKokonaisuusWithErikoisalaMapper::class.java)
    private val toolMapper = context.getBean(ArviointityokaluMapper::class.java)
    private val dirty = "<p onclick='alert(1)'><strong>Hyvää</strong></p><script>alert(1)</script>"
    private val safe = "<p><strong>Hyvää</strong></p>"

    @AfterAll
    fun close() = context.close()

    @Test
    fun `assessment block create replacement and update sanitize both languages before every save`() {
        val repository = mock<ArvioitavaKokonaisuusRepository>()
        val old = ArvioitavaKokonaisuus(id = 42, kuvaus = dirty, kuvausSv = dirty)
        whenever(repository.findById(42)).thenReturn(Optional.of(old))
        val snapshots = mutableListOf<Pair<String?, String?>>()
        whenever(repository.save(any<ArvioitavaKokonaisuus>())).thenAnswer {
            it.getArgument<ArvioitavaKokonaisuus>(0).also { entity ->
                snapshots.add(entity.kuvaus to entity.kuvausSv)
                entity.id = entity.id ?: 43
            }
        }
        val service = ArvioitavaKokonaisuusServiceImpl(repository, blockMapper, detailMapper, mock(), mock(), mock())
        val dto = ArvioitavaKokonaisuusDTO(kuvaus = dirty, kuvausSv = dirty, voimassaoloAlkaa = LocalDate.now())
        service.create(dto.copy())
        service.create(dto.copy(id = 42))
        service.update(dto.copy(id = 42))
        assertThat(snapshots).hasSize(4).allSatisfy { assertThat(it).isEqualTo(safe to safe) }
        assertThat(dto.kuvaus).isEqualTo(dirty)
    }

    @Test
    fun `assessment tool creation and versioning sanitize new and historical versions before every save`() {
        val repository = mock<ArviointityokaluRepository>()
        val old = Arviointityokalu(id = 42, ohjeteksti = dirty)
        whenever(repository.findById(42)).thenReturn(Optional.of(old))
        val snapshots = mutableListOf<String?>()
        whenever(repository.save(any<Arviointityokalu>())).thenAnswer {
            it.getArgument<Arviointityokalu>(0).also { entity ->
                snapshots.add(entity.ohjeteksti)
                entity.id = entity.id ?: 43
            }
        }
        val service = ArviointityokaluServiceImpl(repository, toolMapper, mock())
        service.save(ArviointityokaluDTO(ohjeteksti = dirty), UserDTO(), null)
        service.update(ArviointityokaluDTO(id = 42, ohjeteksti = dirty), null)
        assertThat(snapshots).hasSize(4).allMatch { it == safe }
    }

    @Test
    fun `direct JSON block requests bypassing editor persist only safe descriptions`() {
        val repository = mock<ArvioitavaKokonaisuusRepository>()
        whenever(repository.findById(42)).thenReturn(Optional.of(ArvioitavaKokonaisuus(id = 42)))
        val snapshots = mutableListOf<Pair<String?, String?>>()
        whenever(repository.save(any<ArvioitavaKokonaisuus>())).thenAnswer {
            it.getArgument<ArvioitavaKokonaisuus>(0).also { e ->
                snapshots.add(e.kuvaus to e.kuvausSv)
                e.id = e.id ?: 43
            }
        }
        val service = ArvioitavaKokonaisuusServiceImpl(repository, blockMapper, detailMapper, mock(), mock(), mock())
        val mvc = MockMvcBuilders.standaloneSetup(TekninenPaakayttajaOpetussuunnitelmatResource(
            mock(), mock(), mock(), mock(), service, mock(), mock(), mock(), mock()
        )).build()
        val json = ObjectMapper().writeValueAsString(mapOf(
            "nimi" to "Block", "voimassaoloAlkaa" to "2026-01-01", "kategoria" to mapOf("id" to 1),
            "kuvaus" to dirty, "kuvausSv" to dirty
        ))
        mvc.perform(post("/api/tekninen-paakayttaja/arvioitavakokonaisuus")
            .contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isCreated)
        mvc.perform(put("/api/tekninen-paakayttaja/arvioitavakokonaisuus")
            .contentType(MediaType.APPLICATION_JSON).content(json.replaceFirst("{", "{\"id\":42,")))
            .andExpect(status().isOk)
        assertThat(snapshots).hasSize(2).allSatisfy { assertThat(it).isEqualTo(safe to safe) }
    }

    @Test
    fun `direct multipart tool requests bypassing editor sanitize all saved versions`() {
        val repository = mock<ArviointityokaluRepository>()
        whenever(repository.findById(42)).thenReturn(Optional.of(Arviointityokalu(id = 42, ohjeteksti = dirty)))
        val snapshots = mutableListOf<String?>()
        whenever(repository.save(any<Arviointityokalu>())).thenAnswer {
            it.getArgument<Arviointityokalu>(0).also { e ->
                snapshots.add(e.ohjeteksti)
                e.id = e.id ?: 43
            }
        }
        val service = ArviointityokaluServiceImpl(repository, toolMapper, mock())
        val users = mock<UserService>()
        whenever(users.getAuthenticatedUser(anyOrNull())).thenReturn(UserDTO())
        val jsonMapper = ObjectMapper().findAndRegisterModules()
        val mvc = MockMvcBuilders.standaloneSetup(
            TekninenPaakayttajaArviointityokalutResource(service, mock(), users, jsonMapper)
        ).build()
        val createJson = jsonMapper.writeValueAsBytes(ArviointityokaluDTO(ohjeteksti = dirty))
        mvc.perform(multipart("/api/tekninen-paakayttaja/arviointityokalu")
            .file(MockMultipartFile("data", "", "text/plain;charset=UTF-8", createJson))).andExpect(status().isOk)
        val updateJson = jsonMapper.writeValueAsBytes(ArviointityokaluDTO(id = 42, ohjeteksti = dirty))
        mvc.perform(multipart("/api/tekninen-paakayttaja/arviointityokalu")
            .file(MockMultipartFile("data", "", "text/plain;charset=UTF-8", updateJson))
            .with { it.method = "PATCH"; it }).andExpect(status().isOk)
        assertThat(snapshots).hasSize(4).allMatch { it == safe }
    }

    @Test
    fun `old fields are safe in direct detail and nested DTOs without mutating source entities`() {
        val block = ArvioitavaKokonaisuus(id = 42, kuvaus = dirty, kuvausSv = dirty)
        val tool = Arviointityokalu(id = 43, ohjeteksti = dirty)
        assertThat(blockMapper.toDto(block).kuvaus).isEqualTo(safe)
        assertThat(blockMapper.toDto(block).kuvausSv).isEqualTo(safe)
        assertThat(detailMapper.toDto(block).kuvaus).isEqualTo(safe)
        assertThat(detailMapper.toDto(block).kuvausSv).isEqualTo(safe)
        assertThat(toolMapper.toDto(tool).ohjeteksti).isEqualTo(safe)
        val nested = context.getBean(SuoritusarvioinninArvioitavaKokonaisuusMapper::class.java)
            .toDto(SuoritusarvioinninArvioitavaKokonaisuus(arvioitavaKokonaisuus = block))
        assertThat(nested.arvioitavaKokonaisuus?.kuvaus).isEqualTo(safe)
        assertThat(nested.arvioitavaKokonaisuus?.kuvausSv).isEqualTo(safe)
        val assessment = context.getBean(SuoritusarviointiMapper::class.java)
            .toDto(Suoritusarviointi(arviointityokalut = mutableSetOf(tool)))
        assertThat(assessment.arviointityokalut?.single()?.ohjeteksti).isEqualTo(safe)
        assertThat(block.kuvaus).isEqualTo(dirty)
        assertThat(block.kuvausSv).isEqualTo(dirty)
        assertThat(tool.ohjeteksti).isEqualTo(dirty)
        assertThat(toolMapper.toDto(Arviointityokalu()).ohjeteksti).isNull()
    }
}
