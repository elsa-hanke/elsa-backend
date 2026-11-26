package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.elsapalvelu.elsa.domain.perustiedot.ErikoisalaTyyppi
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.metadata.DynastyMetadataMapper
import fi.elsapalvelu.elsa.service.arkistointi.job.payload.ArchivePayloadReader
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * Acceptance contract for Dynasty metadata. See resources/fixtures/arkistointi/README.md.
 * Change the golden file first, then the code; never regenerate a golden file just to make this test pass.
 */
internal class DynastyMetadataGoldenTest {
    private val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())

    @ParameterizedTest(name = "{0} {1} {2}")
    @MethodSource("scenarios")
    fun `plan matches the golden specification mapping`(profile: DynastyArchiveProfile, caseType: CaseType, programme: String) {
        val golden = "fixtures/arkistointi/dynasty/${profile.name.lowercase()}-${caseType.name.lowercase()}-${programme.lowercase()}.json"
        val expected = requireNotNull(javaClass.classLoader.getResourceAsStream(golden)) { "Missing golden file: $golden" }
            .use { it.readBytes().toString(Charsets.UTF_8) }
        JSONAssert.assertEquals(golden, expected, actual(profile, caseType, programme), JSONCompareMode.STRICT)
    }

    private fun actual(profile: DynastyArchiveProfile, caseType: CaseType, programme: String): String {
        val properties = dynastyTestProperties("https://dynasty.example", profile.university)
        val submission = dynastySubmission(
            mapper, caseType,
            specialtyType = if (programme == "EHL") ErikoisalaTyyppi.HAMMASLAAKETIEDE else ErikoisalaTyyppi.LAAKETIEDE,
            yek = programme == "YEK", university = profile.university
        )
        val settings = profile.archiveProperties(properties.getArkistointi()).dynasty
        val plan = DynastyMetadataMapper(profile, settings, mapper, ArchivePayloadReader(mapper), CLOCK).plan(submission)
        val tosTitles = profile.caseProfile(caseType).documentTypes
        return mapper.valueToTree<ObjectNode>(plan).apply {
            val titles = putArray("tosTitles")
            submission.asiakirjat.forEach { titles.add(tosTitles.getValue(it.asiakirjatyyppi)) }
        }.toString()
    }

    companion object {
        // Europe/Helsinki date is 2026-11-19, which is the secrecy period start.
        private val CLOCK: Clock = Clock.fixed(Instant.parse("2026-11-18T22:30:00Z"), ZoneOffset.UTC)

        @JvmStatic
        fun scenarios() = DynastyArchiveProfile.entries.flatMap { profile ->
            listOf(CaseType.VALMISTUMINEN, CaseType.KOEJAKSO).flatMap { caseType ->
                listOf("EL", "EHL", "YEK").map { Arguments.of(profile, caseType, it) }
            }
        }
    }
}
