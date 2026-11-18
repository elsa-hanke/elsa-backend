    package fi.elsapalvelu.elsa.service.tyoskentely

import fi.elsapalvelu.elsa.domain.tyoskentely.Keskeytysaika
import fi.elsapalvelu.elsa.domain.tyoskentely.PoissaolonSyyTyyppi
import fi.elsapalvelu.elsa.domain.tyoskentely.Tyoskentelyjakso
import fi.elsapalvelu.elsa.extensions.isInRange
import fi.elsapalvelu.elsa.required
import fi.elsapalvelu.elsa.service.dto.tyoskentely.HyvaksiluettavatCounterData
import java.time.LocalDate

/** Työskentelyajan osuus yhdeltä päivältä; 1.0 vastaa täyttä työaikaa. */
object PaivittaisenTyoskentelyajanLaskuri {
    fun laskeOsuus(
        tyoskentelyjaksot: List<Tyoskentelyjakso>,
        paiva: LocalDate,
        uudenJaksonOsuus: Double?,
        haeHyvaksilukusaldo: () -> HyvaksiluettavatCounterData
    ): Double {
        val paivanJaksot = tyoskentelyjaksot.filter { paiva.isInRange(it.alkamispaiva.required(), it.paattymispaiva) }
        if (paivanJaksot.isEmpty()) return 0.0

        var tyoskentelyajanOsuus = paivanJaksot.sumOf { it.osaaikaprosentti.required().toDouble() / 100.0 }
        if (uudenJaksonOsuus != null) tyoskentelyajanOsuus += uudenJaksonOsuus
        // Hyväksilukua kulutetaan vain päiviltä, joiden työaika ylittää 100 % ennen poissaoloja.
        if (tyoskentelyajanOsuus <= 1.0) return tyoskentelyajanOsuus

        // Peräkkäisten Double-vähennysten järjestys säilytetään 100 %:n raja-arvon vuoksi.
        for (jakso in paivanJaksot) {
            val paivanKeskeytykset = jakso.keskeytykset.filter { paiva.isInRange(it.alkamispaiva.required(), it.paattymispaiva) }
            for (keskeytys in paivanKeskeytykset) {
                tyoskentelyajanOsuus -= laskePoissaolonVahennys(keskeytys, jakso, paiva, haeHyvaksilukusaldo)
            }
        }
        return tyoskentelyajanOsuus
    }

    private fun laskePoissaolonVahennys(
        keskeytysaika: Keskeytysaika,
        tyoskentelyjakso: Tyoskentelyjakso,
        paiva: LocalDate,
        haeHyvaksilukusaldo: () -> HyvaksiluettavatCounterData
    ): Double {
        val poissaoloOsuus = keskeytysaika.poissaoloprosentti.required().toDouble() / 100.0 *
            (tyoskentelyjakso.osaaikaprosentti.required().toDouble() / 100.0)
        val poissaolonSyy = keskeytysaika.poissaolonSyy.required()
        return when (poissaolonSyy.vahennystyyppi) {
            PoissaolonSyyTyyppi.VAHENNETAAN_SUORAAN -> poissaoloOsuus
            PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI ->
                PoissaolonHyvaksilukulaskuri.laskePaivittaisestaTyoskentelyajastaVahennettavaOsuus(
                    poissaoloOsuus, paiva.year, poissaolonSyy, haeHyvaksilukusaldo()
                )
            else -> 0.0
        }
    }
}
