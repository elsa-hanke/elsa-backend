package fi.elsapalvelu.elsa.service.impl

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.domain.Asiakirja
import fi.elsapalvelu.elsa.domain.Opintooikeus
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.service.ArkistointiService
import fi.elsapalvelu.elsa.service.dto.arkistointi.ArkistointiMetadata
import fi.elsapalvelu.elsa.service.dto.arkistointi.Record
import org.apache.commons.codec.digest.DigestUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.ResourceBundle
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Service
class ArkistointiServiceImpl(
    private val applicationProperties: ApplicationProperties,
    private val tampereLouhiService: TampereLouhiService
) : ArkistointiService {
    private val log = LoggerFactory.getLogger(ArkistointiServiceImpl::class.java)

    override fun muodostaSahke(
        opintooikeus: Opintooikeus?,
        asiakirjat: List<Pair<Asiakirja, String>>,
        asiaTunnus: String,
        asiaTyyppi: String,
        hyvaksyja: String?,
        hyvaksymisPaiva: LocalDate?
    ): String {
        val name = opintooikeus?.erikoistuvaLaakari?.kayttaja?.user?.getName()
        val title = "Valmistuminen $name"
        var metadata = ArkistointiMetadata()

        val resourceBundle = ResourceBundle.getBundle("i18n/messages")

        var caseFile = metadata.caseFile
        caseFile.created = LocalDate.now()
        caseFile.nativeId = asiaTunnus
        caseFile.title = title

        caseFile.restriction.person.name = name
        caseFile.restriction.person.ssn = opintooikeus?.erikoistuvaLaakari?.syntymaaika

        var action = caseFile.action
        action.created = LocalDate.now()
        action.title = title
        action.type = asiaTyyppi

        asiakirjat.forEach {
            val asiakirja = it.first
            val record = Record()
            record.created = LocalDate.now()
            record.nativeId = asiakirja.id?.toString()
            record.title = asiakirja.nimi
            record.type = asiakirja.tyyppi
            record.retentionPeriod = it.second

            record.restriction.person.name = name
            record.restriction.person.ssn = opintooikeus?.erikoistuvaLaakari?.syntymaaika

            val custom = record.custom
            custom.sahkposti = opintooikeus?.erikoistuvaLaakari?.kayttaja?.user?.email
            custom.matkapuhelin = opintooikeus?.erikoistuvaLaakari?.kayttaja?.user?.phoneNumber
            custom.yliopisto = resourceBundle.getString(opintooikeus?.yliopisto?.nimi?.toString()!!)
            custom.hyvaksyja = hyvaksyja
            custom.hyvaksymispaiva = hyvaksymisPaiva

            val document = record.document
            document.nativeId = asiakirja.nimi
            document.file.name = asiakirja.nimi
            document.file.path = "pdf/${it.first.nimi}"

            document.format.name = asiakirja.tyyppi
            document.hashValue = DigestUtils.sha256Hex(asiakirja.asiakirjaData?.data)
            action.record.add(record)
        }

        var mapper = XmlMapper()
        mapper.registerModule(JavaTimeModule())
        mapper.dateFormat = SimpleDateFormat("dd.MM.yyyy")
        mapper.writeValue(File("./sahke.xml"), metadata)

        val filePath = "./$title.zip"
        var fos = FileOutputStream(filePath)
        var zipOut = ZipOutputStream(fos)

        asiakirjat.forEach {
            val asiakirja = it.first
            var zipEntry = ZipEntry("pdf/${asiakirja.nimi}")
            zipOut.putNextEntry(zipEntry)
            zipOut.write(asiakirja.asiakirjaData?.data)
        }

        zipOut.close()
        fos.close()

        return filePath
    }

    override fun laheta(yliopisto: YliopistoEnum, filePath: String) {
        when (yliopisto) {
            YliopistoEnum.TAMPEREEN_YLIOPISTO -> {
                tampereLouhiService.laheta(filePath)
            }

            YliopistoEnum.HELSINGIN_YLIOPISTO -> {
                tampereLouhiService.laheta(filePath)
            }

            else -> log.info("Integraatiota arkistointiin ei ole tuettu yliopistossa ${yliopisto.name}")
        }
    }

    override fun onKaytossa(yliopisto: YliopistoEnum): Boolean {
        when (yliopisto) {
            YliopistoEnum.OULUN_YLIOPISTO -> {
                return applicationProperties.getArkistointi().getOulu().kaytossa
            }

            YliopistoEnum.HELSINGIN_YLIOPISTO -> {
                return applicationProperties.getArkistointi().getHki().kaytossa
            }

            YliopistoEnum.TAMPEREEN_YLIOPISTO -> {
                return applicationProperties.getArkistointi().getTre().kaytossa
            }

            YliopistoEnum.TURUN_YLIOPISTO -> {
                return applicationProperties.getArkistointi().getTurku().kaytossa
            }

            YliopistoEnum.ITA_SUOMEN_YLIOPISTO -> {
                return applicationProperties.getArkistointi().getUef().kaytossa
            }
        }
    }
}
