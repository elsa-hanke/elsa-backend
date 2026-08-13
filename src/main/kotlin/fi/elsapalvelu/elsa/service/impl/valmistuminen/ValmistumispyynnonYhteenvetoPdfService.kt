package fi.elsapalvelu.elsa.service.impl.valmistuminen

import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.kayttaja.AsiakirjaData
import fi.elsapalvelu.elsa.domain.koulutus.KaytannonKoulutusTyyppi
import fi.elsapalvelu.elsa.domain.koulutus.OpintosuoritusTyyppiEnum
import fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto
import fi.elsapalvelu.elsa.extensions.format
import fi.elsapalvelu.elsa.extensions.toDays
import fi.elsapalvelu.elsa.extensions.toMonths
import fi.elsapalvelu.elsa.extensions.toYears
import fi.elsapalvelu.elsa.repository.kayttaja.AsiakirjaRepository
import fi.elsapalvelu.elsa.repository.valmistuminen.ValmistumispyyntoRepository
import fi.elsapalvelu.elsa.required
import fi.elsapalvelu.elsa.service.arviointi.ArviointiasteikkoService
import fi.elsapalvelu.elsa.service.dto.tyoskentely.TyoskentelyjaksotTilastotDTO
import fi.elsapalvelu.elsa.service.dto.valmistuminen.ValmistumispyynnonTarkistusDTO
import fi.elsapalvelu.elsa.service.koulutus.OpintosuoritusService
import fi.elsapalvelu.elsa.service.koulutus.TeoriakoulutusService
import fi.elsapalvelu.elsa.service.tyoskentely.TyoskentelyjaksoService
import fi.elsapalvelu.elsa.service.valmistuminen.PdfService
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.thymeleaf.context.Context
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.Locale

@Service
class ValmistumispyynnonYhteenvetoPdfService(
    private val pdfService: PdfService,
    private val asiakirjaRepository: AsiakirjaRepository,
    private val valmistumispyyntoRepository: ValmistumispyyntoRepository,
    private val tyoskentelyjaksoService: TyoskentelyjaksoService,
    private val teoriakoulutusService: TeoriakoulutusService,
    private val arviointiasteikkoService: ArviointiasteikkoService,
    private val opintosuoritusService: OpintosuoritusService,
    private val arviointitietoService: ValmistumispyynnonArviointitietoService
) {

    fun luo(
        tarkistus: ValmistumispyynnonTarkistusDTO,
        valmistumispyynto: Valmistumispyynto
    ): Asiakirja = tallenna(
        valmistumispyynto = valmistumispyynto,
        template = "pdf/valmistumisenyhteenveto.html",
        tiedostonimenAlku = "valmistumisen_yhteenveto",
        context = luoContext(tarkistus, valmistumispyynto)
    )

    fun luoYek(
        tarkistus: ValmistumispyynnonTarkistusDTO,
        valmistumispyynto: Valmistumispyynto
    ): Asiakirja = tallenna(
        valmistumispyynto = valmistumispyynto,
        template = "pdf/valmistumisenyhteenveto_yek.html",
        tiedostonimenAlku = "valmistumisen_yhteenveto_yek",
        context = luoYekContext(tarkistus, valmistumispyynto)
    )

    private fun luoContext(
        tarkistus: ValmistumispyynnonTarkistusDTO,
        valmistumispyynto: Valmistumispyynto
    ) = Context(SUOMEN_LOCALE).apply {
        lisaaTarkistusmuuttujat(tarkistus)
        setVariable("sateilusuojakoulutusSuoritettu", tarkistus.sateilusuojakoulutusSuoritettu)
        setVariable("sateilusuojakoulutusVaadittu", tarkistus.sateilusuojakoulutusVaadittu)
        setVariable("johtamiskoulutusSuoritettu", tarkistus.johtamiskoulutusSuoritettu)
        setVariable("johtamiskoulutusVaadittu", tarkistus.johtamiskoulutusVaadittu)
        setVariable("virkailijanYhteenveto", tarkistus.virkailijanYhteenveto)

        valmistumispyynto.opintooikeus?.let { opintooikeus ->
            val tilastot = tyoskentelyjaksoService.getTilastot(opintooikeus)
            lisaaTyoskentelytilastot(tilastot)

            val koulutusYhteensa = tilastot.kaytannonKoulutus.sumOf { it.suoritettu }
            tilastot.kaytannonKoulutus.forEach { koulutus ->
                lisaaKaytannonKoulutus(
                    koulutus.kaytannonKoulutus,
                    koulutus.suoritettu,
                    koulutusYhteensa
                )
            }

            setVariable(
                "tyoskentelyjaksot",
                tyoskentelyjaksoService.findAllByOpintooikeusId(opintooikeus.id.required())
                    .sortedByDescending { it.alkamispaiva }
            )
            setVariable(
                "tyoskentelyjaksotSuoritettu",
                tilastot.tyoskentelyjaksot.groupBy { it.id }
                    .mapValues { (_, jaksot) ->
                        paivatAjanjaksoksi(jaksot.sumOf { it.suoritettu }).format()
                    }
            )

            val teoriakoulutukset = teoriakoulutusService.findAll(opintooikeus.id.required())
            setVariable("teoriakoulutukset", teoriakoulutukset)
            setVariable(
                "teoriakoulutusSuoritettuYhteensa",
                teoriakoulutukset.mapNotNull { it.erikoistumiseenHyvaksyttavaTuntimaara }.sum()
            )
            setVariable(
                "teoriakoulutusVaadittu",
                opintooikeus.opintoopas?.erikoisalanVaatimaTeoriakoulutustenVahimmaismaara
            )
            setVariable(
                "arvioinninKategoriat",
                arviointitietoService.haeKategoriat(opintooikeus.id.required(), true)
            )
            setVariable(
                "arviointiasteikonTasot",
                arviointiasteikkoService.findByOpintooikeusId(opintooikeus.id.required())?.tasot
            )
        }
    }

    private fun luoYekContext(
        tarkistus: ValmistumispyynnonTarkistusDTO,
        valmistumispyynto: Valmistumispyynto
    ) = Context(SUOMEN_LOCALE).apply {
        lisaaTarkistusmuuttujat(tarkistus)

        valmistumispyynto.opintooikeus?.let { opintooikeus ->
            val tilastot = tyoskentelyjaksoService.getTilastot(opintooikeus)
            lisaaYekTyoskentelytilastot(tilastot)
            val tyoskentelyjaksot =
                tyoskentelyjaksoService.findAllByOpintooikeusId(opintooikeus.id.required())
            setVariable(
                "tyoskentelyjaksot",
                tyoskentelyjaksot.sortedByDescending { it.alkamispaiva }
            )
            setVariable(
                "tyoskentelyjaksotSuoritettu",
                tilastot.tyoskentelyjaksot.groupBy { it.id }
                    .mapValues { (_, jaksot) ->
                        paivatAjanjaksoksi(jaksot.sumOf { it.suoritettu }).format()
                    }
            )
            setVariable(
                "laakarikoulutusSuoritettuSuomiTaiBelgia",
                opintooikeus.erikoistuvaLaakari?.laakarikoulutusSuoritettuSuomiTaiBelgia
            )

            val opintosuoritukset =
                opintosuoritusService.getOpintosuorituksetByOpintooikeusIdAndTyyppi(
                opintooikeus.id.required(),
                OpintosuoritusTyyppiEnum.YEK_TEORIAKOULUTUS
            )
            setVariable("teoriakoulutukset", opintosuoritukset.opintosuoritukset)
        }
    }

    private fun Context.lisaaTarkistusmuuttujat(tarkistus: ValmistumispyynnonTarkistusDTO) {
        setVariable("tarkistus", tarkistus)
        setVariable("teoriakoulutusSuoritettu", tarkistus.teoriakoulutusSuoritettu)
        setVariable("teoriakoulutusVaadittu", tarkistus.teoriakoulutusVaadittu)
    }

    private fun Context.lisaaTyoskentelytilastot(tilastot: TyoskentelyjaksotTilastotDTO) {
        lisaaYhteisetTyoskentelytilastot(tilastot)
        val koulutustyypit = tilastot.koulutustyypit
        setVariable(
            "yliopistosairaalaSuoritettu",
            paivatAjanjaksoksi(koulutustyypit.yliopistosairaalaSuoritettu).format()
        )
        setVariable(
            "yliopistosairaalaVaadittuVahintaan",
            paivatAjanjaksoksi(koulutustyypit.yliopistosairaalaVaadittuVahintaan).format()
        )
        setVariable(
            "yliopistosairaaloidenUlkopuolinenSuoritettu",
            paivatAjanjaksoksi(koulutustyypit.yliopistosairaaloidenUlkopuolinenSuoritettu).format()
        )
        setVariable(
            "yliopistosairaaloidenUlkopuolinenVaadittuVahintaan",
            paivatAjanjaksoksi(
                koulutustyypit.yliopistosairaaloidenUlkopuolinenVaadittuVahintaan
            ).format()
        )
    }

    private fun Context.lisaaYekTyoskentelytilastot(tilastot: TyoskentelyjaksotTilastotDTO) {
        lisaaYhteisetTyoskentelytilastot(tilastot)
        val koulutustyypit = tilastot.koulutustyypit
        setVariable(
            "sairaalaSuoritettu",
            paivatAjanjaksoksi(koulutustyypit.yliopistosairaalaSuoritettu).format()
        )
        setVariable(
            "sairaalaVaadittuVahintaan",
            paivatAjanjaksoksi(koulutustyypit.yliopistosairaalaVaadittuVahintaan).format()
        )
        setVariable(
            "muuSuoritettu",
            paivatAjanjaksoksi(koulutustyypit.yliopistosairaaloidenUlkopuolinenSuoritettu).format()
        )
        setVariable(
            "muuVaadittuVahintaan",
            paivatAjanjaksoksi(
                koulutustyypit.yliopistosairaaloidenUlkopuolinenVaadittuVahintaan
            ).format()
        )
    }

    private fun Context.lisaaYhteisetTyoskentelytilastot(
        tilastot: TyoskentelyjaksotTilastotDTO
    ) {
        val koulutustyypit = tilastot.koulutustyypit
        setVariable(
            "tyoskentelyaikaYhteensa",
            paivatAjanjaksoksi(koulutustyypit.yhteensaSuoritettu).format()
        )
        setVariable(
            "arvioErikoistumiseenHyvaksyttavista",
            paivatAjanjaksoksi(tilastot.arvioErikoistumiseenHyvaksyttavista).format()
        )
        setVariable(
            "arvioPuuttuvastaKoulutuksesta",
            paivatAjanjaksoksi(tilastot.arvioPuuttuvastaKoulutuksesta).format()
        )
        setVariable(
            "terveyskeskusSuoritettu",
            paivatAjanjaksoksi(koulutustyypit.terveyskeskusSuoritettu).format()
        )
        setVariable(
            "terveyskeskusVaadittuVahintaan",
            paivatAjanjaksoksi(koulutustyypit.terveyskeskusVaadittuVahintaan).format()
        )
        setVariable(
            "yhteensaSuoritettu",
            paivatAjanjaksoksi(koulutustyypit.yhteensaSuoritettu).format()
        )
        setVariable(
            "yhteensaVaadittuVahintaan",
            paivatAjanjaksoksi(koulutustyypit.yhteensaVaadittuVahintaan).format()
        )
    }

    private fun Context.lisaaKaytannonKoulutus(
        tyyppi: KaytannonKoulutusTyyppi?,
        suoritettu: Double,
        yhteensa: Double
    ) {
        val (suoritettuMuuttuja, osuusMuuttuja) = when (tyyppi) {
            KaytannonKoulutusTyyppi.OMAN_ERIKOISALAN_KOULUTUS ->
                "omaErikoisalaSuoritettu" to "omaErikoisalaOsuus"
            KaytannonKoulutusTyyppi.OMAA_ERIKOISALAA_TUKEVA_KOULUTUS ->
                "omaaErikoisalaaTukevaSuoritettu" to "omaErikoisalaaTukevaOsuus"
            KaytannonKoulutusTyyppi.TUTKIMUSTYO ->
                "tutkimustyoSuoritettu" to "tutkimustyoOsuus"
            KaytannonKoulutusTyyppi.TERVEYSKESKUSTYO ->
                "terveyskeskustyoSuoritettu" to "terveyskeskustyoOsuus"
            null -> return
        }
        setVariable(suoritettuMuuttuja, paivatAjanjaksoksi(suoritettu).format())
        setVariable(osuusMuuttuja, (suoritettu / yhteensa * PROSENTTIA).toInt())
    }

    private fun tallenna(
        valmistumispyynto: Valmistumispyynto,
        template: String,
        tiedostonimenAlku: String,
        context: Context
    ): Asiakirja {
        val outputStream = ByteArrayOutputStream()
        pdfService.luoPdf(template, context, outputStream)
        val aikaleima = LocalDate.now().format(DateTimeFormatter.ofPattern(PAIVAMAARAFORMAATTI))
        val asiakirja = asiakirjaRepository.save(
            Asiakirja(
                opintooikeus = valmistumispyynto.opintooikeus,
                nimi = "${tiedostonimenAlku}_${aikaleima}.pdf",
                tyyppi = MediaType.APPLICATION_PDF_VALUE,
                lisattypvm = LocalDateTime.now(),
                asiakirjaData = AsiakirjaData(data = outputStream.toByteArray())
            )
        )
        valmistumispyynto.yhteenvetoAsiakirja = asiakirja
        valmistumispyyntoRepository.save(valmistumispyynto)
        return asiakirja
    }

    private fun paivatAjanjaksoksi(paivat: Double) = Period.of(
        paivat.toYears(),
        paivat.toMonths(),
        paivat.toDays()
    )

    private companion object {
        const val PAIVAMAARAFORMAATTI = "yyyyMMdd"
        const val PROSENTTIA = 100
        val SUOMEN_LOCALE: Locale = Locale.forLanguageTag("fi")
    }
}
