package fi.elsapalvelu.elsa.service.koejakso

import fi.elsapalvelu.elsa.service.PdfTextFieldValidator
import fi.elsapalvelu.elsa.service.dto.koejakso.KoejaksonKoulutussopimusDTO
import fi.elsapalvelu.elsa.service.dto.koejakso.KoulutussopimuksenKouluttajaDTO
import org.springframework.stereotype.Component

@Component
class KoulutussopimusPdfTextValidator(private val validator: PdfTextFieldValidator) {
    fun validateErikoistujanKentat(dto: KoejaksonKoulutussopimusDTO) {
        val fields = mutableListOf(
            "sahkoposti" to dto.erikoistuvanSahkoposti,
            "puhelinnumero" to dto.erikoistuvanPuhelinnumero
        )
        dto.koulutuspaikat?.forEach { fields.add("koulutuspaikan-nimi" to it.nimi) }
        dto.kouluttajat?.forEach { fields.addAll(osoitekentat(it)) }
        validate(dto, fields)
    }

    fun validateKouluttajanKentat(dto: KoejaksonKoulutussopimusDTO, kouluttajaId: Long?, userId: String) {
        val osoite = dto.kouluttajat?.firstOrNull { it.id == kouluttajaId }
        val yhteystiedot = dto.kouluttajat?.firstOrNull { it.kayttajaUserId == userId }
        validate(dto, osoitekentat(osoite) + listOf(
            "nimike" to yhteystiedot?.nimike,
            "sahkoposti" to yhteystiedot?.sahkoposti,
            "puhelinnumero" to yhteystiedot?.puhelin,
            "korjausehdotus" to dto.korjausehdotus
        ))
    }

    fun validateVastuuhenkilonKentat(dto: KoejaksonKoulutussopimusDTO) {
        validate(dto, listOf(
            "sahkoposti" to dto.vastuuhenkilo?.sahkoposti,
            "puhelinnumero" to dto.vastuuhenkilo?.puhelin,
            "korjausehdotus" to dto.korjausehdotus
        ))
    }

    private fun osoitekentat(kouluttaja: KoulutussopimuksenKouluttajaDTO?) = listOf(
        "toimipaikka" to kouluttaja?.toimipaikka,
        "lahiosoite" to kouluttaja?.lahiosoite,
        "postitoimipaikka" to kouluttaja?.postitoimipaikka
    )

    private fun validate(dto: KoejaksonKoulutussopimusDTO, fields: List<Pair<String, String?>>) {
        validator.validate(fields = fields, pdfSource = "koejaksonkoulutussopimus", sourceId = dto.id)
    }
}
