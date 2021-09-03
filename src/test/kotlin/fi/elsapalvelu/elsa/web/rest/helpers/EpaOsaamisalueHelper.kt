package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.EpaOsaamisalue
import fi.elsapalvelu.elsa.domain.EpaOsaamisalueenKategoria
import fi.elsapalvelu.elsa.domain.Erikoisala
import fi.elsapalvelu.elsa.web.rest.findAll
import java.time.LocalDate
import java.time.ZoneId
import javax.persistence.EntityManager

class EpaOsaamisalueHelper {

    companion object {

        private const val DEFAULT_NIMI = "AAAAAAAAAA"
        private const val UPDATED_NIMI = "BBBBBBBBBB"

        private const val DEFAULT_KUVAUS = "AAAAAAAAAA"
        private const val UPDATED_KUVAUS = "BBBBBBBBBB"

        private val DEFAULT_VOIMASSAOLO_ALKAA: LocalDate = LocalDate.ofEpochDay(0L)
        private val UPDATED_VOIMASSAOLO_ALKAA: LocalDate = LocalDate.now(ZoneId.systemDefault())

        private val DEFAULT_VOIMASSAOLO_LOPPUU: LocalDate = LocalDate.ofEpochDay(0L)
        private val UPDATED_VOIMASSAOLO_LOPPUU: LocalDate = LocalDate.now(ZoneId.systemDefault())

        @JvmStatic
        fun createEntity(em: EntityManager,
                         voimassaoloAlkaa: LocalDate? = DEFAULT_VOIMASSAOLO_ALKAA,
                         voimassaoloLoppuu: LocalDate? = DEFAULT_VOIMASSAOLO_LOPPUU): EpaOsaamisalue {
            val epaOsaamisalue = EpaOsaamisalue(
                nimi = DEFAULT_NIMI,
                kuvaus = DEFAULT_KUVAUS,
                voimassaoloAlkaa = voimassaoloAlkaa,
                voimassaoloLoppuu = voimassaoloLoppuu
            )

            // Lisätään pakollinen tieto
            val erikoisala: Erikoisala
            if (em.findAll(Erikoisala::class).isEmpty()) {
                erikoisala = ErikoisalaHelper.createEntity()
                em.persist(erikoisala)
                em.flush()
            } else {
                erikoisala = em.findAll(Erikoisala::class).get(0)
            }
            epaOsaamisalue.erikoisala = erikoisala

            // Lisätään pakollinen tieto
            val epaOsaamisalueenKategoria: EpaOsaamisalueenKategoria
            if (em.findAll(EpaOsaamisalueenKategoria::class).isEmpty()) {
                epaOsaamisalueenKategoria = EpaOsaamisalueenKategoriaHelper.createEntity()
                em.persist(epaOsaamisalueenKategoria)
                em.flush()
            } else {
                epaOsaamisalueenKategoria = em.findAll(EpaOsaamisalueenKategoria::class).get(0)
            }
            epaOsaamisalue.kategoria = epaOsaamisalueenKategoria

            return epaOsaamisalue
        }

        @JvmStatic
        fun createUpdatedEntity(em: EntityManager): EpaOsaamisalue {
            val epaOsaamisalue = EpaOsaamisalue(
                nimi = UPDATED_NIMI,
                kuvaus = UPDATED_KUVAUS,
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
                erikoisala = em.findAll(Erikoisala::class).get(0)
            }
            epaOsaamisalue.erikoisala = erikoisala

            // Lisätään pakollinen tieto
            val epaOsaamisalueenKategoria: EpaOsaamisalueenKategoria
            if (em.findAll(EpaOsaamisalueenKategoria::class).isEmpty()) {
                epaOsaamisalueenKategoria = EpaOsaamisalueenKategoriaHelper.createUpdatedEntity()
                em.persist(epaOsaamisalueenKategoria)
                em.flush()
            } else {
                epaOsaamisalueenKategoria = em.findAll(EpaOsaamisalueenKategoria::class).get(0)
            }
            epaOsaamisalue.kategoria = epaOsaamisalueenKategoria

            return epaOsaamisalue
        }
    }
}
