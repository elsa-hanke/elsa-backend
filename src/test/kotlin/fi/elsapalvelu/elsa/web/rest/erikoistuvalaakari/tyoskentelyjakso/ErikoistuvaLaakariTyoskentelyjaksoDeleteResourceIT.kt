package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari.tyoskentelyjakso

import fi.elsapalvelu.elsa.web.rest.API_TYOSKENTELYJAKSOT
import fi.elsapalvelu.elsa.web.rest.helpers.AsiakirjaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.AsiakirjaHelper.ASIAKIRJA_PDF_NIMI
import fi.elsapalvelu.elsa.web.rest.helpers.KoulutusjaksoHelper
import fi.elsapalvelu.elsa.web.rest.helpers.SuoritusarviointiHelper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType.*
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import kotlin.test.assertNotNull

class ErikoistuvaLaakariTyoskentelyjaksoDeleteResourceIT : ErikoistuvaLaakariTyoskentelyjaksoResourceTestBase() {

    @Test
    fun deleteTyoskentelyjakso() {
        initTest()
        persistTyoskentelyjakso(tyoskentelyjakso)
        val tyoskentelyjaksoTableSizeBeforeDelete = tyoskentelyjaksoRepository.findAll().size

        testMockMvc.perform(delete("$API_TYOSKENTELYJAKSOT/{id}", tyoskentelyjakso.id).accept(APPLICATION_JSON).with(csrf())).andExpect(status().isNoContent)
        assertThat(tyoskentelyjaksoRepository.findAll()).hasSize(tyoskentelyjaksoTableSizeBeforeDelete - 1)
    }

    @Test
    fun deleteAnotherUserTyoskentelyjakso() {
        initTest(createPersistedErikoistuvaLaakari().kayttaja?.user?.id)
        persistTyoskentelyjakso(tyoskentelyjakso)
        val tyoskentelyjaksoTableSizeBeforeDelete = tyoskentelyjaksoRepository.findAll().size

        testMockMvc.perform(delete("$API_TYOSKENTELYJAKSOT/{id}", tyoskentelyjakso.id).accept(APPLICATION_JSON).with(csrf())).andExpect(status().isForbidden)

        assertThat(tyoskentelyjaksoRepository.findAll()).hasSize(tyoskentelyjaksoTableSizeBeforeDelete)
    }

    @Test
    fun deleteAnotherUserTyoskentelyjaksoShouldNotDetachAsiakirjaOrKoulutusjakso() {
        initTest(createPersistedErikoistuvaLaakari().kayttaja?.user?.id)

        tyoskentelyjakso.asiakirjat.add(AsiakirjaHelper.createEntity(em, user, tyoskentelyjakso))
        val tyoskentelyjaksoId = persistTyoskentelyjakso(tyoskentelyjakso)
        val asiakirjaId = tyoskentelyjakso.asiakirjat.first().id
        assertNotNull(asiakirjaId)

        val koulutusjakso = KoulutusjaksoHelper.createEntity(em, user)
        koulutusjakso.tyoskentelyjaksot = mutableSetOf(tyoskentelyjakso)
        val koulutusjaksoId = persistKoulutusjakso(koulutusjakso)

        testMockMvc.perform(delete("$API_TYOSKENTELYJAKSOT/{id}", tyoskentelyjaksoId).accept(APPLICATION_JSON).with(csrf())).andExpect(status().isForbidden)
        flushClear()

        // Epäonnistunut poisto ei saa muuttaa toisen käyttäjän tietoja
        assertThat(tyoskentelyjaksoRepository.findById(tyoskentelyjaksoId)).isPresent
        assertThat(asiakirjaRepository.findById(asiakirjaId).orElseThrow().tyoskentelyjakso?.id).isEqualTo(tyoskentelyjaksoId)
        assertThat(asiakirjaRepository.findAllByTyoskentelyjaksoId(tyoskentelyjaksoId)).hasSize(1)
        assertThat(koulutusjaksoRepository.findAllByTyoskentelyjaksoId(tyoskentelyjaksoId)).hasSize(1)
        assertThat(koulutusjaksoRepository.findById(koulutusjaksoId).orElseThrow().tyoskentelyjaksot?.map { it.id }).containsExactly(tyoskentelyjaksoId)
    }

    @Test
    fun deleteNonExistentTyoskentelyjaksoShouldReturnForbidden() {
        initTest()
        testMockMvc.perform(delete("$API_TYOSKENTELYJAKSOT/{id}", Long.MAX_VALUE).accept(APPLICATION_JSON).with(csrf())).andExpect(status().isForbidden)
    }

    @Test
    fun deleteTyoskentelyjaksoWithSuoritusarviointi() {
        initTest()
        persistTyoskentelyjakso(tyoskentelyjakso)
        val tyoskentelyjaksoTableSizeBeforeDelete = tyoskentelyjaksoRepository.findAll().size

        em.detach(tyoskentelyjakso)
        suoritusarviointiRepository.saveAndFlush(SuoritusarviointiHelper.createEntity(em, user))

        testMockMvc.perform(delete("$API_TYOSKENTELYJAKSOT/{id}", tyoskentelyjakso.id).accept(APPLICATION_JSON).with(csrf())).andExpect(status().isBadRequest)

        assertThat(tyoskentelyjaksoRepository.findAll()).hasSize(tyoskentelyjaksoTableSizeBeforeDelete)
    }

    @Test
    fun deleteTyoskentelyjaksoShouldRemoveAsiakirjaReferenceAndKeepAsiakirja() {
        initTest()
        tyoskentelyjakso.asiakirjat.add(AsiakirjaHelper.createEntity(em, user, tyoskentelyjakso))
        val tyoskentelyjaksoId = persistTyoskentelyjakso(tyoskentelyjakso)
        val asiakirjaId = tyoskentelyjakso.asiakirjat.first().id
        assertNotNull(asiakirjaId)
        assertThat(asiakirjaRepository.findAllByTyoskentelyjaksoId(tyoskentelyjaksoId)).hasSize(1)

        testMockMvc.perform(delete("$API_TYOSKENTELYJAKSOT/{id}", tyoskentelyjaksoId).accept(APPLICATION_JSON).with(csrf())).andExpect(status().isNoContent)
        flushClear()

        // Työskentelyjakso poistetaan, mutta asiakirja säilyy ilman viittausta työskentelyjaksoon
        assertThat(tyoskentelyjaksoRepository.findById(tyoskentelyjaksoId)).isEmpty
        assertThat(asiakirjaRepository.findAllByTyoskentelyjaksoId(tyoskentelyjaksoId)).isEmpty()

        val asiakirja = asiakirjaRepository.findById(asiakirjaId).orElse(null)
        assertNotNull(asiakirja)
        assertThat(asiakirja.tyoskentelyjakso).isNull()
        assertThat(asiakirja.nimi).isEqualTo(ASIAKIRJA_PDF_NIMI)
    }

    @Test
    fun deleteTyoskentelyjaksoShouldRemoveBothReferencesAndKeepEntities() {
        initTest()

        tyoskentelyjakso.asiakirjat.add(AsiakirjaHelper.createEntity(em, user, tyoskentelyjakso))
        val tyoskentelyjaksoId = persistTyoskentelyjakso(tyoskentelyjakso)
        val asiakirjaId = tyoskentelyjakso.asiakirjat.first().id
        assertNotNull(asiakirjaId)

        val koulutusjakso = KoulutusjaksoHelper.createEntity(em, user)
        koulutusjakso.tyoskentelyjaksot = mutableSetOf(tyoskentelyjakso)
        val koulutusjaksoId = persistKoulutusjakso(koulutusjakso)

        testMockMvc.perform(delete("$API_TYOSKENTELYJAKSOT/{id}", tyoskentelyjaksoId).accept(APPLICATION_JSON).with(csrf())).andExpect(status().isNoContent)
        flushClear()

        assertThat(tyoskentelyjaksoRepository.findById(tyoskentelyjaksoId)).isEmpty
        assertThat(asiakirjaRepository.findById(asiakirjaId).orElseThrow().tyoskentelyjakso).isNull()
        assertThat(koulutusjaksoRepository.findById(koulutusjaksoId).orElseThrow().tyoskentelyjaksot ?: mutableSetOf()).isEmpty()
    }

    @Test
    fun deleteOwnTyoskentelyjaksoWithSuoritusarviointiShouldKeepReferences() {
        initTest()

        tyoskentelyjakso.asiakirjat.add(AsiakirjaHelper.createEntity(em, user, tyoskentelyjakso))
        val tyoskentelyjaksoId = persistTyoskentelyjakso(tyoskentelyjakso)
        val asiakirjaId = tyoskentelyjakso.asiakirjat.first().id
        assertNotNull(asiakirjaId)

        em.detach(tyoskentelyjakso)
        suoritusarviointiRepository.saveAndFlush(SuoritusarviointiHelper.createEntity(em, user))

        testMockMvc.perform(delete("$API_TYOSKENTELYJAKSOT/{id}", tyoskentelyjaksoId).accept(APPLICATION_JSON).with(csrf())).andExpect(status().isBadRequest)
        flushClear()

        // Poisto estyy, jolloin viittauksiakaan ei saa irrottaa
        assertThat(tyoskentelyjaksoRepository.findById(tyoskentelyjaksoId)).isPresent
        assertThat(asiakirjaRepository.findById(asiakirjaId).orElseThrow().tyoskentelyjakso?.id).isEqualTo(tyoskentelyjaksoId)
    }

    @Test
    fun deleteTyoskentelyjaksoShouldRemoveKoulutusjaksoReferenceAndKeepKoulutusjakso() {
        initTest()
        val tyoskentelyjaksoId = persistTyoskentelyjakso(tyoskentelyjakso)

        val koulutusjakso = KoulutusjaksoHelper.createEntity(em, user)
        koulutusjakso.tyoskentelyjaksot = mutableSetOf(tyoskentelyjakso)
        val koulutusjaksoId = persistKoulutusjakso(koulutusjakso)
        assertThat(koulutusjaksoRepository.findAllByTyoskentelyjaksoId(tyoskentelyjaksoId)).hasSize(1)

        testMockMvc.perform(delete("$API_TYOSKENTELYJAKSOT/{id}", tyoskentelyjaksoId).accept(APPLICATION_JSON).with(csrf())).andExpect(status().isNoContent)
        flushClear()

        // Työskentelyjakso poistetaan, mutta koulutusjakso säilyy ilman viittausta työskentelyjaksoon
        assertThat(tyoskentelyjaksoRepository.findById(tyoskentelyjaksoId)).isEmpty
        assertThat(koulutusjaksoRepository.findAllByTyoskentelyjaksoId(tyoskentelyjaksoId)).isEmpty()

        val paivitettyKoulutusjakso = koulutusjaksoRepository.findById(koulutusjaksoId).orElse(null)
        assertNotNull(paivitettyKoulutusjakso)
        assertThat(paivitettyKoulutusjakso.tyoskentelyjaksot ?: mutableSetOf()).isEmpty()
    }
}
