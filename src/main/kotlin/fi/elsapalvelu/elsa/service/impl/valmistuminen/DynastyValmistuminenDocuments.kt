package fi.elsapalvelu.elsa.service.impl.valmistuminen

import fi.elsapalvelu.elsa.config.arkistointi.ArkistointiConfigurationProvider
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiAsiakirjaSnapshotService
import fi.elsapalvelu.elsa.required
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordProperties
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/** Supplements async graduation only; YEK requires the same certificate as EL/EHL. */
@Service
@Transactional(propagation = Propagation.MANDATORY)
class DynastyValmistuminenDocuments(
    private val configuration: ArkistointiConfigurationProvider,
    private val snapshots: ArkistointiAsiakirjaSnapshotService
) {
    fun complete(request: Valmistumispyynto, university: YliopistoEnum, generated: List<RecordProperties>): List<RecordProperties> {
        if (university !in setOf(YliopistoEnum.TURUN_YLIOPISTO, YliopistoEnum.ITA_SUOMEN_YLIOPISTO)) return generated
        val settings = configuration.getConfiguration(university, CaseType.VALMISTUMINEN)
        check(requiredTypes.all { settings.metadata.getDocumentMetadata(it, settings.case) != null }) {
            "Dynasty graduation document configuration is incomplete"
        }
        check(generated.map { it.type } == listOf(RecordType.YHTEENVETO, RecordType.LIITE)) {
            "Dynasty graduation document set is incomplete"
        }
        val studyRight = request.opintooikeus.required()
        val trainee = studyRight.erikoistuvaLaakari.required()
        val snapshot = snapshots.createSnapshot(
            studyRight, trainee.laillistamispaivanLiitetiedostonNimi, trainee.laillistamispaivanLiitetiedostonTyyppi,
            trainee.laillistamistodistus?.data, RecordType.LAILLISTAMISTODISTUS, generated.size
        )
        // The certificate has no source Asiakirja: use the snapshot's own ID, never an AsiakirjaData ID.
        // prepareDocuments/createReference recognizes this persisted snapshot and reuses it.
        return generated + RecordProperties(snapshot.asiakirja, RecordType.LAILLISTAMISTODISTUS)
    }

    private companion object {
        val requiredTypes = listOf(RecordType.YHTEENVETO, RecordType.LIITE, RecordType.LAILLISTAMISTODISTUS)
    }
}
