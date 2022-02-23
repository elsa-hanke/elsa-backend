package fi.elsapalvelu.elsa.web.rest.common

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.ApplicationSetting
import fi.elsapalvelu.elsa.domain.enumeration.ApplicationSettingTyyppi
import fi.elsapalvelu.elsa.repository.ApplicationSettingRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.MockitoAnnotations
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@AutoConfigureMockMvc
@SpringBootTest(classes = [ElsaBackendApp::class])
class JulkisetToiminnotResourceIT {

    @Autowired
    private lateinit var applicationSettingRepository: ApplicationSettingRepository

    @Autowired
    private lateinit var restMockMvc: MockMvc

    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    @Transactional
    fun testSeuraavaPaivitysNotExist() {
        restMockMvc.perform(get("/api/julkinen/seuraava-paivitys"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").doesNotExist())
    }

    @Test
    @Transactional
    fun testSeuraavaPaivitysInpast() {
        applicationSettingRepository.saveAndFlush(
            ApplicationSetting(
                id = 1,
                settingName = ApplicationSettingTyyppi.SEURAAVAN_PAIVITYKSEN_AIKA,
                datetimeSetting = Instant.now().minusSeconds(50000)
            )
        )

        restMockMvc.perform(get("/api/julkinen/seuraava-paivitys"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").doesNotExist())
    }

    @Test
    @Transactional
    fun testSeuraavaPaivitysTooFar() {
        applicationSettingRepository.saveAndFlush(
            ApplicationSetting(
                id = 1,
                settingName = ApplicationSettingTyyppi.SEURAAVAN_PAIVITYKSEN_AIKA,
                datetimeSetting = Instant.now().plusSeconds(260000)
            )
        )

        restMockMvc.perform(get("/api/julkinen/seuraava-paivitys"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").doesNotExist())
    }

    @Test
    @Transactional
    fun testSeuraavaPaivitys() {
        val paivitysAika = Instant.now().plusSeconds(160000)
        applicationSettingRepository.saveAndFlush(
            ApplicationSetting(
                id = 1,
                settingName = ApplicationSettingTyyppi.SEURAAVAN_PAIVITYKSEN_AIKA,
                datetimeSetting = paivitysAika
            )
        )

        restMockMvc.perform(get("/api/julkinen/seuraava-paivitys"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").value(paivitysAika.toString()))
    }
}
