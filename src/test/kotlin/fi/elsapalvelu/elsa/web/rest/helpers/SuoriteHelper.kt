package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.Erikoisala
import fi.elsapalvelu.elsa.domain.Suorite
import fi.elsapalvelu.elsa.domain.SuoritteenKategoria
import fi.elsapalvelu.elsa.web.rest.findAll
import java.time.LocalDate
import java.time.ZoneId
import javax.persistence.EntityManager

class SuoriteHelper {

    companion object {

        private const val DEFAULT_NIMI = "AAAAAAAAAA"
        private const val UPDATED_NIMI = "BBBBBBBBBB"

        private val DEFAULT_VOIMASSAOLON_ALKAMISPAIVA: LocalDate = LocalDate.ofEpochDay(0L)
        private val UPDATED_VOIMASSAOLON_ALKAMISPAIVA: LocalDate = LocalDate.now(ZoneId.systemDefault())

        private val DEFAULT_VOIMASSAOLON_PAATTYMISPAIVA: LocalDate = LocalDate.ofEpochDay(30L)
        private val UPDATED_VOIMASSAOLON_PAATTYMISPAIVA: LocalDate = LocalDate.now(ZoneId.systemDefault())

        @JvmStatic
        fun createEntity(
            em: EntityManager,
            erikoisala: Erikoisala? = null,
            voimassaoloAlkaa: LocalDate? = DEFAULT_VOIMASSAOLON_ALKAMISPAIVA,
            voimassaoloPaattyy: LocalDate? = DEFAULT_VOIMASSAOLON_PAATTYMISPAIVA,
            existingKategoria: SuoritteenKategoria? = null,
            vaadittuLkm: Int? = null
        ): Suorite {
            val suorite = Suorite(
                nimi = DEFAULT_NIMI,
                voimassaolonAlkamispaiva = voimassaoloAlkaa,
                voimassaolonPaattymispaiva = voimassaoloPaattyy,
                vaadittulkm = vaadittuLkm
            )

            // Lisätään pakollinen tieto
            var suoritteenKategoria = existingKategoria
            if (suoritteenKategoria == null) {
                if (em.findAll(SuoritteenKategoria::class).isEmpty()) {
                    suoritteenKategoria = SuoritteenKategoriaHelper.createEntity(em, erikoisala)
                    em.persist(suoritteenKategoria)
                    em.flush()
                } else {
                    suoritteenKategoria = em.findAll(SuoritteenKategoria::class).get(0)
                }
            }
            suorite.kategoria = suoritteenKategoria

            return suorite
        }

        @JvmStatic
        fun createUpdatedEntity(em: EntityManager): Suorite {
            val suorite = Suorite(
                nimi = UPDATED_NIMI,
                voimassaolonAlkamispaiva = UPDATED_VOIMASSAOLON_ALKAMISPAIVA,
                voimassaolonPaattymispaiva = UPDATED_VOIMASSAOLON_PAATTYMISPAIVA
            )

            // Lisätään pakollinen tieto
            val suoritteenKategoria: SuoritteenKategoria
            if (em.findAll(SuoritteenKategoria::class).isEmpty()) {
                suoritteenKategoria = SuoritteenKategoriaHelper.createUpdatedEntity(em)
                em.persist(suoritteenKategoria)
                em.flush()
            } else {
                suoritteenKategoria = em.findAll(SuoritteenKategoria::class).get(0)
            }
            suorite.kategoria = suoritteenKategoria
            return suorite
        }
    }
}
