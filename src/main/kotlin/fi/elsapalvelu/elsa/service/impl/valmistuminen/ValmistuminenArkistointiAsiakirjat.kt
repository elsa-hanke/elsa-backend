package fi.elsapalvelu.elsa.service.impl.valmistuminen

import fi.elsapalvelu.elsa.config.arkistointi.ArkistointiConfiguration
import fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiAsiakirjaSnapshotService
import fi.elsapalvelu.elsa.required
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordProperties
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/** Supplements the configured async graduation document set, independently of transport or university. */
@Service
@Transactional(propagation = Propagation.MANDATORY)
class ValmistuminenArkistointiAsiakirjat(
    private val snapshots: ArkistointiAsiakirjaSnapshotService
) {
    fun taydenna(
        valmistumispyynto: Valmistumispyynto,
        settings: ArkistointiConfiguration,
        generated: List<RecordProperties>
    ): List<RecordProperties> {
        val includeLaillistamistodistus = settings.includesDocument(RecordType.LAILLISTAMISTODISTUS)
        val available = generated.map { it.type } + if (includeLaillistamistodistus) listOf(RecordType.LAILLISTAMISTODISTUS) else emptyList()
        check(settings.hasAllRequiredDocuments(available)) {
            "Configured graduation document is missing"
        }
        if (!includeLaillistamistodistus) {
            return generated
        }
        check(generated.none { it.type == RecordType.LAILLISTAMISTODISTUS }) {
            "Graduation certificate must be added only once"
        }
        val opintooikeus = valmistumispyynto.opintooikeus.required()
        val erikoistuvaLaakari = opintooikeus.erikoistuvaLaakari.required()
        val snapshot = snapshots.createSnapshot(
            opintooikeus, erikoistuvaLaakari.laillistamispaivanLiitetiedostonNimi, erikoistuvaLaakari.laillistamispaivanLiitetiedostonTyyppi,
            erikoistuvaLaakari.laillistamistodistus?.data, RecordType.LAILLISTAMISTODISTUS, generated.size
        )
        // The certificate has no source Asiakirja: use the snapshot's own ID, never an AsiakirjaData ID.
        // The enqueuer's createReference recognizes this persisted snapshot and reuses it.
        return generated + RecordProperties(snapshot.asiakirja, RecordType.LAILLISTAMISTODISTUS)
    }

}
