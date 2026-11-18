package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.tyoskentely.Tyoskentelyjakso
import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.domain.koulutus.KaytannonKoulutusTyyppi
import jakarta.persistence.EntityManager
import java.time.LocalDate

object YekKoulutettavaTyoskentelyjaksoHelper {

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

    fun createEntity(
        em: EntityManager,
        user: User? = null,
        alkamispaiva: LocalDate? = DEFAULT_ALKAMISPAIVA,
        paattymispaiva: LocalDate? = DEFAULT_PAATTYMISPAIVA,
        kaytannonKoulutus: KaytannonKoulutusTyyppi? = DEFAULT_KAYTANNON_KOULUTUS,
        opintooikeus: Opintooikeus = luoYekOpintooikeus(em, user)
    ): Tyoskentelyjakso {
        val tyoskentelyjakso = Tyoskentelyjakso(
            alkamispaiva = alkamispaiva,
            paattymispaiva = paattymispaiva,
            osaaikaprosentti = DEFAULT_OSAAIKAPROSENTTI,
            kaytannonKoulutus = kaytannonKoulutus,
            hyvaksyttyAiempaanErikoisalaan = DEFAULT_HYVAKSYTTY_AIEMPAAN_ERIKOISALAAN
        )

        // Lisätään pakollinen tieto
        tyoskentelyjakso.tyoskentelypaikka = TyoskentelypaikkaHelper.createEntity(em)

        tyoskentelyjakso.opintooikeus = opintooikeus
        return tyoskentelyjakso
    }

    fun createUpdatedEntity(em: EntityManager, opintooikeus: Opintooikeus): Tyoskentelyjakso {
        val tyoskentelyjakso = Tyoskentelyjakso(
            alkamispaiva = UPDATED_ALKAMISPAIVA,
            paattymispaiva = UPDATED_PAATTYMISPAIVA,
            osaaikaprosentti = UPDATED_OSAAIKAPROSENTTI,
            kaytannonKoulutus = UPDATED_KAYTANNON_KOULUTUS,
            hyvaksyttyAiempaanErikoisalaan = UPDATED_HYVAKSYTTY_AIEMPAAN_ERIKOISALAAN
        )

        tyoskentelyjakso.tyoskentelypaikka = TyoskentelypaikkaHelper.createUpdatedEntity(em).also(em::persist)
        tyoskentelyjakso.opintooikeus = opintooikeus

        return tyoskentelyjakso
    }
    private fun luoYekOpintooikeus(em: EntityManager, user: User?): Opintooikeus {
        val erikoistuva = Testisuhteet.opintooikeus(em, user).erikoistuvaLaakari!!
        val opintooikeus = OpintooikeusHelper.addOpintooikeusForYekKoulutettava(em, erikoistuva)
        OpintooikeusHelper.setOpintooikeusKaytossa(erikoistuva, opintooikeus)
        return opintooikeus
    }
}
