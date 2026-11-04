package fi.elsapalvelu.elsa.service.arkistointi.koejakso

import fi.elsapalvelu.elsa.extensions.isYek
import fi.elsapalvelu.elsa.config.arkistointi.ArkistointiConfigurationProvider
import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.koejakso.KoejaksonKoulutussopimus
import fi.elsapalvelu.elsa.domain.koejakso.KoejaksonVastuuhenkilonArvio
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.required
import fi.elsapalvelu.elsa.service.arkistointi.job.creation.ArkistointiJobEnqueuer
import fi.elsapalvelu.elsa.service.arkistointi.SynchronousArkistointiService
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
    private val enqueuer: ArkistointiJobEnqueuer
) {
    fun isAsyncEnabled(university: YliopistoEnum, caseType: CaseType): Boolean =
        configuration.isAsyncEnabled(university, caseType)

    fun archive(sopimus: KoejaksonKoulutussopimus, document: Asiakirja) {
        val studyRight = sopimus.opintooikeus.required()
        val university = studyRight.yliopisto?.nimi.required()
        if (!synchronousArchive.onKaytossa(university, CaseType.SOPIMUS)) return
        if (isAsyncEnabled(university, CaseType.SOPIMUS)) {
            enqueuer.enqueue(university, CaseType.SOPIMUS, sopimus.id.required(),
                { listOf(RecordProperties(document, RecordType.SOPIMUS)) }) { documents ->
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
            studyRight.erikoisala.required()
            synchronousArchive.laheta(
                university, archivePackage.zipFilePath, CaseType.SOPIMUS, studyRight.isYek(),
                sopimus.id.required().toString(), studyRight.erikoistuvaLaakari?.kayttaja?.user?.getName()
            )
        }
    }

    fun archive(arvio: KoejaksonVastuuhenkilonArvio, document: Asiakirja) {
        val studyRight = arvio.opintooikeus.required()
        val university = studyRight.yliopisto?.nimi.required()
        // Preserve the synchronous path's required specialty, including when archiving is disabled.
        if (!isAsyncEnabled(university, CaseType.KOEJAKSO)) {
            studyRight.erikoisala.required()
            if (!synchronousArchive.onKaytossa(university, CaseType.KOEJAKSO)) return
            val archivePackage = synchronousArchive.muodostaSahke(
                studyRight, listOf(RecordProperties(document, RecordType.ARVIOINTI)), arvio.id.required().toString(),
                arvio.virkailija?.user?.getName(), arvio.virkailijanKuittausaika,
                arvio.vastuuhenkilo?.user?.getName(), arvio.vastuuhenkilonKuittausaika,
                university, CaseType.KOEJAKSO
            )
            synchronousArchive.laheta(
                university, archivePackage.zipFilePath, CaseType.KOEJAKSO, studyRight.isYek(),
                arvio.id.required().toString(), studyRight.erikoistuvaLaakari?.kayttaja?.user?.getName()
            )
            return
        }
        if (!synchronousArchive.onKaytossa(university, CaseType.KOEJAKSO)) return
        enqueuer.enqueue(university, CaseType.KOEJAKSO, arvio.id.required(),
            { listOf(RecordProperties(document, RecordType.ARVIOINTI)) }) { documents ->
            KoejaksoArkistointiPayload.create(
                arvio.id.required(), studyRight, documents, arvio.virkailija?.user?.getName(), arvio.virkailijanKuittausaika,
                arvio.vastuuhenkilo?.user?.getName(), arvio.vastuuhenkilonKuittausaika, arvio.koejaksoHyvaksytty
            )
        }
    }

}
