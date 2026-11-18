package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.arviointi.ArvioitavanKokonaisuudenKategoria
import fi.elsapalvelu.elsa.domain.perustiedot.Erikoisala
import jakarta.persistence.EntityManager

object ArvioitavanKokonaisuudenKategoriaHelper {
    private const val DEFAULT_NIMI = "AAAAAAAAAA"
    private const val UPDATED_NIMI = "BBBBBBBBBB"
    private const val DEFAULT_JARJESTYSNUMERO: Int = 1
    private const val UPDATED_JARJESTYSNUMERO: Int = 2

    @JvmStatic
    fun createEntity(
        em: EntityManager,
        erikoisala: Erikoisala? = null
    ): ArvioitavanKokonaisuudenKategoria {
        val arvioitavanKokonaisuudenKategoria = ArvioitavanKokonaisuudenKategoria(
            nimi = DEFAULT_NIMI,
            jarjestysnumero = DEFAULT_JARJESTYSNUMERO
        )

        arvioitavanKokonaisuudenKategoria.erikoisala = erikoisala ?: ErikoisalaHelper.createEntity().also(em::persist)

        return arvioitavanKokonaisuudenKategoria
    }

    @JvmStatic
    fun createUpdatedEntity(em: EntityManager, erikoisala: Erikoisala? = null): ArvioitavanKokonaisuudenKategoria {
        val kategoria = ArvioitavanKokonaisuudenKategoria(
            nimi = UPDATED_NIMI,
            jarjestysnumero = UPDATED_JARJESTYSNUMERO
        )

        kategoria.erikoisala = erikoisala ?: ErikoisalaHelper.createUpdatedEntity().also(em::persist)

        return kategoria
    }
}
