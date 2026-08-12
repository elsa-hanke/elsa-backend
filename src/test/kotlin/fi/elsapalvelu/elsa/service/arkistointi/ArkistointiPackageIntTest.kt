package fi.elsapalvelu.elsa.service.arkistointi

import fi.elsapalvelu.elsa.service.arkistointi.sahke.SahkeMetadataBuilder
import fi.elsapalvelu.elsa.service.arkistointi.sahke.SahkePakettiBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito
import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Stream
import java.util.zip.ZipFile

class ArkistointiPackageIntTest {

    private val service = ArkistointiServiceImpl(
        configurationProvider = ArkistointiConfigurationProvider(
            ArkistointiTestData.createApplicationProperties()
        ),
        metadataBuilder = SahkeMetadataBuilder(),
        pakettiBuilder = SahkePakettiBuilder(),
        dispatcher = Mockito.mock(ArkistointiDispatcher::class.java)
    )

    @ParameterizedTest(name = "{0}")
    @MethodSource("packageScenarios")
    fun `creates SAHKE package for every configured archive case`(scenario: ArkistointiTestScenario) {
        val input = ArkistointiTestData.createInput(scenario)
        val result = ArkistointiTestData.createPackage(service, input)
        val zipPath = Paths.get(result.zipFilePath)

        try {
            assertThat(zipPath).exists()
            assertThat(Files.size(zipPath)).isGreaterThan(0)
            ZipFile(zipPath.toFile()).use { zip ->
                val entries = zip.entries().asSequence().map { it.name }.toList()
                assertThat(entries).contains("sahke.xml")
                assertThat(entries).containsExactlyInAnyOrderElementsOf(
                    listOf("sahke.xml") + input.asiakirjat.map { "pdf/${it.asiakirja.nimi}" }
                )
                val metadata = zip.getInputStream(zip.getEntry("sahke.xml")).bufferedReader().use { it.readText() }
                assertThat(metadata)
                    .describedAs("SÄHKE metadata should contain the traceable test case ID")
                    .contains(input.caseId)
            }
        } finally {
            Files.deleteIfExists(zipPath)
        }
    }

    companion object {
        @JvmStatic
        fun packageScenarios(): Stream<ArkistointiTestScenario> =
            (ArkistointiTestData.helsinkiScenarios + ArkistointiTestData.tamperePackageScenarios)
                .stream()
    }
}
