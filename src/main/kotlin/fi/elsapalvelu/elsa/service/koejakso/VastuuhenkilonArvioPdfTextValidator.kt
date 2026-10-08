package fi.elsapalvelu.elsa.service.koejakso

import fi.elsapalvelu.elsa.service.PdfTextFieldValidator
import fi.elsapalvelu.elsa.service.dto.koejakso.KoejaksonVastuuhenkilonArvioDTO
import org.springframework.stereotype.Component

@Component
class VastuuhenkilonArvioPdfTextValidator(private val validator: PdfTextFieldValidator) {
    fun validateLuonti(dto: KoejaksonVastuuhenkilonArvioDTO, attachmentNames: Iterable<String?>) {
        validate(dto, listOf(
            "sahkoposti" to dto.erikoistuvanSahkoposti,
            "puhelinnumero" to dto.erikoistuvanPuhelinnumero,
            "perustelu-hylkaamiselle" to dto.perusteluHylkaamiselle,
            "lisatiedot-vastuuhenkilolle" to dto.lisatiedotVirkailijalta,
            "korjausehdotus" to dto.virkailijanKorjausehdotus,
            "korjausehdotus" to dto.vastuuhenkilonKorjausehdotus
        ) + attachmentNames.map { "liitetiedoston-nimi" to it }, summary(dto))
    }

    fun validateErikoistujanKentat(dto: KoejaksonVastuuhenkilonArvioDTO, attachmentNames: Iterable<String?>) {
        validate(dto, listOf(
            "sahkoposti" to dto.erikoistuvanSahkoposti,
            "puhelinnumero" to dto.erikoistuvanPuhelinnumero
        ) + attachmentNames.map { "liitetiedoston-nimi" to it })
    }

    fun validateVirkailijanKentat(dto: KoejaksonVastuuhenkilonArvioDTO) {
        val fields = mutableListOf("korjausehdotus" to dto.virkailijanKorjausehdotus)
        if (dto.virkailijanKorjausehdotus.isNullOrBlank()) {
            fields.add("lisatiedot-vastuuhenkilolle" to dto.lisatiedotVirkailijalta)
        }
        validate(dto, fields, summary(dto))
    }

    fun validateVastuuhenkilonKentat(dto: KoejaksonVastuuhenkilonArvioDTO) {
        val fields = mutableListOf(
            "sahkoposti" to dto.vastuuhenkilonSahkoposti,
            "puhelinnumero" to dto.vastuuhenkilonPuhelinnumero,
            "korjausehdotus" to dto.vastuuhenkilonKorjausehdotus
        )
        if (dto.vastuuhenkilonKorjausehdotus.isNullOrBlank() && dto.koejaksoHyvaksytty == false) {
            fields.add("perustelu-hylkaamiselle" to dto.perusteluHylkaamiselle)
        }
        validate(dto, fields)
    }

    private fun summary(dto: KoejaksonVastuuhenkilonArvioDTO) =
        listOf("virkailijan-valmistumisen-yhteenveto" to dto.virkailijanYhteenveto)

    private fun validate(
        dto: KoejaksonVastuuhenkilonArvioDTO,
        fields: List<Pair<String, String?>>,
        htmlFields: List<Pair<String, String?>> = emptyList()
    ) {
        validator.validate(fields = fields, htmlFields = htmlFields, pdfSource = "koejaksonvastuuhenkilonarvio", sourceId = dto.id)
    }
}
