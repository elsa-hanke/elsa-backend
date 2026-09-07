package fi.elsapalvelu.elsa.service.arkistointi.job

import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.kayttaja.AsiakirjaData
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.repository.kayttaja.AsiakirjaRepository
import fi.elsapalvelu.elsa.service.PdfA2bService
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ArkistointiAsiakirjaSnapshotService(
    private val asiakirjaRepository: AsiakirjaRepository,
    private val pdfA2bService: PdfA2bService
) {

    fun createReference(
        asiakirja: Asiakirja,
        asiakirjatyyppi: RecordType,
        jarjestysnumero: Int
    ): ArkistointiAsiakirjaReference {
        val identifier = asiakirja.nimi ?: asiakirja.id?.toString() ?: "tuntematon"
        if (jarjestysnumero < 0 || asiakirja.nimi.isNullOrBlank()) {
            throw ArkistointiAsiakirjaException(ArkistointiAsiakirjaErrors.invalid(identifier))
        }
        val data = asiakirja.asiakirjaData?.data
            ?: throw ArkistointiAsiakirjaException(ArkistointiAsiakirjaErrors.missing(identifier))
        if (data.isEmpty()) {
            throw ArkistointiAsiakirjaException(ArkistointiAsiakirjaErrors.empty(identifier))
        }
        if (asiakirja.id == null) {
            throw ArkistointiAsiakirjaException(ArkistointiAsiakirjaErrors.missing(identifier))
        }
        if (asiakirja.tyyppi != MediaType.APPLICATION_PDF_VALUE || !pdfA2bService.isPdfA2b(data)) {
            throw ArkistointiAsiakirjaException(ArkistointiAsiakirjaErrors.notPdfA2b(identifier))
        }

        return ArkistointiAsiakirjaReference(
            asiakirja = asiakirja,
            asiakirjatyyppi = asiakirjatyyppi,
            jarjestysnumero = jarjestysnumero,
            filename = asiakirja.nimi
                ?: throw ArkistointiAsiakirjaException(ArkistointiAsiakirjaErrors.missing(identifier)),
            contentType = asiakirja.tyyppi
                ?: throw ArkistointiAsiakirjaException(ArkistointiAsiakirjaErrors.missing(identifier)),
            sha256 = ArkistointiChecksum.sha256(data)
        )
    }

    @Transactional
    fun createSnapshot(
        opintooikeus: Opintooikeus,
        filename: String?,
        contentType: String?,
        data: ByteArray?,
        asiakirjatyyppi: RecordType,
        jarjestysnumero: Int
    ): ArkistointiAsiakirjaReference {
        val identifier = filename ?: asiakirjatyyppi.name
        val normalizedData = try {
            pdfA2bService.normalize(data, contentType)
        } catch (exception: RuntimeException) {
            throw ArkistointiAsiakirjaException(
                if (data == null) {
                    ArkistointiAsiakirjaErrors.missing(identifier)
                } else if (data.isEmpty()) {
                    ArkistointiAsiakirjaErrors.empty(identifier)
                } else {
                    ArkistointiAsiakirjaErrors.invalid(identifier)
                },
                exception
            )
        }
        val snapshot = asiakirjaRepository.save(
            Asiakirja(
                opintooikeus = opintooikeus,
                nimi = pdfFilename(filename, asiakirjatyyppi),
                tyyppi = MediaType.APPLICATION_PDF_VALUE,
                lisattypvm = LocalDateTime.now(),
                asiakirjaData = AsiakirjaData(data = normalizedData)
            )
        )
        return createReference(snapshot, asiakirjatyyppi, jarjestysnumero)
    }

    @Transactional
    fun createLaillistamistodistusReference(
        opintooikeus: Opintooikeus,
        jarjestysnumero: Int
    ): ArkistointiAsiakirjaReference {
        val erikoistuvaLaakari = opintooikeus.erikoistuvaLaakari
            ?: throw ArkistointiAsiakirjaException(
                ArkistointiAsiakirjaErrors.missing(RecordType.LAILLISTAMISTODISTUS.name)
            )
        return createSnapshot(
            opintooikeus = opintooikeus,
            filename = erikoistuvaLaakari.laillistamispaivanLiitetiedostonNimi,
            contentType = erikoistuvaLaakari.laillistamispaivanLiitetiedostonTyyppi,
            data = erikoistuvaLaakari.laillistamistodistus?.data,
            asiakirjatyyppi = RecordType.LAILLISTAMISTODISTUS,
            jarjestysnumero = jarjestysnumero
        )
    }

    private fun pdfFilename(filename: String?, asiakirjatyyppi: RecordType): String {
        val basename = filename?.substringBeforeLast('.', filename)?.takeIf { it.isNotBlank() }
            ?: asiakirjatyyppi.name.lowercase()
        return "${basename.take(MAX_FILENAME_LENGTH - PDF_EXTENSION.length)}$PDF_EXTENSION"
    }

    private companion object {
        const val MAX_FILENAME_LENGTH = 255
        const val PDF_EXTENSION = ".pdf"
    }
}
