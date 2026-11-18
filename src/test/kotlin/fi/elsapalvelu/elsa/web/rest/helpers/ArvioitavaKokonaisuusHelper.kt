package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.arviointi.ArvioitavaKokonaisuus
import fi.elsapalvelu.elsa.domain.arviointi.ArvioitavanKokonaisuudenKategoria
import java.time.LocalDate
import java.time.ZoneId
import jakarta.persistence.EntityManager

object ArvioitavaKokonaisuusHelper {
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
        kategoria: ArvioitavanKokonaisuudenKategoria? = null
    ): ArvioitavaKokonaisuus {
        val arvioitavaKokonaisuus = ArvioitavaKokonaisuus(
            nimi = DEFAULT_NIMI,
            kuvaus = DEFAULT_KUVAUS,
            voimassaoloAlkaa = voimassaoloAlkaa,
            voimassaoloLoppuu = voimassaoloLoppuu
        )

        arvioitavaKokonaisuus.kategoria = kategoria ?: ArvioitavanKokonaisuudenKategoriaHelper.createEntity(em).also(em::persist)

        return arvioitavaKokonaisuus
    }

    @JvmStatic
    fun createUpdatedEntity(em: EntityManager, kategoria: ArvioitavanKokonaisuudenKategoria? = null): ArvioitavaKokonaisuus {
        val arvioitavaKokonaisuus = ArvioitavaKokonaisuus(
            nimi = UPDATED_NIMI,
            kuvaus = UPDATED_KUVAUS,
            voimassaoloAlkaa = UPDATED_VOIMASSAOLO_ALKAA,
            voimassaoloLoppuu = UPDATED_VOIMASSAOLO_LOPPUU
        )

        arvioitavaKokonaisuus.kategoria = kategoria ?: ArvioitavanKokonaisuudenKategoriaHelper.createUpdatedEntity(em).also(em::persist)

        return arvioitavaKokonaisuus
    }
}
