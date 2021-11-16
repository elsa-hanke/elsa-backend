package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.*
import java.time.LocalDate
import java.time.ZoneId

class KoejaksonVaiheetHelper {

    companion object {
        const val DEFAULT_ID = "c47f46ad-21c4-47e8-9c7c-ba44f60c8bae"
        const val DEFAULT_LOGIN = "johndoe"
        const val DEFAULT_EMAIL = "john.doe@example.com"
        const val UPDATED_EMAIL = "doe.john@example.com"
        const val UPDATED_PHONE = "+358101001010"
        val DEFAULT_SYNTYMAAIKA: LocalDate = LocalDate.ofEpochDay(0L)
        val DEFAULT_MYONTAMISPAIVA: LocalDate = LocalDate.ofEpochDay(1L)
        val DEFAULT_ALKAMISPAIVA: LocalDate = LocalDate.ofEpochDay(2L)
        val DEFAULT_PAATTYMISPAIVA: LocalDate = LocalDate.ofEpochDay(5L)
        val DEFAULT_MUOKKAUSPAIVA: LocalDate = LocalDate.now(ZoneId.systemDefault())
        val DEFAULT_KUITTAUSAIKA_KOULUTTAJA: LocalDate = LocalDate.ofEpochDay(3L)
        val DEFAULT_KUITTAUSAIKA_VASTUUHENKILO: LocalDate = LocalDate.now(ZoneId.systemDefault())
        const val DEFAULT_KOULUTTAJA_ID = "4b73bc2c-88c4-11eb-8dcd-0242ac130003"
        const val DEFAULT_VASTUUHENKILO_ID = "53d6e70e-88c4-11eb-8dcd-0242ac130003"
        const val DEFAULT_ESIMIES_ID = "43c0ebfa-92f9-11eb-a8b3-0242ac130003"
        const val DEFAULT_KOULUTUSPAIKKA = "TAYS Päivystyskeskus"
        const val DEFAULT_YLIOPISTO = "TAYS"
        const val UPDATED_NIMIKE = "Nimike"
        const val UPDATED_LAHIOSOITE = "Testitie"
        const val UPDATED_TOIMIPAIKKA = "Sairaala"
        const val UPDATED_POSTITOIMIPAIKKA = "Tampere"
        const val UPDATED_KORJAUSEHDOTUS = "Lorem Ipsum"
        const val DEFAULT_OSAAMISTAVOITTEET = "Lorem ipsum"
        const val DEFAULT_VAHVUUDET = "Lorem ipsum"
        const val DEFAULT_KEHITTAMISTOIMENPITEET = "Lorem ipsum"
        const val DEFAULT_ERIKOISALA = "Erikoisala"

        @JvmStatic
        fun createKoulutussopimus(
            erikoistuvaLaakari: ErikoistuvaLaakari,
            vastuuhenkilo: Kayttaja
        ): KoejaksonKoulutussopimus {
            return KoejaksonKoulutussopimus(
                erikoistuvaLaakari = erikoistuvaLaakari,
                erikoistuvanNimi = erikoistuvaLaakari.kayttaja?.getNimi(),
                erikoistuvanOpiskelijatunnus = erikoistuvaLaakari.opintooikeudet.firstOrNull()?.opiskelijatunnus,
                erikoistuvanSyntymaaika = DEFAULT_SYNTYMAAIKA,
                erikoistuvanYliopisto = erikoistuvaLaakari.opintooikeudet.firstOrNull()?.yliopisto?.nimi,
                opintooikeudenMyontamispaiva = DEFAULT_MYONTAMISPAIVA,
                koejaksonAlkamispaiva = DEFAULT_ALKAMISPAIVA,
                erikoistuvanPuhelinnumero = erikoistuvaLaakari.kayttaja?.user?.phoneNumber,
                erikoistuvanSahkoposti = erikoistuvaLaakari.kayttaja?.user?.email,
                lahetetty = true,
                muokkauspaiva = DEFAULT_MUOKKAUSPAIVA,
                vastuuhenkilo = vastuuhenkilo,
                vastuuhenkilonNimi = vastuuhenkilo.getNimi(),
                vastuuhenkilonNimike = vastuuhenkilo.nimike,
            )
        }

        @JvmStatic
        fun createAloituskeskustelu(
            erikoistuvaLaakari: ErikoistuvaLaakari,
            lahikouluttaja: Kayttaja,
            lahiesimies: Kayttaja
        ): KoejaksonAloituskeskustelu {
            return KoejaksonAloituskeskustelu(
                erikoistuvaLaakari = erikoistuvaLaakari,
                erikoistuvanNimi = erikoistuvaLaakari.kayttaja?.getNimi(),
                erikoistuvanErikoisala = erikoistuvaLaakari.opintooikeudet.firstOrNull()?.erikoisala?.nimi,
                erikoistuvanOpiskelijatunnus = erikoistuvaLaakari.opintooikeudet.firstOrNull()?.opiskelijatunnus,
                erikoistuvanYliopisto = erikoistuvaLaakari.opintooikeudet.firstOrNull()?.yliopisto?.nimi,
                erikoistuvanSahkoposti = erikoistuvaLaakari.kayttaja?.user?.email,
                koejaksonSuorituspaikka = DEFAULT_KOULUTUSPAIKKA,
                koejaksonAlkamispaiva = DEFAULT_ALKAMISPAIVA,
                koejaksonPaattymispaiva = DEFAULT_PAATTYMISPAIVA,
                suoritettuKokoaikatyossa = true,
                lahikouluttaja = lahikouluttaja,
                lahikouluttajanNimi = lahikouluttaja.getNimi(),
                lahiesimies = lahiesimies,
                lahiesimiehenNimi = lahiesimies.getNimi(),
                koejaksonOsaamistavoitteet = DEFAULT_OSAAMISTAVOITTEET,
                lahetetty = true,
                muokkauspaiva = DEFAULT_MUOKKAUSPAIVA
            )
        }

        @JvmStatic
        fun createValiarviointi(
            erikoistuvaLaakari: ErikoistuvaLaakari,
            lahikouluttaja: Kayttaja,
            lahiesimies: Kayttaja
        ): KoejaksonValiarviointi {
            return KoejaksonValiarviointi(
                erikoistuvaLaakari = erikoistuvaLaakari,
                erikoistuvanNimi = erikoistuvaLaakari.kayttaja?.getNimi(),
                erikoistuvanErikoisala = erikoistuvaLaakari.opintooikeudet.firstOrNull()?.erikoisala?.nimi,
                erikoistuvanOpiskelijatunnus = erikoistuvaLaakari.opintooikeudet.firstOrNull()?.opiskelijatunnus,
                erikoistuvanYliopisto = erikoistuvaLaakari.opintooikeudet.firstOrNull()?.yliopisto?.nimi,
                lahikouluttaja = lahikouluttaja,
                lahikouluttajanNimi = lahikouluttaja.getNimi(),
                lahiesimies = lahiesimies,
                lahiesimiehenNimi = lahiesimies.getNimi(),
                muokkauspaiva = DEFAULT_MUOKKAUSPAIVA
            )
        }

        @JvmStatic
        fun createKehittamistoimenpiteet(
            erikoistuvaLaakari: ErikoistuvaLaakari,
            lahikouluttaja: Kayttaja,
            lahiesimies: Kayttaja
        ): KoejaksonKehittamistoimenpiteet {
            return KoejaksonKehittamistoimenpiteet(
                erikoistuvaLaakari = erikoistuvaLaakari,
                erikoistuvanNimi = erikoistuvaLaakari.kayttaja?.getNimi(),
                erikoistuvanErikoisala = erikoistuvaLaakari.opintooikeudet.firstOrNull()?.erikoisala?.nimi,
                erikoistuvanOpiskelijatunnus = erikoistuvaLaakari.opintooikeudet.firstOrNull()?.opiskelijatunnus,
                erikoistuvanYliopisto = erikoistuvaLaakari.opintooikeudet.firstOrNull()?.yliopisto?.nimi,
                lahikouluttaja = lahikouluttaja,
                lahikouluttajanNimi = lahikouluttaja.getNimi(),
                lahiesimies = lahiesimies,
                lahiesimiehenNimi = lahiesimies.getNimi(),
                muokkauspaiva = DEFAULT_MUOKKAUSPAIVA
            )
        }

        @JvmStatic
        fun createLoppukeskustelu(
            erikoistuvaLaakari: ErikoistuvaLaakari,
            lahikouluttaja: Kayttaja,
            lahiesimies: Kayttaja
        ): KoejaksonLoppukeskustelu {
            return KoejaksonLoppukeskustelu(
                erikoistuvaLaakari = erikoistuvaLaakari,
                erikoistuvanNimi = erikoistuvaLaakari.kayttaja?.getNimi(),
                erikoistuvanErikoisala = erikoistuvaLaakari.opintooikeudet.firstOrNull()?.erikoisala?.nimi,
                erikoistuvanOpiskelijatunnus = erikoistuvaLaakari.opintooikeudet.firstOrNull()?.opiskelijatunnus,
                erikoistuvanYliopisto = erikoistuvaLaakari.opintooikeudet.firstOrNull()?.yliopisto?.nimi,
                lahikouluttaja = lahikouluttaja,
                lahikouluttajanNimi = lahikouluttaja.getNimi(),
                lahiesimies = lahiesimies,
                lahiesimiehenNimi = lahiesimies.getNimi(),
                muokkauspaiva = DEFAULT_MUOKKAUSPAIVA
            )
        }

        @JvmStatic
        fun createKoulutussopimuksenKouluttaja(
            koejaksonKoulutussopimus: KoejaksonKoulutussopimus,
            kouluttaja: Kayttaja
        ): KoulutussopimuksenKouluttaja {
            return KoulutussopimuksenKouluttaja(
                kouluttaja = kouluttaja,
                nimi = kouluttaja.getNimi(),
                koulutussopimus = koejaksonKoulutussopimus,
                sopimusHyvaksytty = true,
                kuittausaika = DEFAULT_KUITTAUSAIKA_KOULUTTAJA
            )
        }

        @JvmStatic
        fun createKoulutussopimuksenKoulutuspaikka(
            koejaksonKoulutussopimus: KoejaksonKoulutussopimus,
        ): KoulutussopimuksenKoulutuspaikka {
            return KoulutussopimuksenKoulutuspaikka(
                nimi = DEFAULT_KOULUTUSPAIKKA,
                yliopisto = DEFAULT_YLIOPISTO,
                koulutussopimus = koejaksonKoulutussopimus
            )
        }
    }
}
