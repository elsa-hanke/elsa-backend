package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.Kayttaja
import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.web.rest.KayttajaResourceWithMockUserIT
import javax.persistence.EntityManager

class KayttajaHelper {

    companion object {

        const val DEFAULT_ID = "c47f46ad-21c4-47e8-9c7c-ba44f60c8bae"
        const val DEFAULT_LOGIN = "johndoe"
        const val DEFAULT_EMAIL = "john.doe@example.com"

        private const val DEFAULT_NIMI = "AAAAAAAAAA"
        private const val UPDATED_NIMI = "BBBBBBBBBB"

        private const val DEFAULT_NIMIKE = "DEFAULT_NIMIKE"

        @JvmStatic
        fun createEntity(em: EntityManager, user: User? = null): Kayttaja {
            val kayttaja = Kayttaja(
                nimike = DEFAULT_NIMIKE
            )

            // Lisätään pakollinen tieto
            if (user == null) {
                val newUser = KayttajaResourceWithMockUserIT.createEntity()
                em.persist(newUser)
                em.flush()
                kayttaja.user = newUser
            } else {
                kayttaja.user = user
            }

            return kayttaja
        }

        @JvmStatic
        fun createUpdatedEntity(
            em: EntityManager,
            nimi: String = UPDATED_NIMI
        ): Kayttaja {
            val kayttaja = Kayttaja()

            // Lisätään pakollinen tieto
            val user = KayttajaResourceWithMockUserIT.createEntity(nimi)
            em.persist(user)
            em.flush()
            kayttaja.user = user

            return kayttaja
        }
    }
}
