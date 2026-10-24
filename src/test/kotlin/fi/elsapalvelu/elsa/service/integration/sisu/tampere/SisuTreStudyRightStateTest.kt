package fi.elsapalvelu.elsa.service.integration.sisu.tampere

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.config.ERIKOISTUVA_HAMMASLAAKARI_SISU_KOULUTUS
import fi.elsapalvelu.elsa.config.ERIKOISTUVA_LAAKARI_SISU_KOULUTUS
import fi.elsapalvelu.elsa.config.YEK_KOULUTETTAVA_SISU_TRE_KOULUTUS
import fi.elsapalvelu.elsa.domain.kayttaja.OpintooikeudenTila
import fi.elsapalvelu.elsa.repository.perustiedot.YliopistoRepository
import fi.elsapalvelu.elsa.service.dto.enumeration.SisuOpintooikeudenTila
import fi.elsapalvelu.elsa.service.integration.IntegrationAlertService
import fi.elsapalvelu.elsa.service.integration.OkHttpClientBuilder
import fi.elsapalvelu.elsa.service.kayttaja.AlertPublisherService
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.Mockito
import org.mockito.kotlin.verifyNoInteractions

class SisuTreStudyRightStateTest {

    @ParameterizedTest
    @EnumSource(SisuOpintooikeudenTila::class, names = ["TERMINATED", "PASSIVATED"])
    fun `new Sisu states deserialize without preventing valid study rights from being imported`(
        state: SisuOpintooikeudenTila
    ) {
        MockWebServer().use { server ->
            server.start()
            val mapper = jacksonObjectMapper()
            val rights = listOf(
                mapOf("id" to "unrelated", "state" to state.name),
                mapOf(
                    "id" to "el",
                    "state" to state.name,
                    "phase1EducationClassificationUrn" to ERIKOISTUVA_LAAKARI_SISU_KOULUTUS
                ),
                mapOf(
                    "id" to "ehl",
                    "state" to state.name,
                    "phase1EducationClassificationUrn" to ERIKOISTUVA_HAMMASLAAKARI_SISU_KOULUTUS
                ),
                mapOf(
                    "id" to "yek",
                    "state" to state.name,
                    "specialisation" to YEK_KOULUTETTAVA_SISU_TRE_KOULUTUS
                ),
                mapOf(
                    "id" to "active",
                    "state" to "ACTIVE",
                    "phase1EducationClassificationUrn" to ERIKOISTUVA_LAAKARI_SISU_KOULUTUS
                )
            )
            server.enqueue(
                MockResponse().setBody(mapper.writeValueAsString(mapOf("studyrights" to rights)))
            )
            val properties = ApplicationProperties().apply {
                getSecurity().getSisuTre().endpointUrl = server.url("/elsa").toString().removeSuffix("/")
            }
            val alertPublisher = Mockito.mock(AlertPublisherService::class.java)
            val service = SisuTreOpintotietodataFetchingServiceImpl(
                object : OkHttpClientBuilder {
                    override fun okHttpClient() = OkHttpClient()
                },
                properties,
                mapper,
                IntegrationAlertService(alertPublisher),
                Mockito.mock(YliopistoRepository::class.java)
            )

            val result = runBlocking { service.fetchOpintotietodata("010190-1234") }
            val importedRights = requireNotNull(result?.opintooikeudet)

            assertThat(importedRights.map { it.id }).containsExactly("el", "ehl", "yek", "active")
            importedRights.take(3).forEach { assertThat(it.tila).isNull() }
            assertThat(importedRights.last().tila).isEqualTo(OpintooikeudenTila.AKTIIVINEN)
            verifyNoInteractions(alertPublisher)
        }
    }
}
