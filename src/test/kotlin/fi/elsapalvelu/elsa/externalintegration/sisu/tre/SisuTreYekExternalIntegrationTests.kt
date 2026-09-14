package fi.elsapalvelu.elsa.externalintegration.sisu.tre

import fi.elsapalvelu.elsa.config.YEK_KOULUTETTAVA_SISU_TRE_KOULUTUS
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.externalintegration.FetchingServiceExternalIntegrationBase
import fi.elsapalvelu.elsa.service.dto.koulutus.OpintosuorituksetPersistenceDTO
import fi.elsapalvelu.elsa.service.dto.koulutus.OpintotietodataDTO
import fi.elsapalvelu.elsa.service.integration.OpintosuorituksetFetchingService
import fi.elsapalvelu.elsa.service.integration.OpintotietodataFetchingService
import fi.elsapalvelu.elsa.service.integration.sisu.tampere.SisuTreOpintosuorituksetFetchingServiceImpl
import fi.elsapalvelu.elsa.service.integration.sisu.tampere.SisuTreOpintotietodataFetchingServiceImpl
import org.assertj.core.api.Assertions.assertThat
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

/**
 * External integration tests for Sisu TRE YEK student identity (081159-999F).
 */
@SpringBootTest(classes = [SisuTreExternalIntegrationTestApplication::class])
@ActiveProfiles("external-integration")
class SisuTreYekExternalIntegrationTests : FetchingServiceExternalIntegrationBase() {

    @Autowired
    private lateinit var sisuTreOpintotietodataFetchingServiceImpl: SisuTreOpintotietodataFetchingServiceImpl

    @Autowired
    private lateinit var sisuTreOpintosuorituksetFetchingServiceImpl: SisuTreOpintosuorituksetFetchingServiceImpl

    override val opintotietodataService: OpintotietodataFetchingService
        get() = sisuTreOpintotietodataFetchingServiceImpl

    override val opintosuorituksetService: OpintosuorituksetFetchingService
        get() = sisuTreOpintosuorituksetFetchingServiceImpl

    override val fixtureName = "sisu-tre-yek"

    override val expectedUniversity = YliopistoEnum.TAMPEREEN_YLIOPISTO

    override fun assertValidOpintotietodata(result: OpintotietodataDTO?, yliopisto: YliopistoEnum?) {
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
        opintooikeudet.forEach {
            log.info(
                """
                id=${it.id},
                opintooikeudenAlkamispaiva=${it.opintooikeudenAlkamispaiva},
                opintooikeudenPaattymispaiva=${it.opintooikeudenPaattymispaiva},
                asetus=${it.asetus},
                erikoisalaTunnisteList=${it.erikoisalaTunnisteList},
                tila=${it.tila},
                yliopisto=${it.yliopisto},
                opiskelijatunnus=${it.opiskelijatunnus}
                """.trimIndent()
            )
        }

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

            assertThat(it.yliopisto)
                .describedAs("yliopisto should match the provider")
                .isEqualTo(yliopisto)

            assertThat(it.opintooikeudenPaattymispaiva)
                .describedAs("end date should not be before start date")
                .isAfterOrEqualTo(it.opintooikeudenAlkamispaiva)
        }

        val yekStudyRights = opintooikeudet.filter {
            it.erikoisalaTunnisteList?.contains(YEK_KOULUTETTAVA_SISU_TRE_KOULUTUS) == true
        }

        assertThat(yekStudyRights)
            .describedAs(
                "at least one study right must have specialisation '$YEK_KOULUTETTAVA_SISU_TRE_KOULUTUS' " +
                    "(erikoisalaTunnisteList containing '$YEK_KOULUTETTAVA_SISU_TRE_KOULUTUS')"
            )
            .isNotEmpty
    }

    override fun assertValidOpintosuoritukset(
        result: OpintosuorituksetPersistenceDTO?,
        yliopisto: YliopistoEnum?
    ) {
        assertThat(result)
            .describedAs("fetchOpintosuoritukset returned null for $yliopisto")
            .isNotNull

        assertThat(result!!.items)
            .describedAs("items must not be null")
            .isNotNull

        assertThat(result.yliopisto)
            .describedAs("yliopisto should match service yliopisto")
            .isEqualTo(yliopisto)
    }
}
