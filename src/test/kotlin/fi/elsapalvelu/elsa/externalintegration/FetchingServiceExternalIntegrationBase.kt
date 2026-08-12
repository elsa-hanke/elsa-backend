package fi.elsapalvelu.elsa.externalintegration

import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.integration.OpintotietodataFetchingService
import fi.elsapalvelu.elsa.service.integration.OpintosuorituksetFetchingService
import fi.elsapalvelu.elsa.service.dto.koulutus.OpintosuorituksetPersistenceDTO
import fi.elsapalvelu.elsa.service.dto.koulutus.OpintosuoritusDTO
import fi.elsapalvelu.elsa.service.dto.koulutus.OpintosuoritusOsakokonaisuusDTO
import fi.elsapalvelu.elsa.service.dto.koulutus.OpintotietoOpintooikeusDataDTO
import fi.elsapalvelu.elsa.service.dto.koulutus.OpintotietodataDTO
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment

data class StudyDataExternalIntegrationFixture(
    val hetu: String,
    val assertErikoisalaTunnisteList: Boolean,
    val expectedStudyRightId: String?,
    val expectedCourseCode: String?,
    val expectedProgrammeIdentifier: String?
)

abstract class FetchingServiceExternalIntegrationBase : ExternalIntegrationTestSupport() {

    @Autowired
    private lateinit var environment: Environment

    protected abstract val opintotietodataService: OpintotietodataFetchingService

    protected abstract val opintosuorituksetService: OpintosuorituksetFetchingService

    protected abstract val fixtureName: String

    protected abstract val expectedUniversity: YliopistoEnum

    private val fixture: StudyDataExternalIntegrationFixture
        get() {
            val prefix = "external-integration.study-data.$fixtureName"
            return StudyDataExternalIntegrationFixture(
                hetu = environment.getRequiredProperty("$prefix.hetu"),
                assertErikoisalaTunnisteList = environment.getProperty(
                    "$prefix.assert-erikoisala-tunniste-list",
                    Boolean::class.java,
                    true
                ),
                expectedStudyRightId = environment.getOptionalProperty("$prefix.expected-study-right-id"),
                expectedCourseCode = environment.getOptionalProperty("$prefix.expected-course-code"),
                expectedProgrammeIdentifier = environment.getOptionalProperty(
                    "$prefix.expected-programme-identifier"
                )
            )
        }

    @Test
    fun shouldFetchOpintotietodataWithoutErrors() {
        val yliopisto = opintotietodataService.getYliopisto()
        assertThat(yliopisto)
            .describedAs("$fixtureName service should identify the configured university")
            .isEqualTo(expectedUniversity)
        log.info("Testing fetchOpintotietodata for {} with configured test identity", yliopisto)

        val result = runBlocking { opintotietodataService.fetchOpintotietodata(fixture.hetu) }

        log.info(
            "fetchOpintotietodata result for {}: syntymaaika={}, opintooikeusCount={}",
            yliopisto, result?.syntymaaika, result?.opintooikeudet?.size
        )
        assertValidOpintotietodata(result, yliopisto)
    }

    protected open fun assertValidOpintotietodata(result: OpintotietodataDTO?, yliopisto: YliopistoEnum?) {
        assertThat(result)
            .describedAs(
                "fetchOpintotietodata returned null for $yliopisto – " +
                    "the server responded but returned no data for the test hetu. " +
                    "Check that the test hetu has active study rights in the target environment."
            )
            .isNotNull
        assertThat(result!!.opintooikeudet)
            .describedAs("fetchOpintotietodata should return at least one opintooikeus for test hetu")
            .isNotNull
            .isNotEmpty

        val opintooikeudet = result.opintooikeudet.orEmpty()
        opintooikeudet.forEach(::log)

        assertThat(opintooikeudet).allSatisfy {
            assertThat(it.id)
                .describedAs("id should be populated")
                .isNotBlank

            assertThat(it.opintooikeudenAlkamispaiva)
                .describedAs("start date should be populated")
                .isNotNull

            assertThat(it.opintooikeudenPaattymispaiva)
                .describedAs("end date should be populated")
                .isNotNull

            assertThat(it.asetus)
                .describedAs("asetus should be populated")
                .isNotBlank

            if (fixture.assertErikoisalaTunnisteList) {
                assertThat(it.erikoisalaTunnisteList)
                    .describedAs("erikoisalaTunnisteList should be populated")
                    .isNotNull
                    .isNotEmpty
            }

            assertThat(it.tila)
                .describedAs("tila should be populated")
                .isNotNull

            assertThat(it.yliopisto)
                .describedAs("yliopisto should match the provider")
                .isEqualTo(yliopisto)

            assertThat(it.opintooikeudenPaattymispaiva)
                .describedAs("end date should not be before start date")
                .isAfterOrEqualTo(it.opintooikeudenAlkamispaiva)
        }

        fixture.expectedStudyRightId?.let { expectedId ->
            assertThat(opintooikeudet.map { it.id })
                .describedAs("configured study-right sentinel should be present for $fixtureName")
                .contains(expectedId)
        }
        fixture.expectedProgrammeIdentifier?.let { expectedIdentifier ->
            assertThat(opintooikeudet.flatMap { it.erikoisalaTunnisteList.orEmpty() })
                .describedAs("configured programme sentinel should be present for $fixtureName")
                .contains(expectedIdentifier)
        }
    }

    private fun log(data: OpintotietoOpintooikeusDataDTO) {
        log.info(
            """
        id=${data.id},
        opintooikeudenAlkamispaiva=${data.opintooikeudenAlkamispaiva},
        opintooikeudenPaattymispaiva=${data.opintooikeudenPaattymispaiva},
        asetus=${data.asetus},
        erikoisalaTunnisteList=${data.erikoisalaTunnisteList},
        tila=${data.tila},
        yliopisto=${data.yliopisto},
        opiskelijatunnus=${maskIdentifier(data.opiskelijatunnus)}
        """.trimIndent()
        )
    }


    @Test
    fun shouldFetchOpintosuorituksetWithoutErrors() {
        val yliopisto = opintosuorituksetService.getYliopisto()
        assertThat(yliopisto)
            .describedAs("$fixtureName service should identify the configured university")
            .isEqualTo(expectedUniversity)
        log.info("Testing fetchOpintosuoritukset for {} with configured test identity", yliopisto)

        val result = runBlocking { opintosuorituksetService.fetchOpintosuoritukset(fixture.hetu) }

        log.info(
            "fetchOpintosuoritukset result for {}: yliopisto={}, itemCount={}",
            yliopisto, result?.yliopisto, result?.items?.size
        )

        assertValidOpintosuoritukset(result, yliopisto)
    }

    protected open fun assertValidOpintosuoritukset(
        result: OpintosuorituksetPersistenceDTO?,
        yliopisto: YliopistoEnum?
    ) {
        assertThat(result)
            .describedAs(
                "fetchOpintosuoritukset returned null for $yliopisto – " +
                    "the server responded but returned no attainment data for the test hetu."
            )
            .isNotNull

        assertThat(result!!.items)
            .describedAs("items must not be null or empty")
            .isNotNull
            .isNotEmpty

        assertThat(result.yliopisto)
            .describedAs("yliopisto should match service yliopisto")
            .isEqualTo(yliopisto)

        val items = result.items.orEmpty()
        items.forEach(::log)

        assertThat(items).allSatisfy { suoritus ->
            assertThat(suoritus.nimi_fi)
                .describedAs("nimi_fi should be populated")
                .isNotBlank

            assertThat(suoritus.kurssikoodi)
                .describedAs("kurssikoodi should be populated")
                .isNotBlank

            assertThat(suoritus.opintopisteet)
                .describedAs("opintopisteet should be populated")
                .isNotNull
                .isGreaterThanOrEqualTo(0.0)

            assertThat(suoritus.hyvaksytty)
                .describedAs("hyvaksytty should be populated")
                .isNotNull

            assertThat(suoritus.yliopistoOpintooikeusId)
                .describedAs("yliopistoOpintooikeusId should be populated")
                .isNotBlank

            suoritus.osakokonaisuudet.orEmpty().forEach { osakokonaisuus ->
                log(osakokonaisuus)
                assertThat(osakokonaisuus.nimi_fi)
                    .describedAs("osakokonaisuus.nimi_fi should be populated")
                    .isNotBlank

                assertThat(osakokonaisuus.kurssikoodi)
                    .describedAs("osakokonaisuus.kurssikoodi should be populated")
                    .isNotBlank

                assertThat(osakokonaisuus.opintopisteet)
                    .describedAs("osakokonaisuus.opintopisteet should be populated")
                    .isNotNull
                    .isGreaterThanOrEqualTo(0.0)

                assertThat(osakokonaisuus.hyvaksytty)
                    .describedAs("osakokonaisuus.hyvaksytty should be populated")
                    .isNotNull
            }
        }

        fixture.expectedCourseCode?.let { expectedCode ->
            val courseCodes = items.flatMap { suoritus ->
                listOfNotNull(suoritus.kurssikoodi) +
                    suoritus.osakokonaisuudet.orEmpty().mapNotNull { it.kurssikoodi }
            }
            assertThat(courseCodes)
                .describedAs("configured course-code sentinel should be present for $fixtureName")
                .contains(expectedCode)
        }
    }

    private fun log(data: OpintosuoritusDTO) {
        log.info(
            """
        id=${data.id},
        nimi_fi=${data.nimi_fi},
        nimi_sv=${data.nimi_sv},
        kurssikoodi=${data.kurssikoodi},
        tyyppi=${data.tyyppi?.nimi},
        suorituspaiva=${data.suorituspaiva},
        opintopisteet=${data.opintopisteet},
        hyvaksytty=${data.hyvaksytty},
        arvio_fi=${data.arvio_fi},
        arvio_sv=${data.arvio_sv},
        vanhenemispaiva=${data.vanhenemispaiva},
        yliopistoOpintooikeusId=${data.yliopistoOpintooikeusId},
        osakokonaisuudetCount=${data.osakokonaisuudet?.size}
        """.trimIndent()
        )
    }

    private fun log(data: OpintosuoritusOsakokonaisuusDTO) {
        log.info(
            """
        osakokonaisuus.id=${data.id},
        nimi_fi=${data.nimi_fi},
        nimi_sv=${data.nimi_sv},
        kurssikoodi=${data.kurssikoodi},
        suorituspaiva=${data.suorituspaiva},
        opintopisteet=${data.opintopisteet},
        hyvaksytty=${data.hyvaksytty},
        arvio_fi=${data.arvio_fi},
        arvio_sv=${data.arvio_sv},
        vanhenemispaiva=${data.vanhenemispaiva}
        """.trimIndent()
        )
    }

    private fun maskIdentifier(value: String?): String? =
        value?.let { "***${it.takeLast(MASKED_IDENTIFIER_VISIBLE_CHARACTERS)}" }

    private fun Environment.getOptionalProperty(name: String): String? =
        getProperty(name)?.takeIf { it.isNotBlank() }

    private companion object {
        const val MASKED_IDENTIFIER_VISIBLE_CHARACTERS = 4
    }
}
