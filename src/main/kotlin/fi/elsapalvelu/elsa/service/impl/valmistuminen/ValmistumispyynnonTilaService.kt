package fi.elsapalvelu.elsa.service.impl.valmistuminen

import fi.elsapalvelu.elsa.config.YEK_ERIKOISALA_ID
import fi.elsapalvelu.elsa.domain.kayttaja.Kayttaja
import fi.elsapalvelu.elsa.domain.perustiedot.VastuuhenkilonTehtavatyyppiEnum
import fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto
import fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto.Companion.fromValmistumispyyntoArvioija
import fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto.Companion.fromValmistumispyyntoArvioijaHyvaksyja
import fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto.Companion.fromValmistumispyyntoHyvaksyja
import fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto.Companion.fromValmistumispyyntoVirkailija
import fi.elsapalvelu.elsa.required
import fi.elsapalvelu.elsa.service.dto.enumeration.ValmistumispyynnonHyvaksyjaRole
import fi.elsapalvelu.elsa.service.dto.enumeration.ValmistumispyynnonTila
import fi.elsapalvelu.elsa.service.dto.valmistuminen.ValmistumispyyntoListItemDTO
import org.springframework.stereotype.Component

@Component
class ValmistumispyynnonTilaService {

    fun haeTilaOsaamisenArvioijalle(
        valmistumispyynto: Valmistumispyynto
    ): ValmistumispyynnonTila {
        val avoin = valmistumispyynto.erikoistujanKuittausaika != null &&
            valmistumispyynto.vastuuhenkiloOsaamisenArvioijaKuittausaika == null &&
            valmistumispyynto.vastuuhenkiloOsaamisenArvioijaPalautusaika == null
        return fromValmistumispyyntoArvioija(valmistumispyynto, avoin)
    }

    fun haeTilaVirkailijalle(valmistumispyynto: Valmistumispyynto): ValmistumispyynnonTila {
        val avoin = valmistumispyynto.erikoistujanKuittausaika != null &&
            (valmistumispyynto.isYek() ||
                valmistumispyynto.vastuuhenkiloOsaamisenArvioijaKuittausaika != null) &&
            valmistumispyynto.virkailijanKuittausaika == null &&
            valmistumispyynto.virkailijanPalautusaika == null
        return fromValmistumispyyntoVirkailija(valmistumispyynto, avoin)
    }

    fun haeTilaHyvaksyjalle(valmistumispyynto: Valmistumispyynto): ValmistumispyynnonTila {
        val avoin = valmistumispyynto.virkailijanKuittausaika != null &&
            valmistumispyynto.vastuuhenkiloHyvaksyjaKuittausaika == null &&
            valmistumispyynto.vastuuhenkiloHyvaksyjaPalautusaika == null
        return fromValmistumispyyntoHyvaksyja(valmistumispyynto, avoin)
    }

    fun mapVastuuhenkilonListItem(
        valmistumispyynto: Valmistumispyynto,
        roolit: List<Pair<Long, ValmistumispyynnonHyvaksyjaRole>>,
        avoin: Boolean
    ): ValmistumispyyntoListItemDTO {
        val rooli = if (valmistumispyynto.isYek()) {
            ValmistumispyynnonHyvaksyjaRole.VASTUUHENKILO_HYVAKSYJA
        } else {
            roolit.first { it.first == valmistumispyynto.opintooikeus?.erikoisala?.id }.second
        }
        return mapListItem(valmistumispyynto, rooli, avoin)
    }

    fun mapVirkailijanListItem(
        valmistumispyynto: Valmistumispyynto,
        avoin: Boolean
    ): ValmistumispyyntoListItemDTO = mapListItem(
        valmistumispyynto,
        ValmistumispyynnonHyvaksyjaRole.VIRKAILIJA,
        avoin
    )

    fun haeVastuuhenkilonRoolit(
        kayttaja: Kayttaja,
        yek: Boolean = false
    ): List<Pair<Long, ValmistumispyynnonHyvaksyjaRole>> =
        kayttaja.yliopistotAndErikoisalat.mapNotNull { yliopistoErikoisala ->
            val tehtavat = yliopistoErikoisala.vastuuhenkilonTehtavat.map { it.nimi }
            val rooli = when {
                yek && tehtavat.contains(VastuuhenkilonTehtavatyyppiEnum.YEK_VALMISTUMINEN) ->
                    ValmistumispyynnonHyvaksyjaRole.VASTUUHENKILO_OSAAMISEN_ARVIOIJA_HYVAKSYJA

                yek -> null
                tehtavat.containsAll(
                    listOf(
                        VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI,
                        VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_HYVAKSYNTA
                    )
                ) -> ValmistumispyynnonHyvaksyjaRole.VASTUUHENKILO_OSAAMISEN_ARVIOIJA_HYVAKSYJA

                tehtavat.contains(VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI) ->
                    ValmistumispyynnonHyvaksyjaRole.VASTUUHENKILO_OSAAMISEN_ARVIOIJA

                tehtavat.contains(VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_HYVAKSYNTA) ->
                    ValmistumispyynnonHyvaksyjaRole.VASTUUHENKILO_HYVAKSYJA

                else -> null
            }
            rooli?.let {
                Pair(
                    if (yek) YEK_ERIKOISALA_ID else yliopistoErikoisala.erikoisala?.id.required(),
                    it
                )
            }
        }

    private fun mapListItem(
        valmistumispyynto: Valmistumispyynto,
        rooli: ValmistumispyynnonHyvaksyjaRole,
        avoin: Boolean
    ): ValmistumispyyntoListItemDTO {
        val tila = when (rooli) {
            ValmistumispyynnonHyvaksyjaRole.VASTUUHENKILO_OSAAMISEN_ARVIOIJA ->
                fromValmistumispyyntoArvioija(valmistumispyynto, avoin)

            ValmistumispyynnonHyvaksyjaRole.VASTUUHENKILO_HYVAKSYJA ->
                fromValmistumispyyntoHyvaksyja(valmistumispyynto, avoin)

            ValmistumispyynnonHyvaksyjaRole.VIRKAILIJA ->
                fromValmistumispyyntoVirkailija(valmistumispyynto, avoin)

            ValmistumispyynnonHyvaksyjaRole.VASTUUHENKILO_OSAAMISEN_ARVIOIJA_HYVAKSYJA ->
                fromValmistumispyyntoArvioijaHyvaksyja(valmistumispyynto, avoin)
        }
        return ValmistumispyyntoListItemDTO(
            id = valmistumispyynto.id,
            erikoistujanNimi =
                valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi(),
            tila = tila,
            tapahtumanAjankohta = haePalautusaika(valmistumispyynto, tila)
                ?: valmistumispyynto.muokkauspaiva,
            isAvoinForCurrentKayttaja = avoin,
            rooli = rooli
        )
    }

    private fun haePalautusaika(
        valmistumispyynto: Valmistumispyynto,
        tila: ValmistumispyynnonTila
    ) = when (tila) {
        ValmistumispyynnonTila.VASTUUHENKILON_TARKASTUS_PALAUTETTU ->
            valmistumispyynto.vastuuhenkiloOsaamisenArvioijaPalautusaika

        ValmistumispyynnonTila.VIRKAILIJAN_TARKASTUS_PALAUTETTU ->
            valmistumispyynto.virkailijanPalautusaika

        ValmistumispyynnonTila.VASTUUHENKILON_HYVAKSYNTA_PALAUTETTU ->
            valmistumispyynto.vastuuhenkiloHyvaksyjaPalautusaika

        else -> null
    }

    private fun Valmistumispyynto.isYek() =
        opintooikeus?.erikoisala?.id == YEK_ERIKOISALA_ID
}
