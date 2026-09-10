package fi.elsapalvelu.elsa.service.seuranta

import fi.elsapalvelu.elsa.service.PdfTextFieldValidator
import fi.elsapalvelu.elsa.service.dto.seuranta.SeurantajaksoDTO
import org.springframework.stereotype.Component

@Component
class SeurantajaksoPdfTextValidator(
    private val pdfTextFieldValidator: PdfTextFieldValidator
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
        pdfTextFieldValidator.validate(
            fields = fields,
            pdfSource = PDF_SOURCE,
            sourceId = seurantajakso.id,
            sourceDate = seurantajakso.alkamispaiva,
            seurantajaksoId = seurantajakso.id,
            seurantajaksoStartDate = seurantajakso.alkamispaiva
        )
    }

    private companion object {
        const val PDF_SOURCE = "seurantajakso"
    }
}
