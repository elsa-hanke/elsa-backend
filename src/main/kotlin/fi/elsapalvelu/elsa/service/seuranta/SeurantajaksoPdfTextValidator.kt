package fi.elsapalvelu.elsa.service.seuranta

import fi.elsapalvelu.elsa.service.PdfTextValidator
import fi.elsapalvelu.elsa.service.PdfTextSanitizer
import fi.elsapalvelu.elsa.service.dto.seuranta.SeurantajaksoDTO
import fi.elsapalvelu.elsa.web.rest.errors.UnsupportedPdfCharactersException
import org.springframework.stereotype.Component

@Component
class SeurantajaksoPdfTextValidator(
    private val pdfTextValidator: PdfTextValidator
) {
    fun validateErikoistujanKentat(seurantajakso: SeurantajaksoDTO) {
        validate(
            seurantajakso,
            listOf(
                "oma-arviointi-seurantajaksolta" to seurantajakso.omaArviointi,
                "lisahuomioita" to seurantajakso.lisahuomioita,
                "seuraavan-jakson-tavoitteet" to seurantajakso.seuraavanJaksonTavoitteet,
                "yhteiset-merkinnat-keskustelusta-ja-jatkosuunnitelmista" to
                    seurantajakso.seurantakeskustelunYhteisetMerkinnat
            )
        )
    }

    fun validateKouluttajanKentat(seurantajakso: SeurantajaksoDTO) {
        validate(
            seurantajakso,
            listOf(
                "huolenaiheet" to seurantajakso.huolenaiheet,
                "lahikouluttajan-arviointi-jaksosta" to seurantajakso.kouluttajanArvio,
                "erikoisalan-tyoskentelyvalmiudet" to
                    seurantajakso.erikoisalanTyoskentelyvalmiudet,
                "jatkotoimet-ja-raportointi" to seurantajakso.jatkotoimetJaRaportointi
            )
        )
    }

    fun validateKaikkiKentat(seurantajakso: SeurantajaksoDTO) {
        validateErikoistujanKentat(seurantajakso)
        validateKouluttajanKentat(seurantajakso)
    }

    private fun validate(
        seurantajakso: SeurantajaksoDTO,
        fields: List<Pair<String, String?>>
    ) {
        fields.forEach { (field, text) ->
            val sanitizedText = text?.let(PdfTextSanitizer::sanitize)
            val unsupportedCharacters = pdfTextValidator.findUnsupportedCharacters(sanitizedText)
            if (unsupportedCharacters.isNotEmpty()) {
                throw UnsupportedPdfCharactersException(
                    field = field,
                    unsupportedCharacters = unsupportedCharacters.map { it.toString() },
                    seurantajaksoId = seurantajakso.id,
                    seurantajaksoStartDate = seurantajakso.alkamispaiva
                )
            }
        }
    }
}
