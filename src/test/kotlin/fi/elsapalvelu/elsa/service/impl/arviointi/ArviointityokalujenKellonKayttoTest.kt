package fi.elsapalvelu.elsa.service.impl.arviointi

import fi.elsapalvelu.elsa.domain.arviointi.Arviointityokalu
import fi.elsapalvelu.elsa.domain.arviointi.ArviointityokaluKategoria
import fi.elsapalvelu.elsa.repository.arviointi.ArviointityokaluRepository
import fi.elsapalvelu.elsa.repository.arviointi.ArviointityokaluKategoriaRepository
import fi.elsapalvelu.elsa.service.dto.arviointi.ArviointityokaluDTO
import fi.elsapalvelu.elsa.service.dto.arviointi.ArviointityokaluKategoriaDTO
import fi.elsapalvelu.elsa.service.dto.kayttaja.UserDTO
import fi.elsapalvelu.elsa.service.helpers.KELLON_TESTITAPAUKSET
import fi.elsapalvelu.elsa.service.helpers.KellonTestitapaus
import fi.elsapalvelu.elsa.service.mapper.arviointi.ArviointityokaluMapper
import fi.elsapalvelu.elsa.service.mapper.arviointi.ArviointityokaluKategoriaMapper
import java.time.Instant
import java.util.Optional
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ArviointityokalujenKellonKayttoTest {
    private val tyokalut = mock<ArviointityokaluRepository>()
    private val kategoriat = mock<ArviointityokaluKategoriaRepository>()
    private val tyokaluMapper = mock<ArviointityokaluMapper>()
    private val kategoriaMapper = mock<ArviointityokaluKategoriaMapper>()
    private val aiempiAika: Instant = Instant.parse("2019-01-01T00:00:00Z")

    @ParameterizedTest
    @MethodSource(KELLON_TESTITAPAUKSET)
    fun `uuden arviointityokalun luonti ja muokkausaika tulevat kellosta`(aika: KellonTestitapaus) {
        val lomake = ArviointityokaluDTO()
        val tyokalu = Arviointityokalu()
        whenever(tyokaluMapper.toEntity(lomake)).thenReturn(tyokalu)
        whenever(tyokalut.save(any<Arviointityokalu>())).thenAnswer { (it.arguments[0] as Arviointityokalu).apply { id = 1L } }
        whenever(tyokaluMapper.toDto(tyokalu)).thenReturn(lomake)

        ArviointityokaluServiceImpl(tyokalut, tyokaluMapper, kategoriat, aika.kello()).save(lomake, UserDTO(), null)

        assertThat(tyokalu.luontiaika).isEqualTo(aika.aikaleima())
        assertThat(tyokalu.muokkausaika).isEqualTo(aika.aikaleima())
        assertThat(tyokalu.versio).isEqualTo(1)
        assertThat(tyokalu.alkuperainenId).isEqualTo(1L)
    }

    @ParameterizedTest
    @MethodSource(KELLON_TESTITAPAUKSET)
    fun `uusi tyokaluversio sailyttaa luontiajan ja kayttaa kellon muokkausaikaa`(aika: KellonTestitapaus) {
        val vanha = Arviointityokalu(id = 1L, versio = 2, luontiaika = aiempiAika, muokkausaika = aiempiAika)
        val lomake = ArviointityokaluDTO(id = 1L)
        whenever(tyokalut.findById(1L)).thenReturn(Optional.of(vanha))
        whenever(tyokalut.save(any<Arviointityokalu>())).thenAnswer { it.arguments[0] }
        whenever(tyokaluMapper.toDto(any<Arviointityokalu>())).thenReturn(lomake)

        ArviointityokaluServiceImpl(tyokalut, tyokaluMapper, kategoriat, aika.kello()).update(lomake, null)

        val tallennettu = argumentCaptor<Arviointityokalu>()
        verify(tyokaluMapper).toDto(tallennettu.capture())
        assertThat(tallennettu.firstValue.luontiaika).isEqualTo(aiempiAika)
        assertThat(tallennettu.firstValue.muokkausaika).isEqualTo(aika.aikaleima())
        assertThat(tallennettu.firstValue.versio).isEqualTo(3)
        assertThat(vanha.muokkausaika).isEqualTo(aiempiAika)
        assertThat(vanha.kaytossa).isFalse()
    }

    @ParameterizedTest
    @MethodSource(KELLON_TESTITAPAUKSET)
    fun `kategorian luonti kayttaa kellon aikaleimaa molempiin kenttiin`(aika: KellonTestitapaus) {
        val lomake = ArviointityokaluKategoriaDTO()
        val kategoria = ArviointityokaluKategoria()
        whenever(kategoriaMapper.toEntity(lomake)).thenReturn(kategoria)
        whenever(kategoriat.save(kategoria)).thenReturn(kategoria)
        whenever(kategoriaMapper.toDto(kategoria)).thenReturn(lomake)

        ArviointityokaluKategoriaServiceImpl(kategoriat, kategoriaMapper, tyokalut, aika.kello()).create(lomake)

        assertThat(kategoria.luontiaika).isEqualTo(aika.aikaleima())
        assertThat(kategoria.muokkausaika).isEqualTo(aika.aikaleima())
    }

    @ParameterizedTest
    @MethodSource(KELLON_TESTITAPAUKSET)
    fun `kategorian nimen muokkaus sailyttaa nykyisen aikaleimakaytannon`(aika: KellonTestitapaus) {
        val lomake = ArviointityokaluKategoriaDTO(id = 1L, nimi = "Uusi nimi")
        val kategoria = ArviointityokaluKategoria(id = 1L, luontiaika = aiempiAika, muokkausaika = aiempiAika)
        whenever(kategoriat.findById(1L)).thenReturn(Optional.of(kategoria))
        whenever(kategoriat.save(kategoria)).thenReturn(kategoria)
        whenever(kategoriaMapper.toDto(kategoria)).thenReturn(lomake)

        ArviointityokaluKategoriaServiceImpl(kategoriat, kategoriaMapper, tyokalut, aika.kello()).update(lomake)

        assertThat(kategoria.luontiaika).isEqualTo(aiempiAika)
        assertThat(kategoria.muokkausaika).isEqualTo(aiempiAika)
        assertThat(kategoria.nimi).isEqualTo("Uusi nimi")
    }
}
