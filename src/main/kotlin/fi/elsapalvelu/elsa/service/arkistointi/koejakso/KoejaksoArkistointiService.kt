package fi.elsapalvelu.elsa.service.arkistointi.koejakso

import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.config.YEK_ERIKOISALA_ID
import fi.elsapalvelu.elsa.config.arkistointi.ArkistointiConfigurationProvider
import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.koejakso.KoejaksonKoulutussopimus
import fi.elsapalvelu.elsa.domain.koejakso.KoejaksonVastuuhenkilonArvio
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.required
import fi.elsapalvelu.elsa.repository.arkistointi.ArkistointiJobRepository
import fi.elsapalvelu.elsa.service.arkistointi.SynchronousArkistointiService
import fi.elsapalvelu.elsa.service.arkistointi.job.creation.ArkistointiJobCreator
import fi.elsapalvelu.elsa.service.arkistointi.job.creation.CreateArkistointiJobRequest
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiAsiakirjaSnapshotService
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiDocumentIdentity
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
    private val mapper: ObjectMapper,
    private val jobRepository: ArkistointiJobRepository
) {
    fun isAsyncEnabled(university: YliopistoEnum, caseType: CaseType): Boolean =
        configuration.isAsyncEnabled(university, caseType)

    fun archive(sopimus: KoejaksonKoulutussopimus, document: Asiakirja) {
        val studyRight = sopimus.opintooikeus.required()
        val university = studyRight.yliopisto?.nimi.required()
        if (!synchronousArchive.onKaytossa(university, CaseType.SOPIMUS)) return
        if (isAsyncEnabled(university, CaseType.SOPIMUS)) {
            archiveAsync(studyRight, document, CaseType.SOPIMUS, RecordType.SOPIMUS, sopimus.id.required()) { documents ->
                KoejaksoArkistointiPayload.create(
                    sopimus.id.required(), studyRight, documents, "", null,
                    sopimus.vastuuhenkilo?.user?.getName(), sopimus.vastuuhenkilonKuittausaika
                )
            }
        } else {
            val archivePackage = synchronousArchive.muodostaSahke(
                studyRight, listOf(RecordProperties(document, RecordType.SOPIMUS)), sopimus.id.required().toString(),
                "", null, sopimus.vastuuhenkilo?.user?.getName(), sopimus.vastuuhenkilonKuittausaika,
                university, CaseType.SOPIMUS
            )
            val specialty = studyRight.erikoisala.required()
            synchronousArchive.laheta(
                university, archivePackage.zipFilePath, CaseType.SOPIMUS, specialty.id == YEK_ERIKOISALA_ID,
                sopimus.id.required().toString(), studyRight.erikoistuvaLaakari?.kayttaja?.user?.getName()
            )
        }
    }

    fun archive(arvio: KoejaksonVastuuhenkilonArvio, document: Asiakirja) {
        val studyRight = arvio.opintooikeus.required()
        val university = studyRight.yliopisto?.nimi.required()
        // Preserve the synchronous path's required specialty, including when archiving is disabled.
        if (!isAsyncEnabled(university, CaseType.KOEJAKSO)) {
            val specialty = studyRight.erikoisala.required()
            if (!synchronousArchive.onKaytossa(university, CaseType.KOEJAKSO)) return
            val archivePackage = synchronousArchive.muodostaSahke(
                studyRight, listOf(RecordProperties(document, RecordType.ARVIOINTI)), arvio.id.required().toString(),
                arvio.virkailija?.user?.getName(), arvio.virkailijanKuittausaika,
                arvio.vastuuhenkilo?.user?.getName(), arvio.vastuuhenkilonKuittausaika,
                university, CaseType.KOEJAKSO
            )
            synchronousArchive.laheta(
                university, archivePackage.zipFilePath, CaseType.KOEJAKSO, specialty.id == YEK_ERIKOISALA_ID,
                arvio.id.required().toString(), studyRight.erikoistuvaLaakari?.kayttaja?.user?.getName()
            )
            return
        }
        if (!synchronousArchive.onKaytossa(university, CaseType.KOEJAKSO)) return
        archiveAsync(studyRight, document, CaseType.KOEJAKSO, RecordType.ARVIOINTI, arvio.id.required()) { documents ->
            KoejaksoArkistointiPayload.create(
                arvio.id.required(), studyRight, documents, arvio.virkailija?.user?.getName(), arvio.virkailijanKuittausaika,
                arvio.vastuuhenkilo?.user?.getName(), arvio.vastuuhenkilonKuittausaika, arvio.koejaksoHyvaksytty
            )
        }
    }

    private fun archiveAsync(
        studyRight: Opintooikeus,
        document: Asiakirja,
        caseType: CaseType,
        recordType: RecordType,
        caseId: Long,
        createPayload: (List<ArkistointiDocumentIdentity>) -> KoejaksoArkistointiPayload
    ) {
        val university = studyRight.yliopisto?.nimi.required()
        val prefix = if (caseType == CaseType.SOPIMUS) "koejakso-sopimus" else "koejakso-arvio"
        val key = "$prefix:${university.name}:$caseId"
        // Approval callers lock the case row. Check before snapshot creation; creator still checks the key too.
        if (jobRepository.findByKey(key) != null) return
        val sourceId = document.id.required()
        val reference = snapshots.createReference(document, recordType, 0)
        val payload = createPayload(listOf(ArkistointiDocumentIdentity(
            sourceDocumentId = sourceId,
            snapshotDocumentId = reference.asiakirja.id.required(),
            recordType = reference.asiakirjatyyppi,
            jarjestysnumero = reference.jarjestysnumero
        )))
        creator.create(
            CreateArkistointiJobRequest(
                university = payload.university,
                caseType = caseType,
                payload = mapper.writeValueAsString(payload),
                key = key,
                asiakirjat = listOf(reference)
            )
        )
    }
}
