package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.Teoriakoulutus
import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.web.rest.findAll
import java.time.LocalDate
import java.time.ZoneId
import javax.persistence.EntityManager

class TeoriakoulutusHelper {

    companion object {

        private const val DEFAULT_KOULUTUKSEN_NIMI = "AAAAAAAAAA"
        private const val UPDATED_KOULUTUKSEN_NIMI = "BBBBBBBBBB"

        private const val DEFAULT_KOULUTUKSEN_PAIKKA = "AAAAAAAAAA"
        private const val UPDATED_KOULUTUKSEN_PAIKKA = "BBBBBBBBBB"

        private val DEFAULT_ALKAMISPAIVA: LocalDate = LocalDate.ofEpochDay(0L)
        private val UPDATED_ALKAMISPAIVA: LocalDate = LocalDate.now(ZoneId.systemDefault())

        private val DEFAULT_PAATTYMISPAIVA: LocalDate = LocalDate.ofEpochDay(0L)
        private val UPDATED_PAATTYMISPAIVA: LocalDate = LocalDate.now(ZoneId.systemDefault())

        private const val DEFAULT_ERIKOISTUMISEEN_HYVAKSYTTAVA_TUNTIMAARA: Double = 5.0
        private const val UPDATED_ERIKOISTUMISEEN_HYVAKSYTTAVA_TUNTIMAARA: Double = 10.0

        @JvmStatic
        fun createEntity(
            em: EntityManager,
            user: User? = null,
            alkamispaiva: LocalDate = DEFAULT_ALKAMISPAIVA,
            paattymispaiva: LocalDate = DEFAULT_PAATTYMISPAIVA,
            erikoistumiseenHyvaksyttavaTuntimaara: Double? = DEFAULT_ERIKOISTUMISEEN_HYVAKSYTTAVA_TUNTIMAARA
        ): Teoriakoulutus {
            val teoriakoulutus = Teoriakoulutus(
                koulutuksenNimi = DEFAULT_KOULUTUKSEN_NIMI,
                koulutuksenPaikka = DEFAULT_KOULUTUKSEN_PAIKKA,
                alkamispaiva = alkamispaiva,
                paattymispaiva = paattymispaiva,
                erikoistumiseenHyvaksyttavaTuntimaara = erikoistumiseenHyvaksyttavaTuntimaara
            )

            // Lisätään pakollinen tieto
            var erikoistuvaLaakari =
                em.findAll(ErikoistuvaLaakari::class).firstOrNull { it.kayttaja?.user == user }
            if (erikoistuvaLaakari == null) {
                erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, user)
                em.persist(erikoistuvaLaakari)
                em.flush()
            }
            teoriakoulutus.opintooikeus = erikoistuvaLaakari.getOpintooikeusKaytossa()

            return teoriakoulutus
        }

        @JvmStatic
        fun createUpdatedEntity(em: EntityManager): Teoriakoulutus {
            val teoriakoulutus = Teoriakoulutus(
                koulutuksenNimi = UPDATED_KOULUTUKSEN_NIMI,
                koulutuksenPaikka = UPDATED_KOULUTUKSEN_PAIKKA,
                alkamispaiva = UPDATED_ALKAMISPAIVA,
                paattymispaiva = UPDATED_PAATTYMISPAIVA,
                erikoistumiseenHyvaksyttavaTuntimaara = UPDATED_ERIKOISTUMISEEN_HYVAKSYTTAVA_TUNTIMAARA
            )


            // Lisätään pakollinen tieto
            val erikoistuvaLaakari: ErikoistuvaLaakari
            if (em.findAll(ErikoistuvaLaakari::class).isEmpty()) {
                erikoistuvaLaakari = ErikoistuvaLaakariHelper.createUpdatedEntity(em)
                em.persist(erikoistuvaLaakari)
                em.flush()
            } else {
                erikoistuvaLaakari = em.findAll(ErikoistuvaLaakari::class).get(0)
            }
            teoriakoulutus.opintooikeus = erikoistuvaLaakari.getOpintooikeusKaytossa()

            return teoriakoulutus
        }
    }
}
