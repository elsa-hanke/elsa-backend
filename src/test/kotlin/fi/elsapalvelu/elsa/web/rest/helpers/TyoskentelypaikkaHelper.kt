package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.Kunta
import fi.elsapalvelu.elsa.domain.Tyoskentelypaikka
import fi.elsapalvelu.elsa.domain.enumeration.TyoskentelyjaksoTyyppi
import fi.elsapalvelu.elsa.web.rest.findAll
import jakarta.persistence.EntityManager

class TyoskentelypaikkaHelper {

    companion object {

        const val DEFAULT_NIMI = "AAAAAAAAAA"
        const val UPDATED_NIMI = "BBBBBBBBBB"

        val DEFAULT_TYYPPI: TyoskentelyjaksoTyyppi = TyoskentelyjaksoTyyppi.TERVEYSKESKUS
        val UPDATED_TYYPPI: TyoskentelyjaksoTyyppi = TyoskentelyjaksoTyyppi.MUU
        const val UPDATED_MUU_TYYPPI = "CCCCCCCCCC"

        @JvmStatic
        fun createEntity(
            em: EntityManager,
            tyyppi: TyoskentelyjaksoTyyppi = DEFAULT_TYYPPI
        ): Tyoskentelypaikka {
            val tyoskentelypaikka = Tyoskentelypaikka(
                nimi = DEFAULT_NIMI,
                tyyppi = tyyppi,
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
        fun createUpdatedEntity(
            em: EntityManager,
            tyyppi: TyoskentelyjaksoTyyppi = UPDATED_TYYPPI
        ): Tyoskentelypaikka {
            val tyoskentelypaikka = Tyoskentelypaikka(
                nimi = UPDATED_NIMI,
                tyyppi = tyyppi,
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
