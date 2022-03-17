package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.Paivakirjamerkinta
import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.web.rest.findAll
import java.time.LocalDate
import java.time.ZoneId
import javax.persistence.EntityManager

class PaivakirjamerkintaHelper {

    companion object {

        private val DEFAULT_PAIVAMAARA: LocalDate = LocalDate.ofEpochDay(0L)
        private val UPDATED_PAIVAMAARA: LocalDate = LocalDate.now(ZoneId.systemDefault())

        private const val DEFAULT_OPPIMISTAPAHTUMAN_NIMI = "AAAAAAAAAA"
        private const val UPDATED_OPPIMISTAPAHTUMAN_NIMI = "BBBBBBBBBB"

        private const val DEFAULT_MUUN_AIHEEN_NIMI = "AAAAAAAAAA"
        private const val UPDATED_MUUN_AIHEEN_NIMI = "BBBBBBBBBB"

        private const val DEFAULT_REFLEKTIO = "AAAAAAAAAA"
        private const val UPDATED_REFLEKTIO = "BBBBBBBBBB"

        private const val DEFAULT_YKSITYINEN: Boolean = false
        private const val UPDATED_YKSITYINEN: Boolean = true

        @JvmStatic
        fun createEntity(em: EntityManager, user: User? = null): Paivakirjamerkinta {
            val paivakirjamerkinta = Paivakirjamerkinta(
                paivamaara = DEFAULT_PAIVAMAARA,
                oppimistapahtumanNimi = DEFAULT_OPPIMISTAPAHTUMAN_NIMI,
                muunAiheenNimi = DEFAULT_MUUN_AIHEEN_NIMI,
                reflektio = DEFAULT_REFLEKTIO,
                yksityinen = DEFAULT_YKSITYINEN
            )

            var erikoistuvaLaakari =
                em.findAll(ErikoistuvaLaakari::class).firstOrNull { it.kayttaja?.user == user }
            if (erikoistuvaLaakari == null) {
                erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, user)
                em.persist(erikoistuvaLaakari)
                em.flush()
            }
            paivakirjamerkinta.opintooikeus = erikoistuvaLaakari.getOpintooikeusKaytossa()

            return paivakirjamerkinta
        }

        @JvmStatic
        fun createUpdatedEntity(em: EntityManager): Paivakirjamerkinta {
            val paivakirjamerkinta = Paivakirjamerkinta(

                paivamaara = UPDATED_PAIVAMAARA,

                oppimistapahtumanNimi = UPDATED_OPPIMISTAPAHTUMAN_NIMI,

                muunAiheenNimi = UPDATED_MUUN_AIHEEN_NIMI,

                reflektio = UPDATED_REFLEKTIO,

                yksityinen = UPDATED_YKSITYINEN

            )

            val erikoistuvaLaakari: ErikoistuvaLaakari
            if (em.findAll(ErikoistuvaLaakari::class).isEmpty()) {
                erikoistuvaLaakari = ErikoistuvaLaakariHelper.createUpdatedEntity(em)
                em.persist(erikoistuvaLaakari)
                em.flush()
            } else {
                erikoistuvaLaakari = em.findAll(ErikoistuvaLaakari::class)[0]
            }
            paivakirjamerkinta.opintooikeus = erikoistuvaLaakari.getOpintooikeusKaytossa()

            return paivakirjamerkinta
        }
    }
}
