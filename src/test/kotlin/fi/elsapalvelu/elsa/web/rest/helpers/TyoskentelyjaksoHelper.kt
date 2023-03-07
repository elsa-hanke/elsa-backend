package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.Tyoskentelyjakso
import fi.elsapalvelu.elsa.domain.Tyoskentelypaikka
import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.domain.enumeration.KaytannonKoulutusTyyppi
import fi.elsapalvelu.elsa.domain.enumeration.TyoskentelyjaksoTyyppi
import fi.elsapalvelu.elsa.web.rest.findAll
import java.time.LocalDate
import java.time.ZoneId
import jakarta.persistence.EntityManager

class TyoskentelyjaksoHelper {

    companion object {

        private val DEFAULT_ALKAMISPAIVA: LocalDate = LocalDate.ofEpochDay(0L)
        private val UPDATED_ALKAMISPAIVA: LocalDate = LocalDate.now(ZoneId.systemDefault())

        private val DEFAULT_PAATTYMISPAIVA: LocalDate = LocalDate.ofEpochDay(10L)
        private val UPDATED_PAATTYMISPAIVA: LocalDate = LocalDate.now(ZoneId.systemDefault())

        private const val DEFAULT_OSAAIKAPROSENTTI: Int = 50
        private const val UPDATED_OSAAIKAPROSENTTI: Int = 100

        private val DEFAULT_KAYTANNON_KOULUTUS: KaytannonKoulutusTyyppi =
            KaytannonKoulutusTyyppi.OMAN_ERIKOISALAN_KOULUTUS
        private val UPDATED_KAYTANNON_KOULUTUS: KaytannonKoulutusTyyppi =
            KaytannonKoulutusTyyppi.TUTKIMUSTYO

        private const val DEFAULT_HYVAKSYTTY_AIEMPAAN_ERIKOISALAAN: Boolean = false
        private const val UPDATED_HYVAKSYTTY_AIEMPAAN_ERIKOISALAAN: Boolean = true

        @JvmStatic
        fun createEntity(
            em: EntityManager,
            user: User? = null,
            alkamispaiva: LocalDate = DEFAULT_ALKAMISPAIVA,
            paattymispaiva: LocalDate = DEFAULT_PAATTYMISPAIVA,
            tyoskentelyjaksoTyyppi: TyoskentelyjaksoTyyppi = TyoskentelypaikkaHelper.DEFAULT_TYYPPI,
            kaytannonKoulutus: KaytannonKoulutusTyyppi? = DEFAULT_KAYTANNON_KOULUTUS
        ): Tyoskentelyjakso {
            val tyoskentelyjakso = Tyoskentelyjakso(
                alkamispaiva = alkamispaiva,
                paattymispaiva = paattymispaiva,
                osaaikaprosentti = DEFAULT_OSAAIKAPROSENTTI,
                kaytannonKoulutus = kaytannonKoulutus,
                hyvaksyttyAiempaanErikoisalaan = DEFAULT_HYVAKSYTTY_AIEMPAAN_ERIKOISALAAN
            )

            val tyoskentelypaikka = TyoskentelypaikkaHelper.createEntity(em, tyoskentelyjaksoTyyppi)
            em.persist(tyoskentelypaikka)
            em.flush()

            tyoskentelyjakso.tyoskentelypaikka = tyoskentelypaikka

            // Lisätään pakollinen tieto
            val erikoistuvaLaakari: ErikoistuvaLaakari
            if (em.findAll(ErikoistuvaLaakari::class).isEmpty()) {
                erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, user)
                em.persist(erikoistuvaLaakari)
                em.flush()
            } else {
                erikoistuvaLaakari = em.findAll(ErikoistuvaLaakari::class).get(0)
            }

            tyoskentelyjakso.opintooikeus = erikoistuvaLaakari.getOpintooikeusKaytossa()

            return tyoskentelyjakso
        }

        @JvmStatic
        fun createUpdatedEntity(
            em: EntityManager,
            alkamispaiva: LocalDate = UPDATED_ALKAMISPAIVA,
            paattymispaiva: LocalDate = UPDATED_PAATTYMISPAIVA,
            tyoskentelyjaksoTyyppi: TyoskentelyjaksoTyyppi = TyoskentelypaikkaHelper.UPDATED_TYYPPI
        ): Tyoskentelyjakso {
            val tyoskentelyjakso = Tyoskentelyjakso(
                alkamispaiva = alkamispaiva,
                paattymispaiva = paattymispaiva,
                osaaikaprosentti = UPDATED_OSAAIKAPROSENTTI,
                kaytannonKoulutus = UPDATED_KAYTANNON_KOULUTUS,
                hyvaksyttyAiempaanErikoisalaan = UPDATED_HYVAKSYTTY_AIEMPAAN_ERIKOISALAAN
            )

            // Lisätään pakollinen tieto
            val tyoskentelypaikka: Tyoskentelypaikka
            if (em.findAll(Tyoskentelypaikka::class).isEmpty()) {
                tyoskentelypaikka =
                    TyoskentelypaikkaHelper.createUpdatedEntity(em, tyoskentelyjaksoTyyppi)
                em.persist(tyoskentelypaikka)
                em.flush()
            } else {
                tyoskentelypaikka = em.findAll(Tyoskentelypaikka::class).get(0)
            }
            tyoskentelyjakso.tyoskentelypaikka = tyoskentelypaikka
            // Lisätään pakollinen tieto
            val erikoistuvaLaakari: ErikoistuvaLaakari
            if (em.findAll(ErikoistuvaLaakari::class).isEmpty()) {
                erikoistuvaLaakari = ErikoistuvaLaakariHelper.createUpdatedEntity(em)
                em.persist(erikoistuvaLaakari)
                em.flush()
            } else {
                erikoistuvaLaakari = em.findAll(ErikoistuvaLaakari::class).get(0)
            }
            tyoskentelyjakso.opintooikeus = erikoistuvaLaakari.getOpintooikeusKaytossa()
            return tyoskentelyjakso
        }
    }
}
