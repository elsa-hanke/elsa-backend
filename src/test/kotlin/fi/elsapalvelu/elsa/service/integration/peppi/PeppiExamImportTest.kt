package fi.elsapalvelu.elsa.service.integration.peppi

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.dto.koulutus.OpintosuorituksetPersistenceDTO
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PeppiExamImportTest {
    private val objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())

    @Test
    fun `incomplete passed subpart does not become an authoritative deletion snapshot`() {
        val result = fetch(listOf(accomplishment("main", MAIN), accomplishment("essay", ESSAY, date = null)))
        assertThat(result.items).hasSize(1)
        assertThat(result.items!!.single().suorituspaiva).isNull()
        assertThat(result.items!!.single().osakokonaisuudet).isNull()
    }

    @Test
    fun `passed subpart never replaces a failed main exam`() {
        val result = fetch(listOf(accomplishment("main", MAIN, passed = false), accomplishment("essay", ESSAY)))
        val exam = result.items!!.single()
        assertThat(exam.nimi_fi).isEqualTo(MAIN)
        assertThat(exam.hyvaksytty).isFalse
        assertThat(exam.osakokonaisuudet!!.single().hyvaksytty).isTrue
        assertThat(result.replaceOsakokonaisuudet).isTrue
    }

    @Test
    fun `missing or undated main exam never promotes a passed subpart`() {
        val essay = accomplishment("essay", ESSAY)
        assertThat(fetch(listOf(essay)).items).isEmpty()
        val result = fetch(listOf(accomplishment("main", MAIN, passed = false, date = null), essay))
        assertThat(result.items).hasSize(1)
        assertThat(result.items!!.single().suorituspaiva).isNull()
        assertThat(result.items!!.single().nimi_fi).isEqualTo(MAIN)
    }

    @Test
    fun `unrecognized main title cannot certify an exam`() {
        val result = fetch(listOf(accomplishment("other", "Valtakunnallinen osasuoritus")))
        assertThat(result.items).isEmpty()
    }

    @Test
    fun `main-only result carries an empty authoritative child list`() {
        val exam = fetch(listOf(accomplishment("main", MAIN))).items!!.single()
        assertThat(exam.osakokonaisuudet).isNotNull().isEmpty()
    }

    @Test
    fun `all supported university exam codes group subparts`() {
        val codes = listOf(
            YliopistoEnum.TURUN_YLIOPISTO to "ELOP0001",
            YliopistoEnum.TURUN_YLIOPISTO to "EHLO0001",
            YliopistoEnum.ITA_SUOMEN_YLIOPISTO to "4415200",
            YliopistoEnum.ITA_SUOMEN_YLIOPISTO to "4415100"
        )
        codes.forEach { (university, code) ->
            val title = if (code in setOf("EHLO0001", "4415100")) DENTAL_MAIN else MAIN
            val exam = fetch(
                listOf(accomplishment("main", title, code = code), accomplishment("essay", ESSAY, code = code)),
                university
            ).items!!.single()
            assertThat(exam.nimi_fi).isEqualTo(title)
            assertThat(exam.osakokonaisuudet!!.single().kurssikoodi).isEqualTo("essay")
        }
    }

    @Test
    fun `same course in different study entitlements is never merged`() {
        val result = fetch(
            listOf(accomplishment("main", MAIN), accomplishment("other-main", MAIN, entitlement = "other"))
        )
        assertThat(result.items).hasSize(2)
        assertThat(result.items!!.map { it.yliopistoOpintooikeusId }).containsExactlyInAnyOrder("key", "other")
    }

    @Test
    fun `main exam retakes are not imported as subparts and response order is irrelevant`() {
        val payload = listOf(
            accomplishment("main-1", MAIN, grade = "1"),
            accomplishment("main-2", MAIN, grade = "2"),
            accomplishment("failed-main", MAIN, passed = false, date = "2026-06-01T00:00:00"),
            accomplishment("essay", ESSAY)
        )
        val first = fetch(payload).items!!.single()
        val reversed = fetch(payload.reversed()).items!!.single()
        assertThat(first.arvio_fi).isEqualTo("2")
        assertThat(first.hyvaksytty).isTrue
        assertThat(first.osakokonaisuudet!!.map { it.kurssikoodi }).containsExactly("essay")
        assertThat(objectMapper.writeValueAsString(reversed)).isEqualTo(objectMapper.writeValueAsString(first))
    }

    @Test
    fun `missing IDs remain deterministic and long common name prefixes do not collide`() {
        val prefix = "Patologia, " + "a".repeat(60)
        val payload = listOf(
            accomplishment(null, MAIN, grade = "1"),
            accomplishment(null, MAIN, grade = "2"),
            accomplishment(null, prefix + "esseet"),
            accomplishment(null, prefix + "preparaatit"),
            accomplishment("long-id-" + "x".repeat(80), "Patologia, muu")
        )
        val first = fetch(payload).items!!.single()
        val reversed = fetch(payload.reversed()).items!!.single()
        val keys = first.osakokonaisuudet!!.map { it.kurssikoodi!! }
        assertThat(keys).hasSize(3).doesNotHaveDuplicates()
        assertThat(keys).allSatisfy { assertThat(it.length).isLessThanOrEqualTo(50) }
        assertThat(objectMapper.writeValueAsString(reversed)).isEqualTo(objectMapper.writeValueAsString(first))
    }

    @Test
    fun `duplicate source ID produces one child`() {
        val essay = accomplishment("essay", ESSAY)
        val exam = fetch(listOf(accomplishment("main", MAIN), essay, essay)).items!!.single()
        assertThat(exam.osakokonaisuudet).hasSize(1)
    }

    private fun fetch(
        payload: List<Map<String, Any?>>,
        university: YliopistoEnum = YliopistoEnum.TURUN_YLIOPISTO
    ): OpintosuorituksetPersistenceDTO {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setBody(objectMapper.writeValueAsString(payload)))
            return runBlocking {
                PeppiCommonOpintosuorituksetFetchingServiceImpl(objectMapper).fetchOpintosuoritukset(
                    server.url("/attainments").toString(), OkHttpClient(), "010190-1234", university
                )!!
            }
        } finally {
            server.shutdown()
        }
    }

    @Suppress("LongParameterList")
    private fun accomplishment(
        id: String?,
        name: String,
        passed: Boolean = true,
        date: String? = "2026-05-07T00:00:00",
        code: String = "ELOP0001",
        entitlement: String = "key",
        grade: String = if (passed) "Hyväksytty" else "Hylätty"
    ): Map<String, Any?> = mapOf(
        "id" to id,
        "studyEntitlementId" to "123",
        "studyEntitlementKey" to entitlement,
        "kurssiKoodi" to code,
        "suoritusPvm" to date,
        "opintopisteet" to 0.0,
        "nimi" to mapOf("fi" to name, "sv" to ""),
        "hyvaksytty" to passed,
        "arvio" to mapOf("fi" to grade, "sv" to "")
    )

    private companion object {
        const val MAIN = "VALTAKUNNALLINEN ERIKOISLÄÄKÄRIKUULUSTELU"
        const val DENTAL_MAIN = "VALTAKUNNALLINEN ERIKOISHAMMASLÄÄKÄRIKUULUSTELU"
        const val ESSAY = "Patologia, esseet"
    }
}
