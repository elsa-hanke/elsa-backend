package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.OpintosuoritusKurssikoodi
import fi.elsapalvelu.elsa.domain.OpintosuoritusTyyppi
import fi.elsapalvelu.elsa.domain.Yliopisto
import fi.elsapalvelu.elsa.domain.enumeration.OpintosuoritusTyyppiEnum
import fi.elsapalvelu.elsa.web.rest.findAll
import jakarta.persistence.EntityManager

class KurssikoodiHelper {

    companion object {

        private const val DEFAULT_TUNNISTE = "AAAAAAAAAA"
        private val DEFAULT_TYYPPI = OpintosuoritusTyyppiEnum.JOHTAMISOPINTO

        @JvmStatic
        fun createEntity(
            em: EntityManager,
            tunniste: String? = DEFAULT_TUNNISTE,
            tyyppi: OpintosuoritusTyyppiEnum? = DEFAULT_TYYPPI,
            yliopisto: Yliopisto?
        ): OpintosuoritusKurssikoodi {
            val opintosuoritusTyyppi =
                em.findAll(OpintosuoritusTyyppi::class).first { it.nimi == tyyppi }
            val kurssikoodi = OpintosuoritusKurssikoodi(
                tunniste = tunniste,
                tyyppi = opintosuoritusTyyppi,
                isOsakokonaisuus = false,
                yliopisto = yliopisto ?: em.findAll(Yliopisto::class).first()
            )
            em.persist(kurssikoodi)
            em.flush()

            return kurssikoodi
        }

    }
}
