package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari.tyoskentelyjakso

import fi.elsapalvelu.elsa.web.rest.API_TYOSKENTELYJAKSOT
import fi.elsapalvelu.elsa.web.rest.helpers.AsiakirjaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.AsiakirjaHelper.ASIAKIRJA_PNG_NIMI
import fi.elsapalvelu.elsa.web.rest.helpers.AsiakirjaHelper.ASIAKIRJA_PNG_TYYPPI
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType.*
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import kotlin.test.assertNotNull

class ErikoistuvaLaakariTyoskentelyjaksoAsiakirjaResourceIT : ErikoistuvaLaakariTyoskentelyjaksoResourceTestBase() {

    @Test
    fun updateTyoskentelyjaksoAsiakirjat() {
        initTest()
        initMockFiles()

        val asiakirja = AsiakirjaHelper.createEntity(em, user, tyoskentelyjakso)
        tyoskentelyjakso.asiakirjat.add(asiakirja)

        val tyoskentelyjaksoId = persistTyoskentelyjakso(tyoskentelyjakso)
        val poistettavaAsiakirjaId = asiakirja.id
        assertNotNull(poistettavaAsiakirjaId)

        testMockMvc.perform(multipart("$API_TYOSKENTELYJAKSOT/${tyoskentelyjakso.id}/asiakirjat").file(MockMultipartFile("addedFiles",
            ASIAKIRJA_PNG_NIMI, ASIAKIRJA_PNG_TYYPPI, tempFile2.readBytes())).param("deletedFiles", poistettavaAsiakirjaId.toString()).with { it.method = "PUT"; it }
            .with(csrf())).andExpect(status().isOk).andExpect(content().contentType(APPLICATION_JSON_VALUE)).andExpect(jsonPath("$.asiakirjat").value(
            hasSize<Any>(1)
        ))
            .andExpect(jsonPath("$.asiakirjat[0].nimi").value(ASIAKIRJA_PNG_NIMI))
        flushClear()

        assertThat(asiakirjaRepository.findById(poistettavaAsiakirjaId)).isEmpty
        val jaljellaOlevat = asiakirjaRepository.findAllByTyoskentelyjaksoId(tyoskentelyjaksoId)
        assertThat(jaljellaOlevat).hasSize(1)
        assertThat(jaljellaOlevat.first().nimi).isEqualTo(ASIAKIRJA_PNG_NIMI)
    }

    @Test
    fun updateAnotherUserTyoskentelyjaksoAsiakirjatShouldReturnForbiddenAndNotAddFile() {
        initTest(createPersistedErikoistuvaLaakari().kayttaja?.user?.id)
        initMockFiles()

        val asiakirja = AsiakirjaHelper.createEntity(em, user, tyoskentelyjakso)
        tyoskentelyjakso.asiakirjat.add(asiakirja)
        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        testMockMvc.perform(multipart("$API_TYOSKENTELYJAKSOT/${tyoskentelyjakso.id}/asiakirjat")
            .file(MockMultipartFile("addedFiles", ASIAKIRJA_PNG_NIMI, ASIAKIRJA_PNG_TYYPPI, tempFile2.readBytes()))
            .param("deletedFiles", asiakirja.id!!.toString()).with { it.method = "PUT"; it }.with(csrf())).andExpect(status().isForbidden)

        val unchangedTyoskentelyjakso = tyoskentelyjaksoRepository.findById(tyoskentelyjakso.id!!).get()
        assertThat(unchangedTyoskentelyjakso.asiakirjat).hasSize(1)
        assertThat(unchangedTyoskentelyjakso.asiakirjat.first().id).isEqualTo(asiakirja.id)
    }

    @Test
    fun updateAnotherUserTyoskentelyjaksoAsiakirjatShouldNotDeleteFile() {
        initTest(createPersistedErikoistuvaLaakari().kayttaja?.user?.id)

        val asiakirja = AsiakirjaHelper.createEntity(em, user, tyoskentelyjakso)
        tyoskentelyjakso.asiakirjat.add(asiakirja)
        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        testMockMvc.perform(multipart("$API_TYOSKENTELYJAKSOT/${tyoskentelyjakso.id}/asiakirjat")
            .param("deletedFiles", asiakirja.id!!.toString()).with { it.method = "PUT"; it }.with(csrf())).andExpect(status().isForbidden)

        val unchangedTyoskentelyjakso = tyoskentelyjaksoRepository.findById(tyoskentelyjakso.id!!).get()
        assertThat(unchangedTyoskentelyjakso.asiakirjat).hasSize(1)
        assertThat(unchangedTyoskentelyjakso.asiakirjat.first().id).isEqualTo(asiakirja.id)
    }
}
