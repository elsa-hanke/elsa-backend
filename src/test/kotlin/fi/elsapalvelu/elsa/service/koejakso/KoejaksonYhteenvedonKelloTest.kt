package fi.elsapalvelu.elsa.service.koejakso

import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.koejakso.KoejaksonKoulutussopimus
import fi.elsapalvelu.elsa.repository.koejakso.KoejaksonAloituskeskusteluRepository
import fi.elsapalvelu.elsa.repository.koejakso.KoejaksonKoulutussopimusRepository
import fi.elsapalvelu.elsa.service.dto.enumeration.KoejaksoTila
import fi.elsapalvelu.elsa.service.helpers.KellonTestitapaus
import fi.elsapalvelu.elsa.service.kayttaja.etusivu.EtusivunKoejaksopalvelu
import java.util.Optional
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class KoejaksonYhteenvedonKelloTest {
    @ParameterizedTest
    @MethodSource("tapaukset")
    fun `yhteenveto kayttaa kellon paivaa aloitusrajana`(tapaus: Aloitustapaus) {
        val sopimukset = mock<KoejaksonKoulutussopimusRepository>()
        val aloitukset = mock<KoejaksonAloituskeskusteluRepository>()
        whenever(sopimukset.findByOpintooikeusId(11L)).thenReturn(Optional.of(
            KoejaksonKoulutussopimus(koejaksonAlkamispaiva = tapaus.aika.paiva.plusDays(tapaus.paivia))))
        val palvelu = EtusivunKoejaksopalvelu(sopimukset, mock(), aloitukset, tapaus.aika.kello())

        assertThat(palvelu.haeKoejaksonTila(Opintooikeus(id = 11L), emptySequence()))
            .isEqualTo(if (tapaus.paivia > 0) KoejaksoTila.EI_AKTIIVINEN else KoejaksoTila.ODOTTAA_HYVAKSYNTAA)
    }

    data class Aloitustapaus(val aika: KellonTestitapaus, val paivia: Long)
    companion object {
        @JvmStatic
        fun tapaukset(): List<Aloitustapaus> = KellonTestitapaus.tapaukset().flatMap { aika ->
            listOf(-1L, 0L, 1L).map { Aloitustapaus(aika, it) }
        }
    }
}
