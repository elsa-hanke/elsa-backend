package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari.tyoskentelyjakso

import fi.elsapalvelu.elsa.domain.perustiedot.Erikoisala
import fi.elsapalvelu.elsa.domain.tyoskentely.Tyoskentelyjakso
import fi.elsapalvelu.elsa.domain.tyoskentely.Tyoskentelypaikka
import fi.elsapalvelu.elsa.service.dto.tyoskentely.TyoskentelyjaksoDTO
import fi.elsapalvelu.elsa.web.rest.API_TYOSKENTELYJAKSOT
import fi.elsapalvelu.elsa.web.rest.convertObjectToJsonBytes
import fi.elsapalvelu.elsa.web.rest.findAll
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariTyoskentelyjaksoHelper
import fi.elsapalvelu.elsa.web.rest.helpers.SuoritusarviointiHelper
import fi.elsapalvelu.elsa.web.rest.helpers.TyoskentelypaikkaHelper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType.*
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDate
import kotlin.test.assertFalse

class ErikoistuvaLaakariTyoskentelyjaksoUpdateResourceIT : ErikoistuvaLaakariTyoskentelyjaksoResourceTestBase() {

    @Test
    fun updateTyoskentelyjaksoWithoutSuoritusarvioinnit() {
        initTest()
        persistTyoskentelyjakso(tyoskentelyjakso)
        val tyoskentelyjaksoTableSizeBeforeUpdate = tyoskentelyjaksoRepository.findAll().size

        val existingTyoskentelyjakso = tyoskentelyjaksoRepository.findById(tyoskentelyjakso.id!!).get()
        val updatedTyoskentelypaikka = TyoskentelypaikkaHelper.createUpdatedEntity(em)
        val omaaErikoisalaaTukeva = em.findAll(Erikoisala::class).first()

        val tyoskentelyjaksoDTO = populateTyoskentelyjaksoDto(existingTyoskentelyjakso, updatedTyoskentelypaikka, omaaErikoisalaaTukeva)

        testMockMvc.perform(put(API_TYOSKENTELYJAKSOT).param("tyoskentelyjaksoJson", objectMapper.writeValueAsString(tyoskentelyjaksoDTO)).with(csrf())).andExpect(status().isOk)

        flushClear()
        val tyoskentelyjaksoList = tyoskentelyjaksoRepository.findAll()
        assertThat(tyoskentelyjaksoList).hasSize(tyoskentelyjaksoTableSizeBeforeUpdate)
        val testTyoskentelyjakso = tyoskentelyjaksoRepository.findById(tyoskentelyjakso.id!!).get()

        assertUpdatedTyoskentelyjakso(testTyoskentelyjakso, updatedTyoskentelypaikka)
        assertThat(testTyoskentelyjakso.omaaErikoisalaaTukeva).isEqualTo(omaaErikoisalaaTukeva)
        assertThat(testTyoskentelyjakso.hyvaksyttyAiempaanErikoisalaan).isEqualTo(ErikoistuvaLaakariTyoskentelyjaksoHelper.UPDATED_HYVAKSYTTY_AIEMPAAN_ERIKOISALAAN)
    }

    @Test
    fun updateTyoskentelyjaksoWithSuoritusarvioinnitShouldUpdateOnlyPaattymispaiva() {
        initTest()
        assertThat(tyoskentelyjakso.suoritusarvioinnit).isEmpty()
        tyoskentelyjakso.suoritusarvioinnit.add(SuoritusarviointiHelper.createEntity(em, user, ErikoistuvaLaakariTyoskentelyjaksoHelper.UPDATED_ALKAMISPAIVA.plusDays(1)))
        persistTyoskentelyjakso(tyoskentelyjakso)

        val tyoskentelyjaksoTableSizeBeforeUpdate = tyoskentelyjaksoRepository.findAll().size

        val existingTyoskentelyjakso = tyoskentelyjaksoRepository.findById(tyoskentelyjakso.id!!).get()
        val updatedTyoskentelypaikka = TyoskentelypaikkaHelper.createUpdatedEntity(em)
        val omaaErikoisalaaTukeva = em.findAll(Erikoisala::class).first()
        val tyoskentelyjaksoJson = objectMapper.writeValueAsString(populateTyoskentelyjaksoDto(existingTyoskentelyjakso, updatedTyoskentelypaikka, omaaErikoisalaaTukeva))
        testMockMvc.perform(put(API_TYOSKENTELYJAKSOT).param("tyoskentelyjaksoJson", tyoskentelyjaksoJson).with(csrf())).andExpect(status().isOk)

        flushClear()
        val tyoskentelyjaksoList = tyoskentelyjaksoRepository.findAll()
        assertThat(tyoskentelyjaksoList).hasSize(tyoskentelyjaksoTableSizeBeforeUpdate)
        val testTyoskentelyjakso = tyoskentelyjaksoRepository.findById(tyoskentelyjakso.id!!).get()

        assertUpdatedTyoskentelyjakso(testTyoskentelyjakso, updatedTyoskentelypaikka)
        assertThat(testTyoskentelyjakso.omaaErikoisalaaTukeva?.id).isEqualTo(omaaErikoisalaaTukeva.id)
        assertThat(testTyoskentelyjakso.hyvaksyttyAiempaanErikoisalaan).isEqualTo(ErikoistuvaLaakariTyoskentelyjaksoHelper.UPDATED_HYVAKSYTTY_AIEMPAAN_ERIKOISALAAN)
    }

    @Test
    fun updateTyoskentelyjaksoWithSuoritusarvioinnitAndInvalidPaattymispaiva() {
        initTest()

        assertThat(tyoskentelyjakso.suoritusarvioinnit).isEmpty()
        tyoskentelyjakso.suoritusarvioinnit.add(SuoritusarviointiHelper.createEntity(em, user, LocalDate.of(2020, 1, 20)))
        persistTyoskentelyjakso(tyoskentelyjakso)

        val tyoskentelyjaksoDTO = tyoskentelyjaksoMapper.toDto(tyoskentelyjaksoRepository.findById(tyoskentelyjakso.id!!).get())

        // Asetetaan päättymispäivä suoritusarvioinnin ulkopuolelle
        tyoskentelyjaksoDTO.paattymispaiva = LocalDate.of(2020, 1, 19)

        val tyoskentelyjaksoJson = objectMapper.writeValueAsString(tyoskentelyjaksoDTO)
        testMockMvc.perform(put(API_TYOSKENTELYJAKSOT).param("tyoskentelyjaksoJson", tyoskentelyjaksoJson).with(csrf())).andExpect(status().isBadRequest)
    }

    @Test
    fun updateTyoskentelyjaksoWithSuoritusarvioinnitAndValidPaattymispaiva() {
        initTest()

        assertThat(tyoskentelyjakso.suoritusarvioinnit).isEmpty()

        val suoritusarviointiTapahtumanAjankohta = LocalDate.of(2020, 1, 20)
        tyoskentelyjakso.suoritusarvioinnit.add(SuoritusarviointiHelper.createEntity(em, user, suoritusarviointiTapahtumanAjankohta))
        persistTyoskentelyjakso(tyoskentelyjakso)

        val tyoskentelyjaksoDTO = tyoskentelyjaksoMapper.toDto(tyoskentelyjaksoRepository.findById(tyoskentelyjakso.id!!).get())

        // Asetetaan päättymispäivä suoritusarvioinnin päivälle
        tyoskentelyjaksoDTO.paattymispaiva = suoritusarviointiTapahtumanAjankohta

        val tyoskentelyjaksoJson = objectMapper.writeValueAsString(tyoskentelyjaksoDTO)

        testMockMvc.perform(put(API_TYOSKENTELYJAKSOT).param("tyoskentelyjaksoJson", tyoskentelyjaksoJson).with(csrf())).andExpect(status().isOk)
            .andExpect(content().contentType(APPLICATION_JSON_VALUE)).andExpect(jsonPath("$.paattymispaiva").value(suoritusarviointiTapahtumanAjankohta.toString()))
    }

    @Test
    fun updateTyoskentelyjaksoWithoutId() {
        initTest()
        persistTyoskentelyjakso(tyoskentelyjakso)
        val tyoskentelyjaksoTableSizeBeforeUpdate = tyoskentelyjaksoRepository.findAll().size

        val updatedTyoskentelyjakso = tyoskentelyjaksoRepository.findById(tyoskentelyjakso.id!!).get()
        em.detach(updatedTyoskentelyjakso)

        updatedTyoskentelyjakso.id = null
        updatedTyoskentelyjakso.alkamispaiva = ErikoistuvaLaakariTyoskentelyjaksoHelper.UPDATED_ALKAMISPAIVA
        updatedTyoskentelyjakso.paattymispaiva = ErikoistuvaLaakariTyoskentelyjaksoHelper.UPDATED_PAATTYMISPAIVA
        updatedTyoskentelyjakso.osaaikaprosentti = ErikoistuvaLaakariTyoskentelyjaksoHelper.UPDATED_OSAAIKAPROSENTTI
        updatedTyoskentelyjakso.kaytannonKoulutus = ErikoistuvaLaakariTyoskentelyjaksoHelper.UPDATED_KAYTANNON_KOULUTUS
        updatedTyoskentelyjakso.hyvaksyttyAiempaanErikoisalaan = ErikoistuvaLaakariTyoskentelyjaksoHelper.UPDATED_HYVAKSYTTY_AIEMPAAN_ERIKOISALAAN
        val tyoskentelyjaksoJson = objectMapper.writeValueAsString(tyoskentelyjaksoMapper.toDto(updatedTyoskentelyjakso))

        testMockMvc.perform(put(API_TYOSKENTELYJAKSOT).param("tyoskentelyjaksoJson", tyoskentelyjaksoJson).with(csrf())).andExpect(status().isBadRequest)

        assertThat(tyoskentelyjaksoRepository.findAll()).hasSize(tyoskentelyjaksoTableSizeBeforeUpdate)
    }

    @Test
    fun updateAnotherUserTyoskentelyjakso() {
        initTest(createPersistedErikoistuvaLaakari().kayttaja?.user?.id)

        persistTyoskentelyjakso(tyoskentelyjakso)
        val tyoskentelyjaksoTableSizeBeforeUpdate = tyoskentelyjaksoRepository.findAll().size
        val tyoskentelyjaksoJson = objectMapper.writeValueAsString(tyoskentelyjaksoMapper.toDto(tyoskentelyjakso))

        testMockMvc.perform(put(API_TYOSKENTELYJAKSOT).param("tyoskentelyjaksoJson", tyoskentelyjaksoJson).with(csrf())).andExpect(status().isBadRequest)

        assertThat(tyoskentelyjaksoRepository.findAll()).hasSize(tyoskentelyjaksoTableSizeBeforeUpdate)
    }

    @Test
    fun updateLiitettyKoejaksoon() {
        initTest()

        persistTyoskentelyjakso(tyoskentelyjakso)
        assertFalse(tyoskentelyjakso.liitettyKoejaksoon)

        val tyoskentelyjaksoDTO = TyoskentelyjaksoDTO(id = tyoskentelyjakso.id, liitettyKoejaksoon = true)

        testMockMvc.perform(patch("$API_TYOSKENTELYJAKSOT/koejakso").contentType(APPLICATION_JSON).content(convertObjectToJsonBytes(tyoskentelyjaksoDTO))
            .with(csrf())).andExpect(status().isOk).andExpect(content().contentType(APPLICATION_JSON_VALUE)).andExpect(jsonPath("$.liitettyKoejaksoon").value(true))
    }

    private fun populateTyoskentelyjaksoDto(existingTyoskentelyjakso: Tyoskentelyjakso, updatedTyosklypaikka: Tyoskentelypaikka, omaaErikoisala: Erikoisala): TyoskentelyjaksoDTO {
        val tyoskentelyjaksoDTO = tyoskentelyjaksoMapper.toDto(existingTyoskentelyjakso)
        tyoskentelyjaksoDTO.apply {
            tyoskentelypaikka?.nimi = updatedTyosklypaikka.nimi
            tyoskentelypaikka?.tyyppi = updatedTyosklypaikka.tyyppi
            tyoskentelypaikka?.muuTyyppi = updatedTyosklypaikka.muuTyyppi
            tyoskentelypaikka?.kunta = kuntaMapper.toDto(updatedTyosklypaikka.kunta!!)
            alkamispaiva = ErikoistuvaLaakariTyoskentelyjaksoHelper.UPDATED_ALKAMISPAIVA
            paattymispaiva = ErikoistuvaLaakariTyoskentelyjaksoHelper.UPDATED_PAATTYMISPAIVA
            osaaikaprosentti = ErikoistuvaLaakariTyoskentelyjaksoHelper.UPDATED_OSAAIKAPROSENTTI
            kaytannonKoulutus = ErikoistuvaLaakariTyoskentelyjaksoHelper.UPDATED_KAYTANNON_KOULUTUS
            this.omaaErikoisalaaTukeva = erikoisalaMapper.toDto(omaaErikoisala)
            omaaErikoisalaaTukevaId = omaaErikoisala.id
            hyvaksyttyAiempaanErikoisalaan = ErikoistuvaLaakariTyoskentelyjaksoHelper.UPDATED_HYVAKSYTTY_AIEMPAAN_ERIKOISALAAN
        }
        return tyoskentelyjaksoDTO
    }

    private fun assertUpdatedTyoskentelyjakso(testTyoskentelyjakso: Tyoskentelyjakso, updatedTyoskentelypaikka: Tyoskentelypaikka) {
        assertThat(testTyoskentelyjakso.tyoskentelypaikka?.nimi).isEqualTo(updatedTyoskentelypaikka.nimi)
        assertThat(testTyoskentelyjakso.tyoskentelypaikka?.tyyppi).isEqualTo(updatedTyoskentelypaikka.tyyppi)
        assertThat(testTyoskentelyjakso.tyoskentelypaikka?.muuTyyppi).isEqualTo(updatedTyoskentelypaikka.muuTyyppi)
        assertThat(testTyoskentelyjakso.tyoskentelypaikka?.kunta).isEqualTo(updatedTyoskentelypaikka.kunta)
        assertThat(testTyoskentelyjakso.alkamispaiva).isEqualTo(ErikoistuvaLaakariTyoskentelyjaksoHelper.UPDATED_ALKAMISPAIVA)
        assertThat(testTyoskentelyjakso.paattymispaiva).isEqualTo(ErikoistuvaLaakariTyoskentelyjaksoHelper.UPDATED_PAATTYMISPAIVA)
        assertThat(testTyoskentelyjakso.osaaikaprosentti).isEqualTo(ErikoistuvaLaakariTyoskentelyjaksoHelper.UPDATED_OSAAIKAPROSENTTI)
        assertThat(testTyoskentelyjakso.kaytannonKoulutus).isEqualTo(ErikoistuvaLaakariTyoskentelyjaksoHelper.UPDATED_KAYTANNON_KOULUTUS)
    }
}
