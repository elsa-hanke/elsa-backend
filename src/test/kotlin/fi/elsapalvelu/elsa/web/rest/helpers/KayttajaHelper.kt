package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.web.rest.KayttajaResourceIT
import fi.elsapalvelu.elsa.web.rest.createByteArray
import javax.persistence.EntityManager

class KayttajaHelper {

    companion object {

        const val DEFAULT_ID = "c47f46ad-21c4-47e8-9c7c-ba44f60c8bae"
        const val DEFAULT_LOGIN = "johndoe"
        const val DEFAULT_EMAIL = "john.doe@example.com"

        private const val DEFAULT_NIMI = "AAAAAAAAAA"
        private const val UPDATED_NIMI = "BBBBBBBBBB"

        private val DEFAULT_PROFIILIKUVA: ByteArray = createByteArray(1, "0")
        private val UPDATED_PROFIILIKUVA: ByteArray = createByteArray(1, "1")
        private const val DEFAULT_PROFIILIKUVA_CONTENT_TYPE: String = "image/jpg"
        private const val UPDATED_PROFIILIKUVA_CONTENT_TYPE: String = "image/png"

        @JvmStatic
        fun createEntity(em: EntityManager, user: User? = null): Kayttaja {
            val kayttaja = Kayttaja(
                profiilikuva = DEFAULT_PROFIILIKUVA,
                profiilikuvaContentType = DEFAULT_PROFIILIKUVA_CONTENT_TYPE
            )

            // Lisätään pakollinen tieto
            if (user == null) {
                val newUser = KayttajaResourceIT.createEntity()
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
            nimi: String? = UPDATED_NIMI
        ): Kayttaja {
            val kayttaja = Kayttaja(
                profiilikuva = UPDATED_PROFIILIKUVA,
                profiilikuvaContentType = UPDATED_PROFIILIKUVA_CONTENT_TYPE
            )

            // Lisätään pakollinen tieto
            val user = KayttajaResourceIT.createEntity(nimi)
            em.persist(user)
            em.flush()
            kayttaja.user = user

            return kayttaja
        }
    }
}
