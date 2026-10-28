package fi.elsapalvelu.elsa.service.impl.valmistuminen

import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.config.YEK_ERIKOISALA_ID
import fi.elsapalvelu.elsa.config.arkistointi.ArkistointiConfigurationProvider
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.domain.valmistuminen.ValmistumispyynnonTarkistus
import fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto
import fi.elsapalvelu.elsa.required
import fi.elsapalvelu.elsa.repository.arkistointi.ArkistointiJobRepository
import fi.elsapalvelu.elsa.service.arkistointi.SynchronousArkistointiService
import fi.elsapalvelu.elsa.service.arkistointi.job.creation.ArkistointiJobCreator
import fi.elsapalvelu.elsa.service.arkistointi.job.creation.CreateArkistointiJobRequest
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiAsiakirjaSnapshotService
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiAsiakirjaReference
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiDocumentIdentity
import fi.elsapalvelu.elsa.service.arkistointi.valmistuminen.ValmistuminenArkistointiPayload
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordProperties
import fi.elsapalvelu.elsa.service.mapper.valmistuminen.ValmistumispyynnonTarkistusMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ValmistumispyynnonViimeistelyService(
    private val asiakirjojenLuontiService: ValmistumispyynnonAsiakirjojenLuontiService,
    private val tarkistusService: ValmistumispyynnonTarkistusService,
    private val tarkistusMapper: ValmistumispyynnonTarkistusMapper,
    private val ilmoitusService: ValmistumispyynnonIlmoitusService,
    private val arkistointiService: SynchronousArkistointiService,
    private val arkistointiJobCreator: ArkistointiJobCreator,
    private val arkistointiJobRepository: ArkistointiJobRepository,
    private val asiakirjaSnapshotService: ArkistointiAsiakirjaSnapshotService,
    private val arkistointiConfigurationProvider: ArkistointiConfigurationProvider,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun viimeistele(
        valmistumispyyntoId: Long,
        tarkistus: ValmistumispyynnonTarkistus,
        valmistumispyynto: Valmistumispyynto,
        yliopisto: YliopistoEnum?
    ) {
        val archiveEnabled = onkoArkistointiKaytossa(yliopisto)
        val asyncEnabled = archiveEnabled &&
            arkistointiConfigurationProvider.isAsyncEnabled(yliopisto.required(), CaseType.VALMISTUMINEN)
        val jobKey = "valmistuminen:${yliopisto.required().name}:$valmistumispyyntoId"
        if (asyncEnabled && arkistointiJobRepository.findByKey(jobKey) != null) {
            // Approval holds the request row lock; the creator keeps its final key check as well.
            log.info("Arkistointi-job on jo olemassa [valmistumispyyntoId=$valmistumispyyntoId]")
            return
        }

        log.info("Luodaan PDF:t [valmistumispyyntoId=$valmistumispyyntoId]")
        val tarkistusDTO = tarkistusService.taydenna(tarkistusMapper.toDto(tarkistus))
        val asiakirjat = asiakirjojenLuontiService.luo(tarkistusDTO, valmistumispyynto)
        log.info("PDF:t luotu [valmistumispyyntoId=$valmistumispyyntoId]")
        ilmoitusService.lahetaIlmoitusHyvaksynnasta(valmistumispyynto)
        log.info("Hyvaksynta-sahkoposti lahetetty [valmistumispyyntoId=$valmistumispyyntoId]")

        log.info("Tarkistetaan arkistointi [valmistumispyyntoId=$valmistumispyyntoId, yliopisto=$yliopisto]")
        if (!archiveEnabled) {
            log.info(
                "Arkistointi ei kaytossa " +
                    "[valmistumispyyntoId=$valmistumispyyntoId, yliopisto=$yliopisto]"
            )
            return
        }

        val yek = valmistumispyynto.opintooikeus?.erikoisala.required().id == YEK_ERIKOISALA_ID
        if (!asyncEnabled) {
            archiveSynchronously(valmistumispyynto, yliopisto.required(), asiakirjat, yek)
            return
        }
        val documents = prepareDocuments(asiakirjat, yliopisto.required())
        val valmistuminenArkistointiPayload = ValmistuminenArkistointiPayload.create(valmistumispyynto, yliopisto.required(), documents.map { it.identity })
        arkistointiJobCreator.create(
            CreateArkistointiJobRequest(
                university = yliopisto.required(),
                caseType = CaseType.VALMISTUMINEN,
                payload = objectMapper.writeValueAsString(valmistuminenArkistointiPayload),
                key = jobKey,
                asiakirjat = documents.map { it.reference }
            )
        )
        log.info("Arkistointi-job luotu [valmistumispyyntoId=$valmistumispyyntoId, yek=$yek]")
    }

    private fun archiveSynchronously(
        valmistumispyynto: Valmistumispyynto,
        yliopisto: YliopistoEnum,
        asiakirjat: List<RecordProperties>,
        yek: Boolean
    ) {
        val sahke = arkistointiService.muodostaSahke(
            valmistumispyynto.opintooikeus, asiakirjat,
            caseId = valmistumispyynto.id.required().toString(),
            tarkastaja = valmistumispyynto.virkailija?.user?.getName(),
            tarkastusPaiva = valmistumispyynto.virkailijanKuittausaika,
            hyvaksyja = valmistumispyynto.vastuuhenkiloHyvaksyja?.user?.getName(),
            hyvaksymisPaiva = valmistumispyynto.vastuuhenkiloHyvaksyjaKuittausaika,
            yliopisto = yliopisto, caseType = CaseType.VALMISTUMINEN
        )
        arkistointiService.laheta(
            yliopisto = yliopisto, filePath = sahke.zipFilePath,
            caseType = CaseType.VALMISTUMINEN, yek = yek,
            caseId = valmistumispyynto.id.required().toString(),
            erikoistujanNimi = valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.kayttaja?.user?.getName()
        )
    }

    private fun prepareDocuments(
        asiakirjat: List<RecordProperties>,
        yliopisto: YliopistoEnum
    ): List<PreparedDocument> {
        val configuration = arkistointiConfigurationProvider.getConfiguration(yliopisto, CaseType.VALMISTUMINEN)
        val configuredDocuments = asiakirjat.filter {
            configuration.metadata.getDocumentMetadata(it.type, configuration.case) != null
        }
        return configuredDocuments.mapIndexed { index, record ->
            // Apply the same university/case document selection as the synchronous SÄHKE builder.
            val sourceId = record.asiakirja.id.required()
            val reference = asiakirjaSnapshotService.createReference(record.asiakirja, record.type, index)
            PreparedDocument(reference, ArkistointiDocumentIdentity(
                sourceDocumentId = sourceId,
                snapshotDocumentId = reference.asiakirja.id.required(),
                recordType = reference.asiakirjatyyppi,
                jarjestysnumero = reference.jarjestysnumero
            ))
        }
    }

    private data class PreparedDocument(
        val reference: ArkistointiAsiakirjaReference,
        val identity: ArkistointiDocumentIdentity
    )

    fun onkoArkistointiKaytossa(yliopisto: YliopistoEnum?): Boolean = arkistointiService.onKaytossa(yliopisto.required(), CaseType.VALMISTUMINEN)
}
