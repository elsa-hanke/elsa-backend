package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.Tyoskentelyjakso
import fi.elsapalvelu.elsa.domain.Tyoskentelypaikka
import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.domain.enumeration.KaytannonKoulutusTyyppi
import fi.elsapalvelu.elsa.web.rest.findAll
import jakarta.persistence.EntityManager
import java.time.LocalDate

object ErikoistuvaLaakariTyoskentelyjaksoHelper {
    val DEFAULT_ALKAMISPAIVA: LocalDate = LocalDate.of(2020, 1, 1)
    val UPDATED_ALKAMISPAIVA: LocalDate = LocalDate.of(2020, 2, 1)
    val DEFAULT_PAATTYMISPAIVA: LocalDate = LocalDate.of(2020, 1, 30)
    val UPDATED_PAATTYMISPAIVA: LocalDate = LocalDate.of(2020, 4, 1)
    const val DEFAULT_OSAAIKAPROSENTTI: Int = 100
    const val UPDATED_OSAAIKAPROSENTTI: Int = 50
    val DEFAULT_KAYTANNON_KOULUTUS: KaytannonKoulutusTyyppi = KaytannonKoulutusTyyppi.OMAN_ERIKOISALAN_KOULUTUS
    val UPDATED_KAYTANNON_KOULUTUS: KaytannonKoulutusTyyppi = KaytannonKoulutusTyyppi.OMAA_ERIKOISALAA_TUKEVA_KOULUTUS
    const val DEFAULT_HYVAKSYTTY_AIEMPAAN_ERIKOISALAAN: Boolean = false
    const val UPDATED_HYVAKSYTTY_AIEMPAAN_ERIKOISALAAN: Boolean = true

    fun createEntity(em: EntityManager, user: User? = null, alkamispaiva: LocalDate? = DEFAULT_ALKAMISPAIVA, paattymispaiva: LocalDate? = DEFAULT_PAATTYMISPAIVA,
                     kaytannonKoulutus: KaytannonKoulutusTyyppi? = DEFAULT_KAYTANNON_KOULUTUS): Tyoskentelyjakso {
        val tyoskentelyjakso = Tyoskentelyjakso(alkamispaiva = alkamispaiva, paattymispaiva = paattymispaiva, osaaikaprosentti = DEFAULT_OSAAIKAPROSENTTI,
            kaytannonKoulutus = kaytannonKoulutus, hyvaksyttyAiempaanErikoisalaan = DEFAULT_HYVAKSYTTY_AIEMPAAN_ERIKOISALAAN)

        // Lisätään pakollinen tieto
        tyoskentelyjakso.tyoskentelypaikka = TyoskentelypaikkaHelper.createEntity(em)

        // Lisätään pakollinen tieto
        var erikoistuvaLaakari =
            em.findAll(ErikoistuvaLaakari::class).firstOrNull { it.kayttaja?.user == user }
        if (erikoistuvaLaakari == null) {
            erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, user)
            em.persist(erikoistuvaLaakari)
            em.flush()
        }
        tyoskentelyjakso.opintooikeus = erikoistuvaLaakari.getOpintooikeusKaytossa()
        return tyoskentelyjakso
    }

    fun createUpdatedEntity(em: EntityManager): Tyoskentelyjakso {
        val tyoskentelyjakso = Tyoskentelyjakso(alkamispaiva = UPDATED_ALKAMISPAIVA, paattymispaiva = UPDATED_PAATTYMISPAIVA, osaaikaprosentti = UPDATED_OSAAIKAPROSENTTI,
            kaytannonKoulutus = UPDATED_KAYTANNON_KOULUTUS, hyvaksyttyAiempaanErikoisalaan = UPDATED_HYVAKSYTTY_AIEMPAAN_ERIKOISALAAN)

        // Lisätään pakollinen tieto
        val tyoskentelypaikka: Tyoskentelypaikka
        if (em.findAll(Tyoskentelypaikka::class).isEmpty()) {
            tyoskentelypaikka = TyoskentelypaikkaHelper.createUpdatedEntity(em)
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
