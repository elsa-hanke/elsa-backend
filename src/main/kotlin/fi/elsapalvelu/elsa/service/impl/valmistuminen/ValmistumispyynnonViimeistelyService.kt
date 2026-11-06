package fi.elsapalvelu.elsa.service.impl.valmistuminen

import fi.elsapalvelu.elsa.extensions.isYek
import fi.elsapalvelu.elsa.config.arkistointi.ArkistointiConfigurationProvider
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.domain.valmistuminen.ValmistumispyynnonTarkistus
import fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto
import fi.elsapalvelu.elsa.required
import fi.elsapalvelu.elsa.service.arkistointi.job.creation.ArkistointiJobEnqueuer
import fi.elsapalvelu.elsa.service.arkistointi.SynchronousArkistointiService
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
    private val jobEnqueuer: ArkistointiJobEnqueuer,
    private val arkistointiConfigurationProvider: ArkistointiConfigurationProvider,
    private val archiveDocuments: ValmistuminenArkistointiDocuments
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun viimeistele(
        valmistumispyyntoId: Long,
        tarkistus: ValmistumispyynnonTarkistus,
        valmistumispyynto: Valmistumispyynto,
        yliopisto: YliopistoEnum?
    ) {
        val archiveEnabled = onkoArkistointiKaytossa(yliopisto)
        val asyncEnabled = archiveEnabled && arkistointiConfigurationProvider.isAsyncEnabled(yliopisto.required(), CaseType.VALMISTUMINEN)
        val university = yliopisto.required()
        if (asyncEnabled) {
            jobEnqueuer.enqueue(university, CaseType.VALMISTUMINEN, valmistumispyyntoId, {
                val documents = createDocuments(valmistumispyyntoId, tarkistus, valmistumispyynto)
                valmistumispyynto.opintooikeus?.erikoisala.required()
                val completed = archiveDocuments.complete(valmistumispyynto, university, documents)
                selectConfiguredDocuments(completed, university)
            }) { identities ->
                ValmistuminenArkistointiPayload.create(valmistumispyynto, university, identities)
            }
            return
        }
        val asiakirjat = createDocuments(valmistumispyyntoId, tarkistus, valmistumispyynto)

        log.info("Tarkistetaan arkistointi [valmistumispyyntoId=$valmistumispyyntoId, yliopisto=$yliopisto]")
        if (!archiveEnabled) {
            log.info("Arkistointi ei kaytossa [valmistumispyyntoId=$valmistumispyyntoId, yliopisto=$yliopisto]")
            return
        }

        valmistumispyynto.opintooikeus?.erikoisala.required()
        archiveSynchronously(valmistumispyynto, university, asiakirjat)
    }

    private fun createDocuments(
        valmistumispyyntoId: Long,
        tarkistus: ValmistumispyynnonTarkistus,
        valmistumispyynto: Valmistumispyynto
    ): List<RecordProperties> {
        log.info("Luodaan PDF:t [valmistumispyyntoId=$valmistumispyyntoId]")
        val tarkistusDTO = tarkistusService.taydenna(tarkistusMapper.toDto(tarkistus))
        val documents = asiakirjojenLuontiService.luo(tarkistusDTO, valmistumispyynto)
        log.info("PDF:t luotu [valmistumispyyntoId=$valmistumispyyntoId]")
        ilmoitusService.lahetaIlmoitusHyvaksynnasta(valmistumispyynto)
        log.info("Hyvaksynta-sahkoposti lahetetty [valmistumispyyntoId=$valmistumispyyntoId]")
        return documents
    }

    private fun archiveSynchronously(
        valmistumispyynto: Valmistumispyynto,
        yliopisto: YliopistoEnum,
        asiakirjat: List<RecordProperties>,
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
            caseType = CaseType.VALMISTUMINEN, yek = valmistumispyynto.isYek(),
            caseId = valmistumispyynto.id.required().toString(),
            erikoistujanNimi = valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.kayttaja?.user?.getName()
        )
    }

    private fun selectConfiguredDocuments(
        documents: List<RecordProperties>,
        university: YliopistoEnum
    ): List<RecordProperties> {
        val configuration = arkistointiConfigurationProvider.getConfiguration(university, CaseType.VALMISTUMINEN)
        return documents.filter {
            configuration.metadata.getDocumentMetadata(it.type, configuration.case) != null
        }
    }

    fun onkoArkistointiKaytossa(yliopisto: YliopistoEnum?): Boolean = arkistointiService.onKaytossa(yliopisto.required(), CaseType.VALMISTUMINEN)
}
