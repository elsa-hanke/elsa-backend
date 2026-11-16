package fi.elsapalvelu.elsa.service.impl.kayttaja

import fi.elsapalvelu.elsa.domain.kayttaja.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.kayttaja.Kayttaja
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.repository.kayttaja.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.kayttaja.KayttajaRepository
import fi.elsapalvelu.elsa.repository.kayttaja.OpintooikeusRepository
import fi.elsapalvelu.elsa.repository.kayttaja.UserRepository
import fi.elsapalvelu.elsa.service.dto.kayttaja.ErikoistuvaLaakariDTO
import fi.elsapalvelu.elsa.service.dto.kayttajahallinta.KayttajahallintaErikoistuvaLaakariDTO
import fi.elsapalvelu.elsa.service.helpers.KELLON_TESTITAPAUKSET
import fi.elsapalvelu.elsa.service.helpers.KellonTestitapaus
import fi.elsapalvelu.elsa.service.mapper.kayttaja.ErikoistuvaLaakariMapper
import fi.elsapalvelu.elsa.service.perustiedot.ErikoisalaService
import fi.elsapalvelu.elsa.service.dto.perustiedot.ErikoisalaDTO
import fi.elsapalvelu.elsa.service.perustiedot.YliopistoService
import fi.elsapalvelu.elsa.service.dto.perustiedot.YliopistoDTO
import java.util.Optional
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ErikoistuvanLuonninAikaleimaTest {
    @ParameterizedTest
    @MethodSource(KELLON_TESTITAPAUKSET)
    fun `kayttajahallinnassa luotu opintooikeus saa kellon muokkausajan`(aika: KellonTestitapaus) {
        val erikoistujat = mock<ErikoistuvaLaakariRepository>()
        val erikoistujaMapper = mock<ErikoistuvaLaakariMapper>()
        val kayttajat = mock<KayttajaRepository>()
        val tunnukset = mock<UserRepository>()
        val opintooikeudet = mock<OpintooikeusRepository>()
        whenever(tunnukset.save(any<User>())).thenAnswer { (it.arguments[0] as User).apply { id = "erikoistuja" } }
        whenever(kayttajat.save(any<Kayttaja>())).thenAnswer { it.arguments[0] }
        whenever(erikoistujat.save(any<ErikoistuvaLaakari>())).thenAnswer { it.arguments[0] }
        whenever(opintooikeudet.save(any<Opintooikeus>())).thenAnswer { (it.arguments[0] as Opintooikeus).apply { id = 11L } }
        whenever(erikoistujaMapper.toDto(any<ErikoistuvaLaakari>())).thenReturn(ErikoistuvaLaakariDTO())
        val yliopistot = mock<YliopistoService>()
        whenever(yliopistot.findOne(1L)).thenReturn(Optional.of(YliopistoDTO(id = 1L)))
        val erikoisalat = mock<ErikoisalaService>()
        whenever(erikoisalat.findOne(1L)).thenReturn(Optional.of(ErikoisalaDTO(id = 1L)))
        val palvelu = ErikoistuvaLaakariServiceImpl(erikoistujat, erikoistujaMapper, mock(), yliopistot, mock(), erikoisalat, mock(), tunnukset,
            kayttajat, opintooikeudet, mock(), mock(), mock(), mock(), mock(), aika.kello())

        palvelu.save(KayttajahallintaErikoistuvaLaakariDTO(yliopistoId = 1L, erikoisalaId = 1L, asetusId = 1L, opintoopasId = 1L))

        val tallennettu = argumentCaptor<Opintooikeus>()
        verify(opintooikeudet).save(tallennettu.capture())
        assertThat(tallennettu.firstValue.muokkausaika).isEqualTo(aika.aikaleima())
    }
}
