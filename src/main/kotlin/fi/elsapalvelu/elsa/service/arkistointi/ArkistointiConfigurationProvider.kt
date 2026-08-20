package fi.elsapalvelu.elsa.service.arkistointi

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import org.springframework.stereotype.Component

data class ArkistointiConfiguration(
    val metadata: ApplicationProperties.Arkistointi.Metadata,
    val case: ApplicationProperties.Arkistointi.Case
)

@Component
class ArkistointiConfigurationProvider(
    private val applicationProperties: ApplicationProperties
) {
    fun getConfiguration(
        yliopisto: YliopistoEnum,
        caseType: CaseType
    ): ArkistointiConfiguration {
        val metadata = getMetadata(yliopisto)
        val case = metadata?.getCaseMetadata(caseType)
            ?: throw IllegalArgumentException(
                "Arkistointia ${caseType.value} ei ole määritelty yliopistolle ${yliopisto.name}"
            )

        return ArkistointiConfiguration(metadata, case)
    }

    fun onKaytossa(yliopisto: YliopistoEnum, caseType: CaseType): Boolean {
        if (!isEnabled(yliopisto)) {
            return false
        }

        return getMetadata(yliopisto)?.getCaseMetadata(caseType) != null
    }

    fun getTampereHost(): String? = applicationProperties.getArkistointi().getTre().host

    private fun getMetadata(yliopisto: YliopistoEnum): ApplicationProperties.Arkistointi.Metadata? {
        val arkistointi = applicationProperties.getArkistointi()
        return when (yliopisto) {
            YliopistoEnum.TAMPEREEN_YLIOPISTO -> arkistointi.getTre().metadata
            YliopistoEnum.HELSINGIN_YLIOPISTO -> arkistointi.getHki().metadata
            YliopistoEnum.OULUN_YLIOPISTO -> arkistointi.getOulu().metadata
            YliopistoEnum.TURUN_YLIOPISTO -> arkistointi.getTurku().metadata
            YliopistoEnum.ITA_SUOMEN_YLIOPISTO -> arkistointi.getUef().metadata
        }
    }

    private fun isEnabled(yliopisto: YliopistoEnum): Boolean {
        val arkistointi = applicationProperties.getArkistointi()
        return when (yliopisto) {
            YliopistoEnum.OULUN_YLIOPISTO -> arkistointi.getOulu().kaytossa
            YliopistoEnum.HELSINGIN_YLIOPISTO -> arkistointi.getHki().kaytossa
            YliopistoEnum.TAMPEREEN_YLIOPISTO -> arkistointi.getTre().kaytossa
            YliopistoEnum.TURUN_YLIOPISTO -> arkistointi.getTurku().kaytossa
            YliopistoEnum.ITA_SUOMEN_YLIOPISTO -> arkistointi.getUef().kaytossa
        }
    }
}
