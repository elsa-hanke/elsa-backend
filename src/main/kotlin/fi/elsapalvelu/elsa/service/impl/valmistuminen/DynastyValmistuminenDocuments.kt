package fi.elsapalvelu.elsa.service.impl.valmistuminen

import fi.elsapalvelu.elsa.config.arkistointi.ArkistointiConfigurationProvider
import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.kayttaja.AsiakirjaData
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto
import fi.elsapalvelu.elsa.repository.kayttaja.AsiakirjaRepository
import fi.elsapalvelu.elsa.required
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiAsiakirjaException
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiAsiakirjaErrorCategory
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiAsiakirjaErrors
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordProperties
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/** Supplements async graduation only; YEK requires the same certificate as EL/EHL. */
@Service
@Transactional(propagation = Propagation.MANDATORY)
class DynastyValmistuminenDocuments(
    private val configuration: ArkistointiConfigurationProvider,
    private val documents: AsiakirjaRepository
) {
    fun complete(request: Valmistumispyynto, university: YliopistoEnum, generated: List<RecordProperties>): List<RecordProperties> {
        if (university !in setOf(YliopistoEnum.TURUN_YLIOPISTO, YliopistoEnum.ITA_SUOMEN_YLIOPISTO)) return generated
        val settings = configuration.getConfiguration(university, CaseType.VALMISTUMINEN)
        check(requiredTypes.all { settings.metadata.getDocumentMetadata(it, settings.case) != null }) {
            "Dynasty graduation document configuration is incomplete"
        }
        check(generated.map { it.type } == listOf(RecordType.YHTEENVETO, RecordType.LIITE)) {
            "Dynasty graduation document set is incomplete"
        }
        val studyRight = request.opintooikeus.required()
        val trainee = studyRight.erikoistuvaLaakari.required()
        val bytes = trainee.laillistamistodistus?.data
        val filename = trainee.laillistamispaivanLiitetiedostonNimi
        val contentType = trainee.laillistamispaivanLiitetiedostonTyyppi
        val error = when {
            bytes == null -> ArkistointiAsiakirjaErrors.missing()
            bytes.isEmpty() -> ArkistointiAsiakirjaErrors.empty()
            filename.isNullOrBlank() || filename.length > 255 || contentType.isNullOrBlank() || contentType.length > 255 ->
                ArkistointiAsiakirjaErrors.invalid()
            else -> null
        }
        if (error != null) throw ArkistointiAsiakirjaException(error, ArkistointiAsiakirjaErrorCategory.INPUT_DOCUMENT)
        // A real Asiakirja ID is needed by the manifest; AsiakirjaData IDs are a different namespace.
        val source = documents.save(Asiakirja(opintooikeus = studyRight, nimi = filename, tyyppi = contentType,
            lisattypvm = LocalDateTime.now(), asiakirjaData = AsiakirjaData(data = requireNotNull(bytes).copyOf())))
        return generated + RecordProperties(source, RecordType.LAILLISTAMISTODISTUS)
    }

    private companion object {
        val requiredTypes = listOf(RecordType.YHTEENVETO, RecordType.LIITE, RecordType.LAILLISTAMISTODISTUS)
    }
}
