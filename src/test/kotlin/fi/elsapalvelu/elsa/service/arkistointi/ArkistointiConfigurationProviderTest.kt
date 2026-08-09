package fi.elsapalvelu.elsa.service.arkistointi

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class ArkistointiConfigurationProviderTest {
    private val applicationProperties = ApplicationProperties()
    private val provider = ArkistointiConfigurationProvider(applicationProperties)

    @Test
    fun `getConfiguration resolves metadata for every university`() {
        YliopistoEnum.entries.forEach { yliopisto ->
            val metadata = createMetadata(yliopisto.name)
            configure(yliopisto, enabled = true, metadata = metadata)

            val configuration = provider.getConfiguration(yliopisto, CaseType.VALMISTUMINEN)

            assertThat(configuration.metadata).isSameAs(metadata)
            assertThat(configuration.case).isSameAs(
                metadata.getCaseMetadata(CaseType.VALMISTUMINEN)
            )
            assertThat(provider.onKaytossa(yliopisto, CaseType.VALMISTUMINEN)).isTrue()
        }
    }

    @Test
    fun `onKaytossa requires both university enablement and case metadata`() {
        configure(
            YliopistoEnum.HELSINGIN_YLIOPISTO,
            enabled = false,
            metadata = createMetadata("Helsinki")
        )
        configure(
            YliopistoEnum.TAMPEREEN_YLIOPISTO,
            enabled = true,
            metadata = ApplicationProperties.Arkistointi.Metadata()
        )

        assertThat(
            provider.onKaytossa(YliopistoEnum.HELSINGIN_YLIOPISTO, CaseType.VALMISTUMINEN)
        ).isFalse()
        assertThat(
            provider.onKaytossa(YliopistoEnum.TAMPEREEN_YLIOPISTO, CaseType.VALMISTUMINEN)
        ).isFalse()
    }

    @Test
    fun `getConfiguration reports missing case metadata with university context`() {
        configure(
            YliopistoEnum.TURUN_YLIOPISTO,
            enabled = true,
            metadata = ApplicationProperties.Arkistointi.Metadata()
        )

        assertThatThrownBy {
            provider.getConfiguration(YliopistoEnum.TURUN_YLIOPISTO, CaseType.KOEJAKSO)
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining(CaseType.KOEJAKSO.value)
            .hasMessageContaining(YliopistoEnum.TURUN_YLIOPISTO.name)
    }

    private fun createMetadata(organisation: String): ApplicationProperties.Arkistointi.Metadata {
        val case = ApplicationProperties.Arkistointi.Case().apply {
            title = "Valmistuminen"
        }
        return ApplicationProperties.Arkistointi.Metadata().apply {
            this.organisation = organisation
            cases = mapOf(CaseType.VALMISTUMINEN.value to case)
        }
    }

    private fun configure(
        yliopisto: YliopistoEnum,
        enabled: Boolean,
        metadata: ApplicationProperties.Arkistointi.Metadata
    ) {
        val arkistointi = applicationProperties.getArkistointi()
        when (yliopisto) {
            YliopistoEnum.OULUN_YLIOPISTO -> arkistointi.getOulu().apply {
                kaytossa = enabled
                this.metadata = metadata
            }

            YliopistoEnum.HELSINGIN_YLIOPISTO -> arkistointi.getHki().apply {
                kaytossa = enabled
                this.metadata = metadata
            }

            YliopistoEnum.TAMPEREEN_YLIOPISTO -> arkistointi.getTre().apply {
                kaytossa = enabled
                this.metadata = metadata
            }

            YliopistoEnum.TURUN_YLIOPISTO -> arkistointi.getTurku().apply {
                kaytossa = enabled
                this.metadata = metadata
            }

            YliopistoEnum.ITA_SUOMEN_YLIOPISTO -> arkistointi.getUef().apply {
                kaytossa = enabled
                this.metadata = metadata
            }
        }
    }
}
