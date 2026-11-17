package fi.elsapalvelu.elsa.service.koejakso

import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.koejakso.KoejaksonAloituskeskustelu
import fi.elsapalvelu.elsa.domain.koejakso.KoejaksonKoulutussopimus
import fi.elsapalvelu.elsa.domain.koejakso.KoejaksonVastuuhenkilonArvio
import fi.elsapalvelu.elsa.domain.koulutus.Opintosuoritus
import fi.elsapalvelu.elsa.domain.koulutus.OpintosuoritusTyyppi
import fi.elsapalvelu.elsa.domain.koulutus.OpintosuoritusTyyppiEnum
import fi.elsapalvelu.elsa.repository.koejakso.KoejaksonAloituskeskusteluRepository
import fi.elsapalvelu.elsa.repository.koejakso.KoejaksonKoulutussopimusRepository
import fi.elsapalvelu.elsa.repository.koejakso.KoejaksonVastuuhenkilonArvioRepository
import fi.elsapalvelu.elsa.service.dto.enumeration.KoejaksoTila
import fi.elsapalvelu.elsa.service.dto.enumeration.KoejaksoTila.*
import fi.elsapalvelu.elsa.service.kayttaja.etusivu.EtusivunKoejaksopalvelu
import java.time.LocalDate
import java.util.Optional
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.time.Clock

class KoejaksonYhteenvetotilaTest {
    private val sopimukset = mock<KoejaksonKoulutussopimusRepository>()
    private val arviot = mock<KoejaksonVastuuhenkilonArvioRepository>()
    private val aloitukset = mock<KoejaksonAloituskeskusteluRepository>()
    private val palvelu = EtusivunKoejaksopalvelu(sopimukset, arviot, aloitukset, Clock.systemDefaultZone())
    private val opintooikeus = Opintooikeus(id = 11L)

    @ParameterizedTest
    @MethodSource("yhteenvetotapaukset")
    fun `yhteenveto erottaa myonteisen tuloksen kasittelyn hyvaksynnasta ja huomioi aloituspaivan`(tapaus: Yhteenvetotapaus) {
        val tanaan = LocalDate.now()
        whenever(sopimukset.findByOpintooikeusId(11L)).thenReturn(if (tapaus.sopimusOlemassa) Optional.of(
            KoejaksonKoulutussopimus(koejaksonAlkamispaiva = tapaus.sopimuksenPaivia?.let { tanaan.plusDays(it.toLong()) })
        ) else Optional.empty())
        whenever(aloitukset.findByOpintooikeusId(11L)).thenReturn(if (tapaus.aloitusOlemassa) Optional.of(
            KoejaksonAloituskeskustelu(koejaksonAlkamispaiva = tapaus.aloituksenPaivia?.let { tanaan.plusDays(it.toLong()) })
        ) else Optional.empty())
        whenever(arviot.findByOpintooikeusId(11L)).thenReturn(tapaus.hyvaksytty?.let {
            Optional.of(KoejaksonVastuuhenkilonArvio(koejaksoHyvaksytty = it, vastuuhenkiloHyvaksynyt = true))
        } ?: Optional.empty())

        assertThat(palvelu.haeKoejaksonTila(opintooikeus, emptySequence())).isEqualTo(tapaus.odotettu)
    }

    @Test
    fun `hyvaksytty koejaksosuoritus ohittaa vaiheet ilman tietokantahakuja`() {
        val suoritus = Opintosuoritus(tyyppi = OpintosuoritusTyyppi(nimi = OpintosuoritusTyyppiEnum.KOEJAKSO), hyvaksytty = true)

        assertThat(palvelu.haeKoejaksonTila(opintooikeus, sequenceOf(suoritus))).isEqualTo(HYVAKSYTTY)
        verifyNoInteractions(sopimukset, arviot, aloitukset)
    }

    @Test
    fun `hylatty koejaksosuoritus ja muuntyyppinen hyvaksytty suoritus eivat riita`() {
        val hylatty = Opintosuoritus(tyyppi = OpintosuoritusTyyppi(nimi = OpintosuoritusTyyppiEnum.KOEJAKSO), hyvaksytty = false)
        val muu = Opintosuoritus(tyyppi = OpintosuoritusTyyppi(nimi = OpintosuoritusTyyppiEnum.YEK_PATEVYYS), hyvaksytty = true)

        assertThat(palvelu.haeKoejaksonTila(opintooikeus, sequenceOf(hylatty, muu))).isEqualTo(EI_AKTIIVINEN)
    }

    data class Yhteenvetotapaus(
        val kuvaus: String,
        val odotettu: KoejaksoTila,
        val sopimusOlemassa: Boolean = false,
        val sopimuksenPaivia: Int? = null,
        val aloitusOlemassa: Boolean = false,
        val aloituksenPaivia: Int? = null,
        val hyvaksytty: Boolean? = null
    ) {
        override fun toString() = kuvaus
    }

    companion object {
        @JvmStatic
        fun yhteenvetotapaukset(): List<Yhteenvetotapaus> = listOf(
            Yhteenvetotapaus("puuttuvat vaiheet", EI_AKTIIVINEN),
            Yhteenvetotapaus("myonteinen arvio ohittaa tulevan aloituksen", HYVAKSYTTY, sopimusOlemassa = true, sopimuksenPaivia = 1, hyvaksytty = true),
            Yhteenvetotapaus("kasitelty kielteinen arvio ei ole hyvaksytty koejakso", EI_AKTIIVINEN, hyvaksytty = false),
            Yhteenvetotapaus("eilinen sopimus", ODOTTAA_HYVAKSYNTAA, sopimusOlemassa = true, sopimuksenPaivia = -1),
            Yhteenvetotapaus("taman paivan sopimus", ODOTTAA_HYVAKSYNTAA, sopimusOlemassa = true, sopimuksenPaivia = 0),
            Yhteenvetotapaus("huomisen sopimus", EI_AKTIIVINEN, sopimusOlemassa = true, sopimuksenPaivia = 1),
            Yhteenvetotapaus("sopimuksen null paiva tulkitaan alkaneeksi", ODOTTAA_HYVAKSYNTAA, sopimusOlemassa = true),
            Yhteenvetotapaus("eilinen aloitus", ODOTTAA_HYVAKSYNTAA, aloitusOlemassa = true, aloituksenPaivia = -1),
            Yhteenvetotapaus("taman paivan aloitus", ODOTTAA_HYVAKSYNTAA, aloitusOlemassa = true, aloituksenPaivia = 0),
            Yhteenvetotapaus("huomisen aloitus", EI_AKTIIVINEN, aloitusOlemassa = true, aloituksenPaivia = 1),
            Yhteenvetotapaus("aloituksen null paiva tulkitaan alkaneeksi", ODOTTAA_HYVAKSYNTAA, aloitusOlemassa = true),
            Yhteenvetotapaus("aloitus riittaa tulevasta sopimuksesta huolimatta", ODOTTAA_HYVAKSYNTAA,
                sopimusOlemassa = true, sopimuksenPaivia = 1, aloitusOlemassa = true, aloituksenPaivia = 0),
            Yhteenvetotapaus("sopimus riittaa tulevasta aloituksesta huolimatta", ODOTTAA_HYVAKSYNTAA,
                sopimusOlemassa = true, sopimuksenPaivia = 0, aloitusOlemassa = true, aloituksenPaivia = 1)
        )
    }
}
