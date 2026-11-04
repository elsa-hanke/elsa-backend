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

/** Supplements the configured async graduation document set, independently of transport or university. */
@Service
@Transactional(propagation = Propagation.MANDATORY)
class ValmistuminenArkistointiDocuments(
    private val configuration: ArkistointiConfigurationProvider,
    private val snapshots: ArkistointiAsiakirjaSnapshotService
) {
    fun complete(request: Valmistumispyynto, university: YliopistoEnum, generated: List<RecordProperties>): List<RecordProperties> {
        val settings = configuration.getConfiguration(university, CaseType.VALMISTUMINEN)
        val includeCertificate = settings.metadata.getDocumentMetadata(RecordType.LAILLISTAMISTODISTUS, settings.case) != null
        val available = generated.map { it.type } + if (includeCertificate) listOf(RecordType.LAILLISTAMISTODISTUS) else emptyList()
        check(settings.case.documents.orEmpty().keys.all { key -> available.any { it.name.lowercase() == key } }) {
            "Configured graduation document is missing"
        }
        if (!includeCertificate) return generated
        check(generated.none { it.type == RecordType.LAILLISTAMISTODISTUS }) {
            "Graduation certificate must be added only once"
        }
        val studyRight = request.opintooikeus.required()
        val trainee = studyRight.erikoistuvaLaakari.required()
        val snapshot = snapshots.createSnapshot(
            studyRight, trainee.laillistamispaivanLiitetiedostonNimi, trainee.laillistamispaivanLiitetiedostonTyyppi,
            trainee.laillistamistodistus?.data, RecordType.LAILLISTAMISTODISTUS, generated.size
        )
        // The certificate has no source Asiakirja: use the snapshot's own ID, never an AsiakirjaData ID.
        // The enqueuer's createReference recognizes this persisted snapshot and reuses it.
        return generated + RecordProperties(snapshot.asiakirja, RecordType.LAILLISTAMISTODISTUS)
    }

}
