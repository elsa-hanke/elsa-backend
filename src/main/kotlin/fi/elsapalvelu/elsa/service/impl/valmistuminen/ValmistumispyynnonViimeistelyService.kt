package fi.elsapalvelu.elsa.service.impl.valmistuminen

import fi.elsapalvelu.elsa.config.YEK_ERIKOISALA_ID
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.domain.valmistuminen.ValmistumispyynnonTarkistus
import fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto
import fi.elsapalvelu.elsa.required
import fi.elsapalvelu.elsa.service.arkistointi.ArkistointiService
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.mapper.valmistuminen.ValmistumispyynnonTarkistusMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ValmistumispyynnonViimeistelyService(
    private val asiakirjojenLuontiService: ValmistumispyynnonAsiakirjojenLuontiService,
    private val tarkistusService: ValmistumispyynnonTarkistusService,
    private val tarkistusMapper: ValmistumispyynnonTarkistusMapper,
    private val ilmoitusService: ValmistumispyynnonIlmoitusService,
    private val arkistointiService: ArkistointiService
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

        log.info("Arkistointi kaytossa, muodostetaan sahke [valmistumispyyntoId=$valmistumispyyntoId]")
        val sahke = arkistointiService.muodostaSahke(
            valmistumispyynto.opintooikeus,
            asiakirjat,
            caseId = valmistumispyynto.id.required().toString(),
            tarkastaja = valmistumispyynto.virkailija?.user?.getName(),
            tarkastusPaiva = valmistumispyynto.virkailijanKuittausaika,
            hyvaksyja = valmistumispyynto.vastuuhenkiloHyvaksyja?.user?.getName(),
            hyvaksymisPaiva = valmistumispyynto.vastuuhenkiloHyvaksyjaKuittausaika,
            yliopisto = yliopisto,
            caseType = CaseType.VALMISTUMINEN
        )
        val yek = valmistumispyynto.opintooikeus?.erikoisala.required().id == YEK_ERIKOISALA_ID
        arkistointiService.laheta(
            yliopisto = yliopisto.required(),
            filePath = sahke.zipFilePath,
            caseType = CaseType.VALMISTUMINEN,
            yek = yek,
            caseId = valmistumispyyntoId.toString(),
            erikoistujanNimi = valmistumispyynto.opintooikeus?.erikoistuvaLaakari
                ?.kayttaja?.user?.getName()
        )
        log.info("Sahke muodostettu ja lahetetty [valmistumispyyntoId=$valmistumispyyntoId, yek=$yek]")
    }

    fun onkoArkistointiKaytossa(yliopisto: YliopistoEnum?): Boolean =
        arkistointiService.onKaytossa(yliopisto.required(), CaseType.VALMISTUMINEN)
}
