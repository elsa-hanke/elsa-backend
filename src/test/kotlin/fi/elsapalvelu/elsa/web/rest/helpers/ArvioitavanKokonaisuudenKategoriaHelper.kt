package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.ArvioitavanKokonaisuudenKategoria
import fi.elsapalvelu.elsa.domain.Erikoisala
import fi.elsapalvelu.elsa.web.rest.findAll
import java.time.LocalDate
import java.time.ZoneId
import javax.persistence.EntityManager

class ArvioitavanKokonaisuudenKategoriaHelper {

    companion object {

        private const val DEFAULT_NIMI = "AAAAAAAAAA"
        private const val UPDATED_NIMI = "BBBBBBBBBB"

        private const val DEFAULT_JARJESTYSNUMERO: Int = 1
        private const val UPDATED_JARJESTYSNUMERO: Int = 2

        private val DEFAULT_VOIMASSAOLO_ALKAA: LocalDate = LocalDate.ofEpochDay(0L)
        private val UPDATED_VOIMASSAOLO_ALKAA: LocalDate = LocalDate.now(ZoneId.systemDefault())

        private val DEFAULT_VOIMASSAOLO_LOPPUU: LocalDate = LocalDate.ofEpochDay(30L)
        private val UPDATED_VOIMASSAOLO_LOPPUU: LocalDate = LocalDate.now(ZoneId.systemDefault())

        @JvmStatic
        fun createEntity(em: EntityManager): ArvioitavanKokonaisuudenKategoria {
            val kategoria = ArvioitavanKokonaisuudenKategoria(
                nimi = DEFAULT_NIMI,
                jarjestysnumero = DEFAULT_JARJESTYSNUMERO,
                voimassaoloAlkaa = DEFAULT_VOIMASSAOLO_ALKAA,
                voimassaoloLoppuu = DEFAULT_VOIMASSAOLO_LOPPUU
            )

            // Lisätään pakollinen tieto
            val erikoisala: Erikoisala
            if (em.findAll(Erikoisala::class).isEmpty()) {
                erikoisala = ErikoisalaHelper.createEntity()
                em.persist(erikoisala)
                em.flush()
            } else {
                erikoisala = em.findAll(Erikoisala::class)[0]
            }
            kategoria.erikoisala = erikoisala

            return kategoria
        }

        @JvmStatic
        fun createUpdatedEntity(em: EntityManager): ArvioitavanKokonaisuudenKategoria {
            val kategoria = ArvioitavanKokonaisuudenKategoria(
                nimi = UPDATED_NIMI,
                jarjestysnumero = UPDATED_JARJESTYSNUMERO,
                voimassaoloAlkaa = UPDATED_VOIMASSAOLO_ALKAA,
                voimassaoloLoppuu = UPDATED_VOIMASSAOLO_LOPPUU
            )

            // Lisätään pakollinen tieto
            val erikoisala: Erikoisala
            if (em.findAll(Erikoisala::class).isEmpty()) {
                erikoisala = ErikoisalaHelper.createUpdatedEntity()
                em.persist(erikoisala)
                em.flush()
            } else {
                erikoisala = em.findAll(Erikoisala::class)[0]
            }
            kategoria.erikoisala = erikoisala

            return kategoria
        }
    }
}
