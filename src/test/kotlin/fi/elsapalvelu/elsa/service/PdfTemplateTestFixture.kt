package fi.elsapalvelu.elsa.service

import org.thymeleaf.context.Context
import java.time.LocalDate
import java.time.Period
import java.util.Locale

/** Non-empty view models for the real templates, independent of database setup. */
class PdfTemplateTestFixture(private val text: String) {
    private val date = LocalDate.of(2026, 10, 1)
    private val person = model(
        "nimi" to "Ääkkönen", "nimike" to "Lääkäri", "kuittausaika" to date,
        "puhelin" to "+358401234567", "sahkoposti" to "test@example.org",
        "toimipaikka" to "Sairaala", "lahiosoite" to "Katu 1", "postitoimipaikka" to "Jyväskylä"
    )
    private val work = model(
        "id" to 1L, "alkamispaiva" to date, "paattymispaiva" to date.plusMonths(6),
        "osaaikaprosentti" to 100.0, "hyvaksyttyAiempaanErikoisalaan" to false,
        "kaytannonKoulutus" to "TERVEYSKESKUSTYO",
        "tyoskentelypaikka" to model("nimi" to "Jyväskylän sairaala", "tyyppi" to "TERVEYSKESKUS",
            "kunta" to model("abbreviation" to "Jyväskylä"))
    )
    private val level = model("taso" to 4, "nimi" to "TOIMINTA_ILMAN_OHJAUSTA")
    private val scale = model("nimi" to "EPA", "tasot" to listOf(level))
    private val assessment = assessment()
    private val categories = listOf(model(
        "nimi" to "Kliiniset taidot", "arviointejaYhteensa" to 1,
        "arvioitavatKokonaisuudet" to listOf(model(
            "nimi" to "Potilastyö", "suoritusarvioinnit" to listOf(assessment), "arvioinnit" to listOf(assessment)
        ))
    ))
    private val procedure = model(
        "tyoskentelyjakso" to work, "suorite" to model("nimi" to "Potilastyö"),
        "arviointiasteikko" to scale, "arviointiasteikonTaso" to 4,
        "vaativuustaso" to 3, "suorituspaiva" to date, "lisatiedot" to text
    )
    private val trainingPeriod = model(
        "nimi" to "Koulutusjakso", "muutOsaamistavoitteet" to "Potilastyö",
        "tyoskentelyjaksot" to listOf(work), "osaamistavoitteet" to listOf(model("nimi" to "Potilastyö"))
    )
    private val theory = model(
        "koulutuksenNimi" to "Ääkköskoulutus", "koulutuksenPaikka" to "Jyväskylä",
        "alkamispaiva" to date, "paattymispaiva" to date.plusDays(1),
        "erikoistumiseenHyvaksyttavaTuntimaara" to 12.0,
        "nimi_fi" to "Ääkköskoulutus", "suorituspaiva" to date
    )

    fun context(template: String): Context {
        val variables = summaryVariables() + mapOf(
            "koulutussuunnitelma" to plan(), "paivakirjamerkinnat" to listOf(diary()),
            "arviointi" to assessment, "arvioinninKategoriat" to categories,
            "arviointiasteikko" to scale,
            "arviointiasteikonTasot" to if (template == "pdf/valmistumisenyhteenveto.html") listOf(level) else mapOf(4 to level),
            "vaativuusTasot" to mapOf(3 to "TAVANOMAINEN"), "suoritemerkinta" to procedure,
            "suoritteenKategoriat" to listOf(model("nimi" to "Toimenpiteet", "arviointiasteikko" to "EPA",
                "suoritteet" to listOf(model("nimi" to text, "vaadittulkm" to 1, "suoritemerkinnat" to listOf(procedure))))),
            "seurantajakso" to followUp(), "seurantajaksonTiedot" to followUpDetails(),
            "sopimus" to contract(), "arvio" to review(),
            "erikoistuvanSyntymaaika" to date.minusYears(35), "opintooikeudenPaattymispaiva" to date.plusYears(6),
            "yhteenlaskettuKesto" to Period.ofMonths(6)
        )
        return Context(Locale.forLanguageTag("fi"), variables)
    }

    private fun plan() = model(
        "koulutusjaksot" to listOf(trainingPeriod), "motivaatiokirje" to text,
        "motivaatiokirjeAsiakirja" to null, "motivaatiokirjeYksityinen" to false,
        "opiskeluJaTyohistoria" to "Työhistoria", "opiskeluJaTyohistoriaYksityinen" to false,
        "vahvuudet" to "Potilastyö", "vahvuudetYksityinen" to false,
        "tulevaisuudenVisiointi" to "Erikoislääkäri", "tulevaisuudenVisiointiYksityinen" to false,
        "osaamisenKartuttaminen" to "Koulutus", "osaamisenKartuttaminenYksityinen" to false,
        "elamankentta" to "Jyväskylä", "elamankenttaYksityinen" to false
    )

    private fun diary() = model(
        "oppimistapahtumanNimi" to "Oppimistapahtuma", "paivamaara" to date, "reflektio" to text,
        "aihekategoriat" to listOf(model("nimi" to "Koulutus", "teoriakoulutus" to true, "muunAiheenNimi" to false)),
        "teoriakoulutus" to theory, "muunAiheenNimi" to null
    )

    private fun assessment() = model(
        "arvioitavaTapahtuma" to text, "tapahtumanAjankohta" to date,
        "arvioinninAntaja" to person, "arvioinninSaaja" to person,
        "tyoskentelyjakso" to work, "arviointiasteikko" to scale,
        "arviointiasteikonTaso" to 4, "itsearviointiArviointiasteikonTaso" to 4,
        "arviointiAika" to date, "itsearviointiAika" to date,
        "arviointityokalut" to listOf(model("nimi" to "Arviointityökalu")),
        "arviointiPerustuu" to "LASNA", "muuPeruste" to null,
        "arvioitavatKokonaisuudet" to listOf(model(
            "arvioitavaKokonaisuus" to model("nimi" to "Potilastyö", "kategoria" to model("nimi" to "Kliiniset taidot")),
            "arviointiasteikonTaso" to 4, "itsearviointiArviointiasteikonTaso" to 4
        )),
        "vaativuustaso" to 3, "itsearviointiVaativuustaso" to 3,
        "sanallinenArviointi" to text, "sanallinenItsearviointi" to "Hyvä kehitys",
        "arviointiAsiakirjat" to listOf(model("nimi" to "Arviointi.pdf")),
        "itsearviointiAsiakirjat" to listOf(model("nimi" to "Itsearviointi.pdf"))
    )

    private fun followUp() = model(
        "erikoistuvanNimi" to "Ääkkönen", "erikoistuvanOpiskelijatunnus" to "123456",
        "erikoistuvanYliopistoNimi" to "HELSINGIN_YLIOPISTO", "alkamispaiva" to date,
        "paattymispaiva" to date.plusMonths(6), "seuraavanKeskustelunAjankohta" to date.plusMonths(7),
        "edistyminenTavoitteidenMukaista" to true, "hyvaksytty" to true, "kouluttaja" to person,
        "omaArviointi" to text, "lisahuomioita" to "Hyvä kehitys", "kouluttajanArvio" to "Hyvä kehitys",
        "erikoisalanTyoskentelyvalmiudet" to "Potilastyö", "huolenaiheet" to "Ei huolia",
        "seurantakeskustelunYhteisetMerkinnat" to "Yhteiset tavoitteet",
        "seuraavanJaksonTavoitteet" to "Potilastyö", "jatkotoimetJaRaportointi" to "Koulutus"
    )

    private fun followUpDetails() = model(
        "koulutusjaksot" to listOf(trainingPeriod), "arvioinnit" to categories, "arviointienMaara" to 1,
        "suoritemerkinnat" to listOf(model("suorite" to "Potilastyö", "suoritemerkinnat" to listOf(procedure))),
        "suoritemerkinnatMaara" to 1, "teoriakoulutukset" to listOf(theory)
    )

    private fun contract() = model(
        "erikoistuvanNimi" to "Ääkkönen", "erikoistuvanErikoisala" to "Yleislääketiede",
        "erikoistuvanOpiskelijatunnus" to "123456", "erikoistuvanSyntymaaika" to date.minusYears(35),
        "erikoistuvanYliopisto" to "HELSINGIN_YLIOPISTO", "opintooikeudenMyontamispaiva" to date,
        "koejaksonAlkamispaiva" to date, "erikoistuvanAllekirjoitusaika" to date,
        "erikoistuvanPuhelinnumero" to "+358401234567", "erikoistuvanSahkoposti" to "test@example.org",
        "koulutuspaikat" to listOf(model("nimi" to text, "koulutussopimusOmanYliopistonKanssa" to true,
            "yliopisto" to "HELSINGIN_YLIOPISTO")),
        "kouluttajat" to listOf(person), "vastuuhenkilo" to person
    )

    private fun review() = model(
        "erikoistuvanNimi" to "Ääkkönen", "erikoistuvanErikoisala" to "Yleislääketiede",
        "erikoistuvanOpiskelijatunnus" to "123456", "erikoistuvanYliopisto" to "HELSINGIN_YLIOPISTO",
        "erikoistuvanKuittausaika" to date, "erikoistuvanPuhelinnumero" to "+358401234567",
        "erikoistuvanSahkoposti" to "test@example.org", "muutOpintooikeudet" to emptyList<Any>(),
        "koulutussopimusHyvaksytty" to true, "koejaksonSuorituspaikat" to model("tyoskentelyjaksot" to listOf(work)),
        "aloituskeskustelu" to model("koejaksonAlkamispaiva" to date, "koejaksonPaattymispaiva" to date.plusMonths(6),
            "koejaksonSuorituspaikka" to "Jyväskylä", "suoritettuKokoaikatyossa" to true,
            "lahiesimies" to person, "lahikouluttaja" to person),
        "valiarviointi" to model("edistyminenTavoitteidenMukaista" to true, "lahiesimies" to person, "lahikouluttaja" to person),
        "loppukeskustelu" to model("esitetaanKoejaksonHyvaksymista" to true, "lahiesimies" to person, "lahikouluttaja" to person),
        "virkailija" to person, "virkailijanYhteenveto" to "<p><strong>$text</strong> &amp; kouluttaja</p>",
        "vastuuhenkilo" to person, "vastuuhenkilonPuhelinnumero" to "+358401234567",
        "vastuuhenkilonSahkoposti" to "test@example.org", "koejaksoHyvaksytty" to true,
        "perusteluHylkaamiselle" to null, "hylattyArviointiKaytyLapiKeskustellen" to false
    )

    private fun graduation() = model(
        "erikoistujanNimi" to "Ääkkönen", "erikoistujanErikoisala" to "Yleislääketiede",
        "erikoistujanOpiskelijatunnus" to "123456", "erikoistujanYliopisto" to "HELSINGIN_YLIOPISTO",
        "erikoistujanSyntymaaika" to date.minusYears(35), "opintooikeudenMyontamispaiva" to date,
        "erikoistujanLaillistamispaiva" to date, "erikoistujanAsetus" to "Asetus 56/2015",
        "vastuuhenkiloOsaamisenArvioijaNimi" to "Ääkkönen", "vastuuhenkiloOsaamisenArvioijaNimike" to "Lääkäri",
        "vastuuhenkiloOsaamisenArvioijaKuittausaika" to date, "vastuuhenkiloHyvaksyjaNimi" to "Ääkkönen",
        "vastuuhenkiloHyvaksyjaNimike" to "Lääkäri", "vastuuhenkiloHyvaksyjaKuittausaika" to date,
        "virkailijaNimi" to "Ääkkönen", "virkailijanKuittausaika" to date, "virkailijanSaate" to text,
        "selvitysVanhentuneistaSuorituksista" to text
    )

    private fun inspection() = model(
        "valmistumispyynto" to graduation(), "virkailijanYhteenveto" to "<p><strong>$text</strong> &amp; kouluttaja</p>",
        "yekSuoritettu" to true, "yekSuorituspaiva" to date, "ptlSuoritettu" to false, "ptlSuorituspaiva" to null,
        "aiempiElKoulutusSuoritettu" to false, "aiempiElKoulutusSuorituspaiva" to null,
        "ltTutkintoSuoritettu" to false, "ltTutkintoSuorituspaiva" to null,
        "terveyskeskustyoHyvaksyttyPvm" to date, "koejaksoHyvaksyttyPvm" to date,
        "yliopistosairaalanUlkopuolinenTyoTarkistettu" to true,
        "suoritustenTila" to model("vanhojaTyoskentelyjaksojaOrSuorituksiaExists" to true),
        "kuulustelut" to listOf(model("nimi_fi" to "Valtakunnallinen kuulustelu", "hyvaksytty" to true, "suorituspaiva" to date))
    )

    private fun summaryVariables(): Map<String, Any?> {
        val numeric = listOf(
            "teoriakoulutusSuoritettu", "teoriakoulutusVaadittu", "teoriakoulutusSuoritettuYhteensa",
            "johtamiskoulutusSuoritettu", "johtamiskoulutusVaadittu", "sateilusuojakoulutusSuoritettu", "sateilusuojakoulutusVaadittu"
        ).associateWith { 12.0 }
        val durations = listOf(
            "yhteensaSuoritettu", "yhteensaVaadittuVahintaan", "tyoskentelyaikaYhteensa",
            "terveyskeskusSuoritettu", "terveyskeskusVaadittuVahintaan", "terveyskeskustyoSuoritettu",
            "omaErikoisalaSuoritettu", "omaaErikoisalaaTukevaSuoritettu", "tutkimustyoSuoritettu",
            "yliopistosairaalaSuoritettu", "yliopistosairaalaVaadittuVahintaan",
            "yliopistosairaaloidenUlkopuolinenSuoritettu", "yliopistosairaaloidenUlkopuolinenVaadittuVahintaan",
            "sairaalaSuoritettu", "sairaalaVaadittuVahintaan", "muuSuoritettu", "muuVaadittuVahintaan"
        ).associateWith { "6 kk" }
        return numeric + durations + model(
            "tarkistus" to inspection(), "tyoskentelyjaksot" to listOf(work), "tyoskentelyjaksotSuoritettu" to mapOf(1L to "6 kk"),
            "teoriakoulutukset" to listOf(theory), "arvioErikoistumiseenHyvaksyttavista" to "6 kk",
            "arvioPuuttuvastaKoulutuksesta" to "0 kk", "laakarikoulutusSuoritettuSuomiTaiBelgia" to true,
            "omaErikoisalaOsuus" to 25, "omaErikoisalaaTukevaOsuus" to 25, "tutkimustyoOsuus" to 25, "terveyskeskustyoOsuus" to 25
        )
    }

    private fun model(vararg fields: Pair<String, Any?>): Map<String, Any?> = mapOf(*fields)
}
