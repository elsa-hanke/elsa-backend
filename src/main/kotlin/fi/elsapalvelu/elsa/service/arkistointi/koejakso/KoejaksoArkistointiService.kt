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
    private val jobEnqueuer: ArkistointiJobEnqueuer
) {
    fun isAsyncEnabled(university: YliopistoEnum, caseType: CaseType): Boolean =
        configuration.isAsyncEnabled(university, caseType)

    fun archive(sopimus: KoejaksonKoulutussopimus, document: Asiakirja) {
        val opintooikeus = sopimus.opintooikeus.required()
        val university = opintooikeus.yliopisto?.nimi.required()
        if (!synchronousArchive.onKaytossa(university, CaseType.SOPIMUS)) return
        if (isAsyncEnabled(university, CaseType.SOPIMUS)) {
            jobEnqueuer.enqueue(university, CaseType.SOPIMUS, sopimus.id.required(),
                    { listOf(RecordProperties(document, RecordType.SOPIMUS)) }) { documents ->
                KoejaksoArkistointiPayload.create(
                    sopimus.id.required(), opintooikeus, documents, "", null,
                    sopimus.vastuuhenkilo?.user?.getName(), sopimus.vastuuhenkilonKuittausaika
                )
            }
        } else {
            val archivePackage = synchronousArchive.muodostaSahke(
                opintooikeus, listOf(RecordProperties(document, RecordType.SOPIMUS)), sopimus.id.required().toString(),
                "", null, sopimus.vastuuhenkilo?.user?.getName(), sopimus.vastuuhenkilonKuittausaika,
                university, CaseType.SOPIMUS
            )
            opintooikeus.erikoisala.required()
            synchronousArchive.laheta(
                university, archivePackage.zipFilePath, CaseType.SOPIMUS, opintooikeus.isYek(),
                sopimus.id.required().toString(), opintooikeus.erikoistuvaLaakari?.kayttaja?.user?.getName()
            )
        }
    }

    fun archive(arvio: KoejaksonVastuuhenkilonArvio, document: Asiakirja) {
        val opintooikeus = arvio.opintooikeus.required()
        val university = opintooikeus.yliopisto?.nimi.required()
        // Preserve the synchronous path's required specialty, including when archiving is disabled.
        if (!isAsyncEnabled(university, CaseType.KOEJAKSO)) {
            opintooikeus.erikoisala.required()
            if (!synchronousArchive.onKaytossa(university, CaseType.KOEJAKSO)) return
            val archivePackage = synchronousArchive.muodostaSahke(
                opintooikeus, listOf(RecordProperties(document, RecordType.ARVIOINTI)), arvio.id.required().toString(),
                arvio.virkailija?.user?.getName(), arvio.virkailijanKuittausaika,
                arvio.vastuuhenkilo?.user?.getName(), arvio.vastuuhenkilonKuittausaika,
                university, CaseType.KOEJAKSO
            )
            synchronousArchive.laheta(
                university, archivePackage.zipFilePath, CaseType.KOEJAKSO, opintooikeus.isYek(),
                arvio.id.required().toString(), opintooikeus.erikoistuvaLaakari?.kayttaja?.user?.getName()
            )
            return
        }
        if (!synchronousArchive.onKaytossa(university, CaseType.KOEJAKSO)) return
        jobEnqueuer.enqueue(university, CaseType.KOEJAKSO, arvio.id.required(),
            { listOf(RecordProperties(document, RecordType.ARVIOINTI)) }) { documents ->
            KoejaksoArkistointiPayload.create(
                arvio.id.required(), opintooikeus, documents, arvio.virkailija?.user?.getName(), arvio.virkailijanKuittausaika,
                arvio.vastuuhenkilo?.user?.getName(), arvio.vastuuhenkilonKuittausaika, arvio.koejaksoHyvaksytty
            )
        }
    }

}
