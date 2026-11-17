package fi.elsapalvelu.elsa.service.impl.arviointi

import fi.elsapalvelu.elsa.domain.arviointi.Suoritusarviointi
import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.kayttaja.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.tyoskentely.Tyoskentelyjakso
import fi.elsapalvelu.elsa.repository.arviointi.SuoritusarviointiRepository
import fi.elsapalvelu.elsa.repository.kayttaja.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.service.dto.arviointi.SuoritusarviointiDTO
import fi.elsapalvelu.elsa.service.dto.kayttaja.AsiakirjaDTO
import fi.elsapalvelu.elsa.service.helpers.KELLON_TESTITAPAUKSET
import fi.elsapalvelu.elsa.service.helpers.KellonTestitapaus
import fi.elsapalvelu.elsa.service.mapper.arviointi.SuoritusarviointiMapper
import fi.elsapalvelu.elsa.service.mapper.kayttaja.AsiakirjaMapper
import java.util.Optional
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ItsearvioinninPaivamaaratTest {
    @ParameterizedTest
    @MethodSource(KELLON_TESTITAPAUKSET)
    fun `itsearviointi ja sen liite kayttavat kellon paivamaaria`(aika: KellonTestitapaus) {
        val arviot = mock<SuoritusarviointiRepository>()
        val erikoistujat = mock<ErikoistuvaLaakariRepository>()
        val arvioMapper = mock<SuoritusarviointiMapper>()
        val asiakirjaMapper = mock<AsiakirjaMapper>()
        val erikoistuja = ErikoistuvaLaakari(id = 5L)
        val arvio = Suoritusarviointi(id = 1L, tyoskentelyjakso = Tyoskentelyjakso(opintooikeus = Opintooikeus(erikoistuvaLaakari = erikoistuja)))
        val lomake = SuoritusarviointiDTO(id = 1L, sanallinenItsearviointi = "Oppimistavoite saavutettu")
        val liitelomake = AsiakirjaDTO(nimi = "itsearviointi.pdf")
        val liite = Asiakirja()
        whenever(arviot.findOneById(1L)).thenReturn(Optional.of(arvio))
        whenever(arviot.save(arvio)).thenReturn(arvio)
        whenever(erikoistujat.findOneByKayttajaUserId("erikoistuja")).thenReturn(erikoistuja)
        whenever(arvioMapper.toDto(arvio)).thenReturn(lomake)
        whenever(asiakirjaMapper.toEntity(liitelomake)).thenReturn(liite)
        val palvelu = SuoritusarviointiServiceImpl(arviot, erikoistujat, mock(), mock(), arvioMapper, mock(), mock(), mock(), mock(),
            asiakirjaMapper, mock(), mock(), mock(), aika.kello())

        palvelu.save(lomake, mutableSetOf(liitelomake), null, "erikoistuja")

        assertThat(arvio.itsearviointiAika).isEqualTo(aika.paiva)
        assertThat(liite.lisattypvm).isEqualTo(aika.paikallinenAika())
        assertThat(liite.itsearviointi).isSameAs(arvio)
    }
}
