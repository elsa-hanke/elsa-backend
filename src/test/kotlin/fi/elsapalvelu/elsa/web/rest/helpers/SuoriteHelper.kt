package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.Erikoisala
import fi.elsapalvelu.elsa.domain.Suorite
import fi.elsapalvelu.elsa.domain.SuoritteenKategoria
import fi.elsapalvelu.elsa.web.rest.findAll
import java.time.LocalDate
import java.time.ZoneId
import jakarta.persistence.EntityManager

object SuoriteHelper {

    private const val DEFAULT_NIMI = "AAAAAAAAAA"
    private const val UPDATED_NIMI = "BBBBBBBBBB"

    private val DEFAULT_VOIMASSAOLON_ALKAMISPAIVA: LocalDate = LocalDate.ofEpochDay(0L)
    private val UPDATED_VOIMASSAOLON_ALKAMISPAIVA: LocalDate =
        LocalDate.now(ZoneId.systemDefault())

    private val DEFAULT_VOIMASSAOLON_PAATTYMISPAIVA: LocalDate =
        LocalDate.ofEpochDay(30L)

    private val UPDATED_VOIMASSAOLON_PAATTYMISPAIVA: LocalDate =
        LocalDate.now(ZoneId.systemDefault())

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

        val suoritteenKategoria = existingKategoria ?: run {
            if (em.findAll(SuoritteenKategoria::class).isEmpty()) {
                SuoritteenKategoriaHelper.createEntity(em, erikoisala).also {
                    em.persist(it)
                    em.flush()
                }
            } else {
                em.findAll(SuoritteenKategoria::class).first()
            }
        }

        suorite.kategoria = suoritteenKategoria

        return suorite
    }

    fun createUpdatedEntity(em: EntityManager): Suorite {
        val suorite = Suorite(
            nimi = UPDATED_NIMI,
            voimassaolonAlkamispaiva = UPDATED_VOIMASSAOLON_ALKAMISPAIVA,
            voimassaolonPaattymispaiva = UPDATED_VOIMASSAOLON_PAATTYMISPAIVA
        )

        val suoritteenKategoria =
            if (em.findAll(SuoritteenKategoria::class).isEmpty()) {
                SuoritteenKategoriaHelper.createUpdatedEntity(em).also {
                    em.persist(it)
                    em.flush()
                }
            } else {
                em.findAll(SuoritteenKategoria::class).first()
            }

        suorite.kategoria = suoritteenKategoria

        return suorite
    }
}
