package fi.elsapalvelu.elsa.externalintegration

import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.service.OpintotietodataFetchingService
import fi.elsapalvelu.elsa.service.OpintosuorituksetFetchingService
import fi.elsapalvelu.elsa.service.dto.OpintosuorituksetPersistenceDTO
import fi.elsapalvelu.elsa.service.dto.OpintosuoritusDTO
import fi.elsapalvelu.elsa.service.dto.OpintosuoritusOsakokonaisuusDTO
import fi.elsapalvelu.elsa.service.dto.OpintotietoOpintooikeusDataDTO
import fi.elsapalvelu.elsa.service.dto.OpintotietodataDTO
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

abstract class FetchingServiceExternalIntegrationBase : ExternalIntegrationTestSupport() {

    protected abstract val opintotietodataService: OpintotietodataFetchingService

    protected abstract val opintosuorituksetService: OpintosuorituksetFetchingService

    protected open fun getTestHetu() = "210281-9988"

    @Test
    fun shouldFetchOpintotietodataWithoutErrors() {
        val yliopisto = opintotietodataService.getYliopisto()
        log.info("Testing fetchOpintotietodata for {}, for hetu {}", yliopisto, getTestHetu())

        val result = runBlocking { opintotietodataService.fetchOpintotietodata(getTestHetu()) }

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

        result.opintooikeudet?.forEach { oikeus ->
            log(oikeus)

            assertThat(result.opintooikeudet)
                .describedAs("opintooikeudet must not be empty")
                .isNotNull
                .isNotEmpty

            assertThat(result.opintooikeudet)
                .allSatisfy {

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

                    assertThat(it.tila)
                        .describedAs("tila should be populated")
                        .isNotNull

                    assertThat(it.yliopisto)
                        .describedAs("yliopisto should be populated")
                        .isEqualTo(yliopisto)

                    assertThat(it.opintooikeudenPaattymispaiva)
                        .describedAs("end date should not be before start date")
                        .isAfterOrEqualTo(it.opintooikeudenAlkamispaiva)
                }
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
        opiskelijatunnus=${data.opiskelijatunnus}
        """.trimIndent()
        )
    }


    @Test
    fun shouldFetchOpintosuorituksetWithoutErrors() {
        val yliopisto = opintosuorituksetService.getYliopisto()
        log.info("Testing fetchOpintosuoritukset for {}, for hetu {}", yliopisto, getTestHetu())

        val result = runBlocking { opintosuorituksetService.fetchOpintosuoritukset(getTestHetu()) }

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

        result.items?.forEach { suoritus ->

            log(suoritus)

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

            suoritus.osakokonaisuudet?.forEach { osakokonaisuus ->

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
}
