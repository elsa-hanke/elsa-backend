package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.Kayttaja
import fi.elsapalvelu.elsa.web.rest.UserResourceIT
import fi.elsapalvelu.elsa.web.rest.createByteArray
import javax.persistence.EntityManager

class KayttajaHelper {

    companion object {

        private const val DEFAULT_NIMI = "AAAAAAAAAA"
        private const val UPDATED_NIMI = "BBBBBBBBBB"

        private val DEFAULT_PROFIILIKUVA: ByteArray = createByteArray(1, "0")
        private val UPDATED_PROFIILIKUVA: ByteArray = createByteArray(1, "1")
        private const val DEFAULT_PROFIILIKUVA_CONTENT_TYPE: String = "image/jpg"
        private const val UPDATED_PROFIILIKUVA_CONTENT_TYPE: String = "image/png"

        @JvmStatic
        fun createEntity(em: EntityManager, userId: String? = null): Kayttaja {
            val kayttaja = Kayttaja(
                nimi = DEFAULT_NIMI,
                profiilikuva = DEFAULT_PROFIILIKUVA,
                profiilikuvaContentType = DEFAULT_PROFIILIKUVA_CONTENT_TYPE
            )

            // Add required entity
            val user = UserResourceIT.createEntity(userId)
            em.persist(user)
            em.flush()
            kayttaja.user = user
            return kayttaja
        }

        @JvmStatic
        fun createUpdatedEntity(em: EntityManager): Kayttaja {
            val kayttaja = Kayttaja(
                nimi = UPDATED_NIMI,
                profiilikuva = UPDATED_PROFIILIKUVA,
                profiilikuvaContentType = UPDATED_PROFIILIKUVA_CONTENT_TYPE
            )

            // Add required entity
            val user = UserResourceIT.createEntity()
            em.persist(user)
            em.flush()
            kayttaja.user = user
            return kayttaja
        }
    }
}
