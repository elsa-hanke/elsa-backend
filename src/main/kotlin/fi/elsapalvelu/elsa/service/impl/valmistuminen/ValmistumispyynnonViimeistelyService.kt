package fi.elsapalvelu.elsa.service.impl.valmistuminen

import fi.elsapalvelu.elsa.config.YEK_ERIKOISALA_ID
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.domain.valmistuminen.ValmistumispyynnonTarkistus
import fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto
import fi.elsapalvelu.elsa.required
import fi.elsapalvelu.elsa.service.arkistointi.ArkistointiService
import fi.elsapalvelu.elsa.service.arkistointi.job.ArkistointiAsiakirjaSnapshotService
import fi.elsapalvelu.elsa.service.arkistointi.job.ArkistointiJobCreator
import fi.elsapalvelu.elsa.service.arkistointi.job.CreateArkistointiJobRequest
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
    private val arkistointiService: ArkistointiService,
    private val arkistointiJobCreator: ArkistointiJobCreator,
    private val asiakirjaSnapshotService: ArkistointiAsiakirjaSnapshotService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun viimeistele(
        valmistumispyyntoId: Long,
        tarkistus: ValmistumispyynnonTarkistus,
        valmistumispyynto: Valmistumispyynto,
        yliopisto: YliopistoEnum?
    ) {
        log.info("Luodaan PDF:t [valmistumispyyntoId=$valmistumispyyntoId]")
        val tarkistusDTO = tarkistusService.taydenna(tarkistusMapper.toDto(tarkistus))
        val asiakirjat = asiakirjojenLuontiService.luo(tarkistusDTO, valmistumispyynto)
        log.info("PDF:t luotu [valmistumispyyntoId=$valmistumispyyntoId]")
        ilmoitusService.lahetaIlmoitusHyvaksynnasta(valmistumispyynto)
        log.info("Hyvaksynta-sahkoposti lahetetty [valmistumispyyntoId=$valmistumispyyntoId]")

        log.info("Tarkistetaan arkistointi [valmistumispyyntoId=$valmistumispyyntoId, yliopisto=$yliopisto]")
        if (!onkoArkistointiKaytossa(yliopisto)) {
            log.info(
                "Arkistointi ei kaytossa " +
                    "[valmistumispyyntoId=$valmistumispyyntoId, yliopisto=$yliopisto]"
            )
            return
        }

        val yek = valmistumispyynto.opintooikeus?.erikoisala.required().id == YEK_ERIKOISALA_ID
        val references = createDocumentReferences(asiakirjat, valmistumispyynto, yek)
        arkistointiJobCreator.create(
            CreateArkistointiJobRequest(
                university = yliopisto.required(),
                caseType = CaseType.VALMISTUMINEN,
                payload = "{\"valmistumispyyntoId\":$valmistumispyyntoId}",
                key = "valmistuminen:${yliopisto.required().name}:$valmistumispyyntoId",
                asiakirjat = references
            )
        )
        log.info("Arkistointi-job luotu [valmistumispyyntoId=$valmistumispyyntoId, yek=$yek]")
    }

    private fun createDocumentReferences(
        asiakirjat: List<RecordProperties>,
        valmistumispyynto: Valmistumispyynto,
        yek: Boolean
    ) = buildList {
        asiakirjat.forEachIndexed { index, record ->
            add(asiakirjaSnapshotService.createReference(record.asiakirja, record.type, index))
        }
        if (!yek) {
            add(
                asiakirjaSnapshotService.createLaillistamistodistusReference(
                    valmistumispyynto.opintooikeus.required(),
                    size
                )
            )
        }
    }

    fun onkoArkistointiKaytossa(yliopisto: YliopistoEnum?): Boolean =
        arkistointiService.onKaytossa(yliopisto.required(), CaseType.VALMISTUMINEN)
}
