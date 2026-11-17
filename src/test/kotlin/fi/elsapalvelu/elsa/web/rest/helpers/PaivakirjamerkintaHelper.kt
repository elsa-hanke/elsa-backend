package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.seuranta.Paivakirjamerkinta
import fi.elsapalvelu.elsa.domain.kayttaja.User
import java.time.LocalDate
import java.time.ZoneId
import jakarta.persistence.EntityManager

object PaivakirjamerkintaHelper {

    private val DEFAULT_PAIVAMAARA: LocalDate = LocalDate.ofEpochDay(0L)
    private val UPDATED_PAIVAMAARA: LocalDate =
        LocalDate.now(ZoneId.systemDefault())

    private const val DEFAULT_OPPIMISTAPAHTUMAN_NIMI = "AAAAAAAAAA"
    private const val UPDATED_OPPIMISTAPAHTUMAN_NIMI = "BBBBBBBBBB"

    private const val DEFAULT_MUUN_AIHEEN_NIMI = "AAAAAAAAAA"
    private const val UPDATED_MUUN_AIHEEN_NIMI = "BBBBBBBBBB"

    private const val DEFAULT_REFLEKTIO = "AAAAAAAAAA"
    private const val UPDATED_REFLEKTIO = "BBBBBBBBBB"

    private const val DEFAULT_YKSITYINEN = false
    private const val UPDATED_YKSITYINEN = true

    fun createEntity(
        em: EntityManager,
        user: User? = null,
        opintooikeus: Opintooikeus = Testisuhteet.opintooikeus(em, user)
    ): Paivakirjamerkinta {
        val paivakirjamerkinta = Paivakirjamerkinta(
            paivamaara = DEFAULT_PAIVAMAARA,
            oppimistapahtumanNimi = DEFAULT_OPPIMISTAPAHTUMAN_NIMI,
            muunAiheenNimi = DEFAULT_MUUN_AIHEEN_NIMI,
            reflektio = DEFAULT_REFLEKTIO,
            yksityinen = DEFAULT_YKSITYINEN
        )

        paivakirjamerkinta.opintooikeus = opintooikeus

        return paivakirjamerkinta
    }

    fun createUpdatedEntity(opintooikeus: Opintooikeus): Paivakirjamerkinta {
        val paivakirjamerkinta = Paivakirjamerkinta(
            paivamaara = UPDATED_PAIVAMAARA,
            oppimistapahtumanNimi = UPDATED_OPPIMISTAPAHTUMAN_NIMI,
            muunAiheenNimi = UPDATED_MUUN_AIHEEN_NIMI,
            reflektio = UPDATED_REFLEKTIO,
            yksityinen = UPDATED_YKSITYINEN
        )

        paivakirjamerkinta.opintooikeus = opintooikeus

        return paivakirjamerkinta
    }
}
