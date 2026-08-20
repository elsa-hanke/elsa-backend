package fi.elsapalvelu.elsa.service.arkistointi

import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.dto.arkistointi.ArkistointiResult
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordProperties
import fi.elsapalvelu.elsa.service.arkistointi.sahke.SahkeMetadataBuilder
import fi.elsapalvelu.elsa.service.arkistointi.sahke.SahkeMetadataRequest
import fi.elsapalvelu.elsa.service.arkistointi.sahke.SahkePakettiBuilder
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class ArkistointiServiceImpl(
    private val configurationProvider: ArkistointiConfigurationProvider,
    private val metadataBuilder: SahkeMetadataBuilder,
    private val pakettiBuilder: SahkePakettiBuilder,
    private val dispatcher: ArkistointiDispatcher
) : ArkistointiService {
    override fun muodostaSahke(
        opintooikeus: Opintooikeus?,
        asiakirjat: List<RecordProperties>,
        caseId: String?,
        tarkastaja: String?,
        tarkastusPaiva: LocalDate?,
        hyvaksyja: String?,
        hyvaksymisPaiva: LocalDate?,
        yliopisto: YliopistoEnum?,
        caseType: CaseType
    ): ArkistointiResult {
        val yliopistoValue = requireNotNull(yliopisto)
        val configuration = configurationProvider.getConfiguration(yliopistoValue, caseType)
        val metadataResult = metadataBuilder.build(
            SahkeMetadataRequest(
                opintooikeus = opintooikeus,
                asiakirjat = asiakirjat,
                caseId = caseId,
                tarkastaja = tarkastaja,
                tarkastusPaiva = tarkastusPaiva,
                hyvaksyja = hyvaksyja,
                hyvaksymisPaiva = hyvaksymisPaiva
            ),
            configuration
        )

        return pakettiBuilder.build(
            metadataResult = metadataResult,
            opintooikeus = opintooikeus,
            zipMetadata = configuration.metadata.zipMetadata
        )
    }

    override fun laheta(
        yliopisto: YliopistoEnum,
        filePath: String,
        caseType: CaseType,
        yek: Boolean,
        caseId: String?,
        erikoistujanNimi: String?
    ) {
        dispatcher.laheta(
            ArkistointiDeliveryRequest(
                yliopisto = yliopisto,
                filePath = filePath,
                caseType = caseType,
                yek = yek,
                caseId = caseId,
                erikoistujanNimi = erikoistujanNimi
            )
        )
    }

    override fun onKaytossa(yliopisto: YliopistoEnum, caseType: CaseType): Boolean =
        configurationProvider.onKaytossa(yliopisto, caseType)
}
