package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.ArvioitavaKokonaisuus
import fi.elsapalvelu.elsa.domain.ArvioitavanKokonaisuudenKategoria
import fi.elsapalvelu.elsa.web.rest.findAll
import java.time.LocalDate
import java.time.ZoneId
import javax.persistence.EntityManager

class ArvioitavaKokonaisuusHelper {

    companion object {

        private const val DEFAULT_NIMI = "AAAAAAAAAA"
        private const val UPDATED_NIMI = "BBBBBBBBBB"

        private const val DEFAULT_KUVAUS = "AAAAAAAAAA"
        private const val UPDATED_KUVAUS = "BBBBBBBBBB"

        private val DEFAULT_VOIMASSAOLO_ALKAA: LocalDate = LocalDate.ofEpochDay(0L)
        private val UPDATED_VOIMASSAOLO_ALKAA: LocalDate = LocalDate.now(ZoneId.systemDefault())

        private val DEFAULT_VOIMASSAOLO_LOPPUU: LocalDate = LocalDate.ofEpochDay(30L)
        private val UPDATED_VOIMASSAOLO_LOPPUU: LocalDate = LocalDate.now(ZoneId.systemDefault())

        @JvmStatic
        fun createEntity(
            em: EntityManager,
            voimassaoloAlkaa: LocalDate? = DEFAULT_VOIMASSAOLO_ALKAA,
            voimassaoloLoppuu: LocalDate? = DEFAULT_VOIMASSAOLO_LOPPUU,
            existingKategoria: ArvioitavanKokonaisuudenKategoria? = null
        ): ArvioitavaKokonaisuus {
            val arvioitavaKokonaisuus = ArvioitavaKokonaisuus(
                nimi = DEFAULT_NIMI,
                kuvaus = DEFAULT_KUVAUS,
                voimassaoloAlkaa = voimassaoloAlkaa,
                voimassaoloLoppuu = voimassaoloLoppuu
            )

            // Lisätään pakollinen tieto
            var arvioitavanKokonaisuudenKategoria = existingKategoria
            if (arvioitavanKokonaisuudenKategoria == null) {
                if (em.findAll(ArvioitavanKokonaisuudenKategoria::class).isEmpty()) {
                    arvioitavanKokonaisuudenKategoria =
                        ArvioitavanKokonaisuudenKategoriaHelper.createEntity(em)
                    em.persist(arvioitavanKokonaisuudenKategoria)
                    em.flush()
                } else {
                    arvioitavanKokonaisuudenKategoria =
                        em.findAll(ArvioitavanKokonaisuudenKategoria::class)[0]
                }
            }
            arvioitavaKokonaisuus.kategoria = arvioitavanKokonaisuudenKategoria

            return arvioitavaKokonaisuus
        }

        @JvmStatic
        fun createUpdatedEntity(em: EntityManager): ArvioitavaKokonaisuus {
            val arvioitavaKokonaisuus = ArvioitavaKokonaisuus(
                nimi = UPDATED_NIMI,
                kuvaus = UPDATED_KUVAUS,
                voimassaoloAlkaa = UPDATED_VOIMASSAOLO_ALKAA,
                voimassaoloLoppuu = UPDATED_VOIMASSAOLO_LOPPUU
            )

            // Lisätään pakollinen tieto
            val arvioitavanKokonaisuudenKategoria: ArvioitavanKokonaisuudenKategoria
            if (em.findAll(ArvioitavanKokonaisuudenKategoria::class).isEmpty()) {
                arvioitavanKokonaisuudenKategoria =
                    ArvioitavanKokonaisuudenKategoriaHelper.createUpdatedEntity(em)
                em.persist(arvioitavanKokonaisuudenKategoria)
                em.flush()
            } else {
                arvioitavanKokonaisuudenKategoria =
                    em.findAll(ArvioitavanKokonaisuudenKategoria::class)[0]
            }
            arvioitavaKokonaisuus.kategoria = arvioitavanKokonaisuudenKategoria

            return arvioitavaKokonaisuus
        }
    }
}
