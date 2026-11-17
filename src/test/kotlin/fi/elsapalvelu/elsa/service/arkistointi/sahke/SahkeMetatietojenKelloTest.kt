package fi.elsapalvelu.elsa.service.arkistointi.sahke

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.config.arkistointi.ArkistointiConfiguration
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.service.helpers.KellonTestitapaus
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class SahkeMetatietojenKelloTest {
    @ParameterizedTest
    @MethodSource("tapaukset")
    fun `metatiedon luontipaiva tulee kellosta ellei se ole annettu aineistossa`(tapaus: Luontitapaus) {
        val asetukset = ArkistointiConfiguration(ApplicationProperties.Arkistointi.Metadata(), ApplicationProperties.Arkistointi.Case())
        val pyynto = SahkeMetadataRequest(Opintooikeus(id = 11L), emptyList(), "1", null, null, null, null,
            created = tapaus.annettuPaiva)
        val tulos = SahkeMetadataBuilder(tapaus.aika.kello()).build(pyynto, asetukset)
        val odotettu = tapaus.annettuPaiva ?: tapaus.aika.paiva

        assertThat(tulos.metadata.caseFile.created).isEqualTo(odotettu)
        assertThat(tulos.metadata.transferInformation.nativeId).contains("${odotettu.year}.${odotettu.year}")
    }

    data class Luontitapaus(val aika: KellonTestitapaus, val annettuPaiva: LocalDate?)
    companion object {
        @JvmStatic
        fun tapaukset(): List<Luontitapaus> = KellonTestitapaus.tapaukset().flatMap { aika ->
            listOf(null, LocalDate.of(2018, 1, 1)).map { Luontitapaus(aika, it) }
        }
    }
}
