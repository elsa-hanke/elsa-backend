package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.Paivakirjamerkinta
import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.web.rest.findAll
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
        user: User? = null
    ): Paivakirjamerkinta {
        val paivakirjamerkinta = Paivakirjamerkinta(
            paivamaara = DEFAULT_PAIVAMAARA,
            oppimistapahtumanNimi = DEFAULT_OPPIMISTAPAHTUMAN_NIMI,
            muunAiheenNimi = DEFAULT_MUUN_AIHEEN_NIMI,
            reflektio = DEFAULT_REFLEKTIO,
            yksityinen = DEFAULT_YKSITYINEN
        )

        val erikoistuvaLaakari =
            em.findAll(ErikoistuvaLaakari::class)
                .firstOrNull { it.kayttaja?.user == user }
                ?: ErikoistuvaLaakariHelper.createEntity(em, user).also {
                    em.persist(it)
                    em.flush()
                }

        paivakirjamerkinta.opintooikeus =
            erikoistuvaLaakari.getOpintooikeusKaytossa()

        return paivakirjamerkinta
    }

    fun createUpdatedEntity(em: EntityManager): Paivakirjamerkinta {
        val paivakirjamerkinta = Paivakirjamerkinta(
            paivamaara = UPDATED_PAIVAMAARA,
            oppimistapahtumanNimi = UPDATED_OPPIMISTAPAHTUMAN_NIMI,
            muunAiheenNimi = UPDATED_MUUN_AIHEEN_NIMI,
            reflektio = UPDATED_REFLEKTIO,
            yksityinen = UPDATED_YKSITYINEN
        )

        val existing = em.findAll(ErikoistuvaLaakari::class)

        val erikoistuvaLaakari =
            if (existing.isEmpty()) {
                ErikoistuvaLaakariHelper.createUpdatedEntity(em).also {
                    em.persist(it)
                    em.flush()
                }
            } else {
                existing.first()
            }

        paivakirjamerkinta.opintooikeus =
            erikoistuvaLaakari.getOpintooikeusKaytossa()

        return paivakirjamerkinta
    }
}
