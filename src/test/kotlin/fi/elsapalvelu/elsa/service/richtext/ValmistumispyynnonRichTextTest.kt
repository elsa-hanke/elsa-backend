package fi.elsapalvelu.elsa.service.richtext

import fi.elsapalvelu.elsa.domain.arviointi.ArvioitavaKokonaisuus
import fi.elsapalvelu.elsa.domain.arviointi.SuoritusarvioinninArvioitavaKokonaisuus
import fi.elsapalvelu.elsa.domain.arviointi.Suoritusarviointi
import fi.elsapalvelu.elsa.domain.kayttaja.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.kayttaja.Kayttaja
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.tyoskentely.Tyoskentelyjakso
import fi.elsapalvelu.elsa.repository.arviointi.SuoritusarviointiRepository
import fi.elsapalvelu.elsa.service.dto.kayttaja.KayttajaDTO
import fi.elsapalvelu.elsa.service.dto.tyoskentely.TyoskentelyjaksoDTO
import fi.elsapalvelu.elsa.service.impl.valmistuminen.ValmistumispyynnonArviointitietoService
import fi.elsapalvelu.elsa.service.mapper.kayttaja.KayttajaMapper
import fi.elsapalvelu.elsa.service.mapper.tyoskentely.TyoskentelyjaksoMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

class ValmistumispyynnonRichTextTest {
    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `graduation assessment DTO cleans both descriptions without changing historical entities`(highestOnly: Boolean) {
        val repository = mock<SuoritusarviointiRepository>()
        val userMapper = mock<KayttajaMapper>()
        val periodMapper = mock<TyoskentelyjaksoMapper>()
        whenever(userMapper.toDto(any<Kayttaja>())).thenReturn(KayttajaDTO())
        whenever(periodMapper.toDto(any<Tyoskentelyjakso>())).thenReturn(TyoskentelyjaksoDTO())
        val service = ValmistumispyynnonArviointitietoService(repository, userMapper, periodMapper, mock())
        val dirtyFi = "<p onclick='alert(1)'><strong>Hyvää</strong></p><script>alert(1)</script>"
        val dirtySv = "<h2>Åland</h2><a href='javascript:alert(1)'>Länk</a>"
        for (descriptions in listOf(dirtyFi to dirtySv, null to null)) {
            val block = ArvioitavaKokonaisuus(id = 42, kuvaus = descriptions.first, kuvausSv = descriptions.second)
            val assessment = Suoritusarviointi(
                id = 43,
                arvioinninAntaja = Kayttaja(),
                tyoskentelyjakso = Tyoskentelyjakso(
                    opintooikeus = Opintooikeus(erikoistuvaLaakari = ErikoistuvaLaakari(kayttaja = Kayttaja()))
                )
            )
            assessment.arvioitavatKokonaisuudet.add(SuoritusarvioinninArvioitavaKokonaisuus(
                id = 44, arvioitavaKokonaisuus = block, suoritusarviointi = assessment, arviointiasteikonTaso = 3
            ))
            whenever(repository.findAllByTyoskentelyjaksoOpintooikeusId(7)).thenReturn(listOf(assessment))

            val dto = service.haeKategoriat(7, highestOnly).single().arvioitavatKokonaisuudet!!.single()

            assertThat(dto.kuvaus).isEqualTo(if (descriptions.first == null) null else "<p><strong>Hyvää</strong></p>")
            assertThat(dto.kuvausSv).isEqualTo(if (descriptions.second == null) null else "<h2>Åland</h2><a>Länk</a>")
            assertThat(block.kuvaus to block.kuvausSv).isEqualTo(descriptions)
        }
        verify(repository, times(2)).findAllByTyoskentelyjaksoOpintooikeusId(7)
        verifyNoMoreInteractions(repository)
    }
}
