package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.koulutus.Teoriakoulutus
import fi.elsapalvelu.elsa.domain.kayttaja.User
import java.time.LocalDate
import java.time.ZoneId
import jakarta.persistence.EntityManager

object TeoriakoulutusHelper {

    private const val DEFAULT_KOULUTUKSEN_NIMI = "AAAAAAAAAA"
    private const val UPDATED_KOULUTUKSEN_NIMI = "BBBBBBBBBB"

    private const val DEFAULT_KOULUTUKSEN_PAIKKA = "AAAAAAAAAA"
    private const val UPDATED_KOULUTUKSEN_PAIKKA = "BBBBBBBBBB"

    private val DEFAULT_ALKAMISPAIVA: LocalDate =
        LocalDate.ofEpochDay(0L)

    private val UPDATED_ALKAMISPAIVA: LocalDate =
        LocalDate.now(ZoneId.systemDefault())

    private val DEFAULT_PAATTYMISPAIVA: LocalDate =
        LocalDate.ofEpochDay(0L)

    private val UPDATED_PAATTYMISPAIVA: LocalDate =
        LocalDate.now(ZoneId.systemDefault())

    private const val DEFAULT_ERIKOISTUMISEEN_HYVAKSYTTAVA_TUNTIMAARA =
        5.0

    private const val UPDATED_ERIKOISTUMISEEN_HYVAKSYTTAVA_TUNTIMAARA =
        10.0

    fun createEntity(
        em: EntityManager,
        user: User? = null,
        alkamispaiva: LocalDate = DEFAULT_ALKAMISPAIVA,
        paattymispaiva: LocalDate = DEFAULT_PAATTYMISPAIVA,
        erikoistumiseenHyvaksyttavaTuntimaara: Double? =
            DEFAULT_ERIKOISTUMISEEN_HYVAKSYTTAVA_TUNTIMAARA,
        opintooikeus: Opintooikeus = Testisuhteet.opintooikeus(em, user)
    ): Teoriakoulutus {
        val teoriakoulutus = Teoriakoulutus(
            koulutuksenNimi = DEFAULT_KOULUTUKSEN_NIMI,
            koulutuksenPaikka = DEFAULT_KOULUTUKSEN_PAIKKA,
            alkamispaiva = alkamispaiva,
            paattymispaiva = paattymispaiva,
            erikoistumiseenHyvaksyttavaTuntimaara =
                erikoistumiseenHyvaksyttavaTuntimaara
        )

        teoriakoulutus.opintooikeus = opintooikeus

        return teoriakoulutus
    }

    fun createUpdatedEntity(opintooikeus: Opintooikeus): Teoriakoulutus {
        val teoriakoulutus = Teoriakoulutus(
            koulutuksenNimi = UPDATED_KOULUTUKSEN_NIMI,
            koulutuksenPaikka = UPDATED_KOULUTUKSEN_PAIKKA,
            alkamispaiva = UPDATED_ALKAMISPAIVA,
            paattymispaiva = UPDATED_PAATTYMISPAIVA,
            erikoistumiseenHyvaksyttavaTuntimaara =
                UPDATED_ERIKOISTUMISEEN_HYVAKSYTTAVA_TUNTIMAARA
        )

        teoriakoulutus.opintooikeus = opintooikeus

        return teoriakoulutus
    }
}
