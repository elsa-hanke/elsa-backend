package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.Kunta
import fi.elsapalvelu.elsa.domain.Tyoskentelypaikka
import fi.elsapalvelu.elsa.domain.enumeration.TyoskentelyjaksoTyyppi
import fi.elsapalvelu.elsa.web.rest.findAll
import javax.persistence.EntityManager

class TyoskentelypaikkaHelper {

    companion object {

        private const val DEFAULT_NIMI = "AAAAAAAAAA"
        private const val UPDATED_NIMI = "BBBBBBBBBB"

        private val DEFAULT_TYYPPI: TyoskentelyjaksoTyyppi = TyoskentelyjaksoTyyppi.TERVEYSKESKUS
        private val UPDATED_TYYPPI: TyoskentelyjaksoTyyppi = TyoskentelyjaksoTyyppi.MUU
        private const val UPDATED_MUU_TYYPPI = "CCCCCCCCCC"

        @JvmStatic
        fun createEntity(em: EntityManager): Tyoskentelypaikka {
            val tyoskentelypaikka = Tyoskentelypaikka(
                nimi = DEFAULT_NIMI,
                tyyppi = DEFAULT_TYYPPI,
                muuTyyppi = null
            )

            // Lisätään pakollinen tieto
            val kunta: Kunta
            if (em.findAll(Kunta::class).isEmpty()) {
                kunta = KuntaHelper.createEntity()
                em.persist(kunta)
                em.flush()
            } else {
                kunta = em.findAll(Kunta::class).get(0)
            }
            tyoskentelypaikka.kunta = kunta

            return tyoskentelypaikka
        }

        @JvmStatic
        fun createUpdatedEntity(em: EntityManager): Tyoskentelypaikka {
            val tyoskentelypaikka = Tyoskentelypaikka(
                nimi = UPDATED_NIMI,
                tyyppi = UPDATED_TYYPPI,
                muuTyyppi = UPDATED_MUU_TYYPPI
            )

            // Lisätään pakollinen tieto
            val kunta: Kunta
            if (em.findAll(Kunta::class).isEmpty()) {
                kunta = KuntaHelper.createUpdatedEntity()
                em.persist(kunta)
                em.flush()
            } else {
                kunta = em.findAll(Kunta::class).get(0)
            }
            tyoskentelypaikka.kunta = kunta

            return tyoskentelypaikka
        }
    }
}
