package fi.elsapalvelu.elsa.config

import fi.elsapalvelu.elsa.externalintegration.arkistointi.helsinki.HelsinkiArkistointiExternalIntegrationTestApplication
import fi.elsapalvelu.elsa.externalintegration.arkistointi.tampere.TampereArkistointiExternalIntegrationTestApplication
import fi.elsapalvelu.elsa.externalintegration.arkistointi.turku.TurkuArkistointiExternalIntegrationTestApplication
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyClient
import fi.elsapalvelu.elsa.service.arkistointi.sahke.SahkeMetadataBuilder
import java.time.Clock
import javax.sql.DataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class ArkistoinninUlkoistenTestienKontekstiTest {
    enum class Arkistointikonteksti(val asetukset: Class<*>, val kellonKayttaja: Class<*>) {
        HELSINKI(HelsinkiArkistointiExternalIntegrationTestApplication::class.java, SahkeMetadataBuilder::class.java),
        TAMPERE(TampereArkistointiExternalIntegrationTestApplication::class.java, SahkeMetadataBuilder::class.java),
        TURKU(TurkuArkistointiExternalIntegrationTestApplication::class.java, DynastyClient::class.java)
    }

    @ParameterizedTest
    @EnumSource(Arkistointikonteksti::class)
    fun `ulkoinen arkistointikonteksti kaynnistyy yhteisella kellolla ilman tietokantaa`(tapaus: Arkistointikonteksti) {
        // Käynnistetään oikeat testisovellukset paikallisilla asetuksilla kutsumatta lähetysmetodeja.
        ApplicationContextRunner()
            .withPropertyValues(
                "spring.profiles.active=external-integration",
                "application.arkistointi.tre.host=localhost",
                "application.arkistointi.tre.user=testi"
            )
            .withUserConfiguration(tapaus.asetukset)
            .run { konteksti ->
                assertThat(konteksti).hasNotFailed()
                assertThat(konteksti).hasSingleBean(Aikakonfiguraatio::class.java)
                assertThat(konteksti).hasSingleBean(Clock::class.java)
                assertThat(konteksti).hasSingleBean(tapaus.kellonKayttaja)
                assertThat(konteksti).doesNotHaveBean(DataSource::class.java)
            }
    }
}
