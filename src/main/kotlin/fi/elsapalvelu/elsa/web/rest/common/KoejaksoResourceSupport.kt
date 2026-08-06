package fi.elsapalvelu.elsa.web.rest.common

import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import org.springframework.stereotype.Component
import java.util.Optional

@Component
class KoejaksoResourceSupport {

    fun <T> findByLahikouluttajaOrLahiesimies(
        id: Long,
        userId: String,
        findByLahikouluttaja: (Long, String) -> Optional<T>,
        findByLahiesimies: (Long, String) -> Optional<T>
    ): Optional<T> {
        val lahikouluttajaResult = findByLahikouluttaja(id, userId)
        return if (lahikouluttajaResult.isPresent) {
            lahikouluttajaResult
        } else {
            findByLahiesimies(id, userId)
        }
    }

    fun <T> findForUpdateByLahikouluttajaOrLahiesimies(
        id: Long,
        userId: String,
        entity: String,
        notFoundMessage: String,
        notFoundErrorKey: String,
        lahiesimiesBeforeLahikouluttajaMessage: String,
        lahiesimiesBeforeLahikouluttajaErrorKey: String,
        findByLahikouluttaja: (Long, String) -> Optional<T>,
        findByLahiesimies: (Long, String) -> Optional<T>,
        lahikouluttajaSopimusHyvaksytty: (T) -> Boolean?
    ): T {
        var result = findByLahikouluttaja(id, userId)

        if (!result.isPresent) {
            result = findByLahiesimies(id, userId)

            if (!result.isPresent) {
                throw BadRequestAlertException(notFoundMessage, entity, notFoundErrorKey)
            }

            if (lahikouluttajaSopimusHyvaksytty(result.get()) != true) {
                throw BadRequestAlertException(
                    lahiesimiesBeforeLahikouluttajaMessage,
                    entity,
                    lahiesimiesBeforeLahikouluttajaErrorKey
                )
            }
        }

        return result.get()
    }

    fun validateArviointi(
        hyvaksytty: Boolean?,
        entity: String
    ) {
        if (hyvaksytty == true) {
            throw BadRequestAlertException(
                "Hyväksyttyä arviointia ei saa muokata",
                entity,
                "dataillegal.hyvaksyttya-arviointia-ei-saa-muokata"
            )
        }
    }

    fun validateLahetetty(
        lahetetty: Boolean?,
        entity: String
    ) {
        if (lahetetty != true) {
            throw BadRequestAlertException(
                "Arviointia ei saa muokata, jos erikoistuva ei ole lähettänyt pyyntöä.",
                entity,
                "dataillegal.arviointia-ei-saa-muokata-jos-erikoistuva-ei-ole-lahettanyt-pyyntoa"
            )
        }
    }

    fun validateId(id: Long?, entity: String) {
        if (id == null) {
            throw BadRequestAlertException(
                "Virheellinen id",
                entity,
                "idnull"
            )
        }
    }
}
