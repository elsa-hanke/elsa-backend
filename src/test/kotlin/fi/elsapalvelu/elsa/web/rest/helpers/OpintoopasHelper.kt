package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.Arviointiasteikko
import fi.elsapalvelu.elsa.domain.Opintoopas
import fi.elsapalvelu.elsa.web.rest.findAll
import java.time.LocalDate
import java.time.ZoneId
import javax.persistence.EntityManager

class OpintoopasHelper {

    companion object {

        private const val DEFAULT_NIMI = "AAAAAAAAAA"
        private const val UPDATED_NIMI = "BBBBBBBBBB"

        private val DEFAULT_VOIMASSAOLO_ALKAA: LocalDate = LocalDate.ofEpochDay(0L)
        private val UPDATED_VOIMASSAOLO_ALKAA: LocalDate = LocalDate.now(ZoneId.systemDefault())

        private val DEFAULT_VOIMASSAOLO_PAATTYY: LocalDate = LocalDate.ofEpochDay(30L)
        private val UPDATED_VOIMASSAOLO_PAATTYY: LocalDate = LocalDate.now(ZoneId.systemDefault())

        private const val DEFAULT_KAYTANNON_KOULUTUKSEN_VAHIMMAISPITUUS: Double = 1.0
        private const val UPDATED_KAYTANNON_KOULUTUKSEN_VAHIMMAISPITUUS: Double = 2.0

        private const val DEFAULT_TERVEYSKESKUSKOULUTUSJAKSON_VAHIMMAISPITUUS: Double = 1.0
        private const val UPDATED_TERVEYSKESKUSKOULUTUSJAKSON_VAHIMMAISPITUUS: Double = 2.0

        private const val DEFAULT_YLIOPISTOSAIRAALAJAKSON_VAHIMMAISPITUUS: Double = 1.0
        private const val UPDATED_YLIOPISTOSAIRAALAJAKSON_VAHIMMAISPITUUS: Double = 2.0

        private const val DEFAULT_YLIOPISTOSAIRAALAN_ULKOPUOLISEN_TYOSKENTELYN_VAHIMMAISPITUUS: Double = 1.0
        private const val UPDATED_YLIOPISTOSAIRAALAN_ULKOPUOLISEN_TYOSKENTELYN_VAHIMMAISPITUUS: Double = 2.0

        const val DEFAULT_ERIKOISALAN_VAATIMA_TEORIAKOULUTUSTEN_VAHIMMAISMAARA: Double = 60.0
        const val UPDATED_ERIKOISALAN_VAATIMA_TEORIAKOULUTUSTEN_VAHIMMAISMAARA: Double = 120.0

        const val DEFAULT_ERIKOISALAN_VAATIMA_SATEILYSUOJAKOULUTUSTEN_VAHIMMAISMAARA: Double = 0.0
        const val UPDATED_ERIKOISALAN_VAATIMA_SATEILYSUOJAKOULUTUSTEN_VAHIMMAISMAARA: Double = 3.0

        const val DEFAULT_ERIKOISALAN_VAATIMA_JOHTAMISOPINTOJEN_VAHIMMAISMAARA: Double = 10.0
        const val UPDATED_ERIKOISALAN_VAATIMA_JOHTAMISOPINTOJEN_VAHIMMAISMAARA: Double = 15.0

        @JvmStatic
        fun createEntity(
            em: EntityManager,
            voimassaoloAlkaa: LocalDate? = DEFAULT_VOIMASSAOLO_ALKAA,
            voimassaoloPaattyy: LocalDate? = DEFAULT_VOIMASSAOLO_PAATTYY
        ): Opintoopas {
            val arviointiasteikko: Arviointiasteikko
            if (em.findAll(Arviointiasteikko::class).isEmpty()) {
                arviointiasteikko = ArviointiasteikkoHelper.createEntity()
                em.persist(arviointiasteikko)
                em.flush()
            } else {
                arviointiasteikko = em.findAll(Arviointiasteikko::class).get(0)
            }

            val opintoopas = Opintoopas(
                nimi = DEFAULT_NIMI,
                voimassaoloAlkaa = voimassaoloAlkaa,
                voimassaoloPaattyy = voimassaoloPaattyy,
                kaytannonKoulutuksenVahimmaispituus = DEFAULT_KAYTANNON_KOULUTUKSEN_VAHIMMAISPITUUS,
                terveyskeskuskoulutusjaksonVahimmaispituus = DEFAULT_TERVEYSKESKUSKOULUTUSJAKSON_VAHIMMAISPITUUS,
                yliopistosairaalajaksonVahimmaispituus = DEFAULT_YLIOPISTOSAIRAALAJAKSON_VAHIMMAISPITUUS,
                yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituus =
                DEFAULT_YLIOPISTOSAIRAALAN_ULKOPUOLISEN_TYOSKENTELYN_VAHIMMAISPITUUS,
                erikoisalanVaatimaTeoriakoulutustenVahimmaismaara =
                DEFAULT_ERIKOISALAN_VAATIMA_TEORIAKOULUTUSTEN_VAHIMMAISMAARA,
                erikoisalanVaatimaSateilysuojakoulutustenVahimmaismaara =
                DEFAULT_ERIKOISALAN_VAATIMA_SATEILYSUOJAKOULUTUSTEN_VAHIMMAISMAARA,
                erikoisalanVaatimaJohtamisopintojenVahimmaismaara =
                DEFAULT_ERIKOISALAN_VAATIMA_JOHTAMISOPINTOJEN_VAHIMMAISMAARA,
                arviointiasteikko = arviointiasteikko
            )

            return opintoopas
        }

        @JvmStatic
        fun createUpdatedEntity(): Opintoopas {
            return Opintoopas(
                nimi = UPDATED_NIMI,
                voimassaoloAlkaa = UPDATED_VOIMASSAOLO_ALKAA,
                voimassaoloPaattyy = UPDATED_VOIMASSAOLO_PAATTYY,
                kaytannonKoulutuksenVahimmaispituus = UPDATED_KAYTANNON_KOULUTUKSEN_VAHIMMAISPITUUS,
                terveyskeskuskoulutusjaksonVahimmaispituus = UPDATED_TERVEYSKESKUSKOULUTUSJAKSON_VAHIMMAISPITUUS,
                yliopistosairaalajaksonVahimmaispituus = UPDATED_YLIOPISTOSAIRAALAJAKSON_VAHIMMAISPITUUS,
                yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituus =
                UPDATED_YLIOPISTOSAIRAALAN_ULKOPUOLISEN_TYOSKENTELYN_VAHIMMAISPITUUS,
                erikoisalanVaatimaTeoriakoulutustenVahimmaismaara =
                UPDATED_ERIKOISALAN_VAATIMA_TEORIAKOULUTUSTEN_VAHIMMAISMAARA,
                erikoisalanVaatimaSateilysuojakoulutustenVahimmaismaara =
                UPDATED_ERIKOISALAN_VAATIMA_SATEILYSUOJAKOULUTUSTEN_VAHIMMAISMAARA,
                erikoisalanVaatimaJohtamisopintojenVahimmaismaara =
                UPDATED_ERIKOISALAN_VAATIMA_JOHTAMISOPINTOJEN_VAHIMMAISMAARA
            )
        }
    }

}
