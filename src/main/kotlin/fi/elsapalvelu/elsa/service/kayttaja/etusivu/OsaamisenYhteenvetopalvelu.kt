package fi.elsapalvelu.elsa.service.kayttaja.etusivu

import fi.elsapalvelu.elsa.domain.arviointi.ArvioitavaKokonaisuus
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.suoritteet.Suorite
import fi.elsapalvelu.elsa.domain.suoritteet.Suoritemerkinta
import fi.elsapalvelu.elsa.repository.arviointi.ArvioitavanKokonaisuudenKategoriaRepository
import fi.elsapalvelu.elsa.repository.arviointi.SuoritusarviointiRepository
import fi.elsapalvelu.elsa.repository.suoritteet.SuoritemerkintaRepository
import fi.elsapalvelu.elsa.repository.suoritteet.SuoritteenKategoriaRepository
import fi.elsapalvelu.elsa.required
import java.time.LocalDate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class OsaamisenYhteenvetopalvelu(
    private val suoritusarviointiRepository: SuoritusarviointiRepository,
    private val arvioitavanKokonaisuudenKategoriaRepository: ArvioitavanKokonaisuudenKategoriaRepository,
    private val suoritemerkintaRepository: SuoritemerkintaRepository,
    private val suoritteenKategoriaRepository: SuoritteenKategoriaRepository
) {

    fun haeKokonaisuuksienParhaatArviot(opintooikeusId: Long): Map<ArvioitavaKokonaisuus, Int> {
        return suoritusarviointiRepository.findAllByTyoskentelyjaksoOpintooikeusId(opintooikeusId)
            .asSequence()
            .flatMap { arviointi -> arviointi.arvioitavatKokonaisuudet }
            .filter { kokonaisuus -> kokonaisuus.arviointiasteikonTaso != null }
            .groupBy { kokonaisuus -> kokonaisuus.arvioitavaKokonaisuus.required() }
            .mapValues { it.value.maxOf { arviointi -> arviointi.arviointiasteikonTaso.required() } }
    }

    fun laskeArviointienKeskiarvo(kokonaisuuksienArviot: Map<ArvioitavaKokonaisuus, Int>): Double? =
        if (kokonaisuuksienArviot.isNotEmpty()) kokonaisuuksienArviot.values.sumOf { it } / kokonaisuuksienArviot.keys.size.toDouble() else null

    fun laskeArvioitavatKokonaisuudet(
        opintooikeus: Opintooikeus,
        arvioidutKokonaisuudet: Set<ArvioitavaKokonaisuus>
    ): Int {
        var arvioitavatKokonaisuudetLkm =
            arvioitavanKokonaisuudenKategoriaRepository.findAllByErikoisalaIdAndValid(
                opintooikeus.erikoisala?.id,
                opintooikeus.osaamisenArvioinninOppaanPvm.required()
            ).map { it.arvioitavatKokonaisuudet }.flatten().count {
                onkoVoimassa(
                    it.voimassaoloAlkaa.required(),
                    it.voimassaoloLoppuu,
                    opintooikeus.osaamisenArvioinninOppaanPvm.required()
                )
            }

        // Lisätään arvioitava kokonaisuus mukaan kokonaismäärään voimassaolosta huolimatta, jos siihen kohdistuu arviointi.
        arvioidutKokonaisuudet.forEach {
            val arvioitavaKokonaisuusVoimassa = onkoVoimassa(
                it.voimassaoloAlkaa.required(),
                it.voimassaoloLoppuu,
                opintooikeus.osaamisenArvioinninOppaanPvm.required()
            )
            if (!arvioitavaKokonaisuusVoimassa) {
                arvioitavatKokonaisuudetLkm++
            }
        }

        return arvioitavatKokonaisuudetLkm
    }

    fun haeSuoritemerkinnat(opintooikeusId: Long): Map<Suorite, List<Suoritemerkinta>> =
        suoritemerkintaRepository.findAllByTyoskentelyjaksoOpintooikeusId(opintooikeusId).groupBy { merkinta -> merkinta.suorite.required() }

    fun laskeSuoritemerkinnat(suoritemerkinnatSuoritteittain: Map<Suorite, List<Suoritemerkinta>>): Int {
        var lkm = 0
        suoritemerkinnatSuoritteittain.forEach {
            // Mikäli vaadittu määrä asetettu, ei huomioida sen yli meneviä suoritemerkintöjä.
            val merkinnat =
                if (it.key.vaadittulkm != null && it.value.size > it.key.vaadittulkm.required()) it.key.vaadittulkm.required()
                else it.value.size
            lkm = lkm.plus(merkinnat)
        }
        return lkm
    }

    fun laskeVaaditutSuoritemerkinnat(opintooikeus: Opintooikeus, suoritemerkinnat: List<Suoritemerkinta>): Int {
        val voimassaOlevatKategoriat = suoritteenKategoriaRepository.findAllByErikoisalaIdAndValid(opintooikeus.erikoisala?.id,
            opintooikeus.osaamisenArvioinninOppaanPvm.required())

        var vaaditutSuoritteetLkm = voimassaOlevatKategoriat.sumOf { kategoria ->
            kategoria.suoritteet.filter { suorite ->
                onkoVoimassa(
                    suorite.voimassaolonAlkamispaiva.required(),
                    suorite.voimassaolonPaattymispaiva,
                    opintooikeus.osaamisenArvioinninOppaanPvm.required()
                ) && suorite.vaadittulkm != null
            }
                .sumOf { suorite -> suorite.vaadittulkm.required() }
        }
        suoritemerkinnat.forEach {
            // Oppaan päivänä voimassa olemattoman suoritteen vaatimus kasvaa yhdellä jokaista merkintää kohti,
            // jos suoritteen vaadittu määrä on asetettu.
            if (suoriteEiOleVoimassa(it.suorite.required(), opintooikeus.osaamisenArvioinninOppaanPvm.required()) && it.suorite?.vaadittulkm != null) {
                vaaditutSuoritteetLkm++
            }
        }

        return vaaditutSuoritteetLkm
    }

    fun laskeSuoritetutOsaalueet(
        suoritemerkinnatSuoritteittain: Map<Suorite, List<Suoritemerkinta>>,
        osaamisenArvioinninOppaanPvm: LocalDate
    ): Int {
        var lkm = 0
        // Kun vaadittu määrä on asetettu, oppaan päivänä voimassa olematon suorite lasketaan suoritetuksi jo yhdellä merkinnällä.
        suoritemerkinnatSuoritteittain.forEach {
            if (it.key.vaadittulkm != null && (it.value.size >= it.key.vaadittulkm.required() || suoriteEiOleVoimassa(
                    it.key,
                    osaamisenArvioinninOppaanPvm
                ))
            ) {
                lkm++
            }
        }

        return lkm
    }

    fun laskeVaaditutOsaalueet(
        opintooikeus: Opintooikeus,
        suoritemerkinnatSuoritteittain: Map<Suorite, List<Suoritemerkinta>>
    ): Int {
        var suoriteOsaalueetVaadittuLkm =
            suoritteenKategoriaRepository.findAllByErikoisalaIdAndValid(
                opintooikeus.erikoisala?.id,
                opintooikeus.osaamisenArvioinninOppaanPvm.required()
            ).map { kategoria ->
                kategoria.suoritteet.filter {
                    onkoVoimassa(
                        it.voimassaolonAlkamispaiva.required(),
                        it.voimassaolonPaattymispaiva,
                        opintooikeus.osaamisenArvioinninOppaanPvm.required()
                    ) && it.vaadittulkm != null
                }
            }.flatten().size

        suoritemerkinnatSuoritteittain.keys.forEach {
            // Otetaan osa-alue (suorite, jolle asetettu vaadittu lkm)
            // mukaan laskentaan voimassaolosta huolimatta, jos siihen kohdistuu suoritemerkintä.
            if (suoriteEiOleVoimassa(
                    it,
                    opintooikeus.osaamisenArvioinninOppaanPvm.required()
                ) && it.vaadittulkm != null
            ) {
                suoriteOsaalueetVaadittuLkm++
            }
        }

        return suoriteOsaalueetVaadittuLkm
    }

    private fun suoriteEiOleVoimassa(suorite: Suorite, osaamisenArvioinninOppaanPvm: LocalDate): Boolean {
        val suoriteVoimassa = onkoVoimassa(suorite.voimassaolonAlkamispaiva.required(), suorite.voimassaolonPaattymispaiva, osaamisenArvioinninOppaanPvm)
        return !suoriteVoimassa
    }

    private fun onkoVoimassa(voimassaoloAlkaa: LocalDate, voimassaoloPaattyy: LocalDate?, osaamisenArvioinninOppaanPvm: LocalDate): Boolean =
        voimassaoloAlkaa <= osaamisenArvioinninOppaanPvm && (voimassaoloPaattyy == null || voimassaoloPaattyy >= osaamisenArvioinninOppaanPvm)
}
