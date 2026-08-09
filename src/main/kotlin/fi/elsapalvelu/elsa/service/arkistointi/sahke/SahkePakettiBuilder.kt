package fi.elsapalvelu.elsa.service.arkistointi.sahke

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.service.dto.arkistointi.ArkistointiResult
import org.springframework.stereotype.Component
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Component
class SahkePakettiBuilder {
    fun build(
        metadataResult: SahkeMetadataResult,
        opintooikeus: Opintooikeus?,
        zipMetadata: Boolean
    ): ArkistointiResult {
        val mapper = createXmlMapper()
        val metadata = metadataResult.metadata
        val user = opintooikeus?.erikoistuvaLaakari?.kayttaja?.user
        val title = "${metadata.caseFile.action.type?.lowercase()}_" +
            "${user?.firstName?.lowercase()}_${user?.lastName?.lowercase()}"
        val filePath = "/tmp/$title.zip"

        ZipOutputStream(FileOutputStream(filePath)).use { zipOut ->
            if (zipMetadata) {
                zipOut.putNextEntry(ZipEntry("sahke.xml"))
                zipOut.write(mapper.writeValueAsBytes(metadata))
            }

            metadataResult.asiakirjat.forEach {
                val asiakirja = it.asiakirja
                zipOut.putNextEntry(ZipEntry("pdf/${asiakirja.nimi}"))
                zipOut.write(asiakirja.asiakirjaData?.data)
            }
        }

        val metadataBytes = if (zipMetadata) null else mapper.writeValueAsBytes(metadata)
        return ArkistointiResult(filePath, metadataBytes)
    }

    private fun createXmlMapper(): XmlMapper = XmlMapper().apply {
        registerModule(JavaTimeModule())
        dateFormat = SimpleDateFormat("dd.MM.yyyy")
        configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true)
    }
}
