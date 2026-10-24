package fi.elsapalvelu.elsa.service.arkistointi.koejakso

import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.config.arkistointi.ArkistointiConfigurationProvider
import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.koejakso.KoejaksonKoulutussopimus
import fi.elsapalvelu.elsa.domain.koejakso.KoejaksonVastuuhenkilonArvio
import fi.elsapalvelu.elsa.required
import fi.elsapalvelu.elsa.service.arkistointi.SynchronousArkistointiService
import fi.elsapalvelu.elsa.service.arkistointi.job.creation.ArkistointiJobCreator
import fi.elsapalvelu.elsa.service.arkistointi.job.creation.CreateArkistointiJobRequest
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiAsiakirjaSnapshotService
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordProperties
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(propagation = Propagation.MANDATORY)
class KoejaksoArkistointiService(
    private val configuration: ArkistointiConfigurationProvider,
    private val synchronousArchive: SynchronousArkistointiService,
    private val snapshots: ArkistointiAsiakirjaSnapshotService,
    private val creator: ArkistointiJobCreator,
    private val mapper: ObjectMapper
) {
    fun archive(sopimus: KoejaksonKoulutussopimus, document: Asiakirja) {
        val studyRight = sopimus.opintooikeus.required()
        archive(
            studyRight, document, CaseType.SOPIMUS, RecordType.SOPIMUS,
            KoejaksoArkistointiPayload.create(
                sopimus.id.required(), studyRight, "", null,
                sopimus.vastuuhenkilo?.user?.getName(), sopimus.vastuuhenkilonKuittausaika
            )
        )
    }

    fun archive(arvio: KoejaksonVastuuhenkilonArvio, document: Asiakirja) {
        val studyRight = arvio.opintooikeus.required()
        archive(
            studyRight, document, CaseType.KOEJAKSO, RecordType.ARVIOINTI,
            KoejaksoArkistointiPayload.create(
                arvio.id.required(), studyRight,
                arvio.virkailija?.user?.getName(), arvio.virkailijanKuittausaika,
                arvio.vastuuhenkilo?.user?.getName(), arvio.vastuuhenkilonKuittausaika,
                arvio.koejaksoHyvaksytty
            )
        )
    }

    private fun archive(
        studyRight: Opintooikeus,
        document: Asiakirja,
        caseType: CaseType,
        recordType: RecordType,
        payload: KoejaksoArkistointiPayload
    ) {
        if (!synchronousArchive.onKaytossa(payload.university, caseType)) return
        if (configuration.isAsyncEnabled(payload.university, caseType)) {
            val prefix = if (caseType == CaseType.SOPIMUS) "koejakso-sopimus" else "koejakso-arvio"
            creator.create(
                CreateArkistointiJobRequest(
                    university = payload.university,
                    caseType = caseType,
                    payload = mapper.writeValueAsString(payload),
                    key = "$prefix:${payload.university.name}:${payload.caseId}",
                    asiakirjat = listOf(snapshots.createReference(document, recordType, 0))
                )
            )
        } else {
            val archivePackage = synchronousArchive.muodostaSahke(
                studyRight, listOf(RecordProperties(document, recordType)), payload.caseId.toString(),
                payload.reviewer, payload.reviewDate, payload.approver, payload.approvalDate,
                payload.university, caseType
            )
            synchronousArchive.laheta(
                payload.university, archivePackage.zipFilePath, caseType, payload.yek,
                payload.caseId.toString(), payload.traineeName
            )
        }
    }
}
