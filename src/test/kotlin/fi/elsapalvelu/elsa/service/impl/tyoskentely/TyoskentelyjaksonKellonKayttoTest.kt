package fi.elsapalvelu.elsa.service.impl.tyoskentely

import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.tyoskentely.Keskeytysaika
import fi.elsapalvelu.elsa.domain.tyoskentely.PoissaolonSyy
import fi.elsapalvelu.elsa.domain.tyoskentely.PoissaolonSyyTyyppi
import fi.elsapalvelu.elsa.domain.tyoskentely.Tyoskentelyjakso
import fi.elsapalvelu.elsa.repository.kayttaja.OpintooikeusRepository
import fi.elsapalvelu.elsa.repository.tyoskentely.TyoskentelyjaksoRepository
import fi.elsapalvelu.elsa.service.dto.kayttaja.AsiakirjaDTO
import fi.elsapalvelu.elsa.service.dto.tyoskentely.TyoskentelyjaksoDTO
import fi.elsapalvelu.elsa.service.dto.tyoskentely.TyoskentelypaikkaDTO
import fi.elsapalvelu.elsa.service.helpers.KellonTestitapaus
import fi.elsapalvelu.elsa.service.mapper.kayttaja.AsiakirjaMapper
import fi.elsapalvelu.elsa.service.mapper.tyoskentely.TyoskentelyjaksoMapper
import java.time.LocalDate
import java.util.Optional
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class TyoskentelyjaksonKellonKayttoTest {
    private val jaksot = mock<TyoskentelyjaksoRepository>()
    private val jaksoMapper = mock<TyoskentelyjaksoMapper>()
    private val asiakirjaMapper = mock<AsiakirjaMapper>()
    private val opintooikeudet = mock<OpintooikeusRepository>()

    @ParameterizedTest
    @MethodSource("paattymistapaukset")
    fun `poissaolon vahennys rajataan kellon paivaan tai aiempaan paattymispaivaan`(tapaus: Paattymistapaus) {
        val jakso = Tyoskentelyjakso(id = 1L, alkamispaiva = tapaus.aika.paiva.minusDays(2), paattymispaiva = tapaus.paattymispaiva, osaaikaprosentti = 50)
        jakso.keskeytykset.add(Keskeytysaika(alkamispaiva = tapaus.aika.paiva.minusDays(2), paattymispaiva = tapaus.aika.paiva.plusDays(3),
            poissaoloprosentti = 100, poissaolonSyy = PoissaolonSyy(vahennystyyppi = PoissaolonSyyTyyppi.VAHENNETAAN_SUORAAN), tyoskentelyjakso = jakso))

        val tulos = palvelu(tapaus.aika).getVahennettavatPaivat(listOf(jakso))

        assertThat(tulos).containsEntry(1L, tapaus.odotettu)
    }

    @ParameterizedTest
    @MethodSource("liitetapaukset")
    fun `jakson luonti ja muokkaus kayttavat kellon aikaa uudelle liitteelle`(tapaus: Liitetapaus) {
        val opintooikeus = Opintooikeus(id = 11L)
        val jakso = Tyoskentelyjakso(id = 1L, opintooikeus = opintooikeus)
        val lomake = TyoskentelyjaksoDTO(id = 1L, tyoskentelypaikka = TyoskentelypaikkaDTO())
        val liitelomake = AsiakirjaDTO(nimi = "todistus.pdf")
        val liite = Asiakirja()
        whenever(opintooikeudet.findById(11L)).thenReturn(Optional.of(opintooikeus))
        whenever(jaksot.findOneByIdAndOpintooikeusId(1L, 11L)).thenReturn(jakso)
        whenever(jaksoMapper.toEntity(lomake)).thenReturn(Tyoskentelyjakso(id = 1L))
        whenever(jaksoMapper.toDto(any<Tyoskentelyjakso>())).thenReturn(lomake)
        whenever(jaksot.save(any<Tyoskentelyjakso>())).thenAnswer { it.arguments[0] }
        whenever(asiakirjaMapper.toEntity(liitelomake)).thenReturn(liite)
        val palvelu = palvelu(tapaus.aika)

        if (tapaus.muokkaus) palvelu.update(lomake, 11L, mutableSetOf(liitelomake), null)
        else palvelu.create(lomake, 11L, mutableSetOf(liitelomake))

        assertThat(liite.lisattypvm).isEqualTo(tapaus.aika.paikallinenAika())
        assertThat(liite.opintooikeus).isSameAs(opintooikeus)
        assertThat(liite.tyoskentelyjakso!!.id).isEqualTo(1L)
    }

    private fun palvelu(aika: KellonTestitapaus) = TyoskentelyjaksoServiceImpl(
        tyoskentelyjaksoRepository = jaksot, kuntaRepository = mock(), erikoisalaRepository = mock(), tyoskentelyjaksoMapper = jaksoMapper,
        tyoskentelyjaksoWithKeskeytysajatMapper = mock(), asiakirjaMapper = asiakirjaMapper,
        tyoskentelyjaksonPituusCounterService = TyoskentelyjaksonPituusCounterServiceImpl(aika.kello()), opintooikeusRepository = opintooikeudet,
        pdfTextFieldValidator = mock(), asiakirjaRepository = mock(), koulutusjaksoRepository = mock(), clock = aika.kello()
    )

    data class Paattymistapaus(val aika: KellonTestitapaus, val paattymispaiva: LocalDate?, val odotettu: Double)
    data class Liitetapaus(val aika: KellonTestitapaus, val muokkaus: Boolean)

    companion object {
        @JvmStatic
        fun paattymistapaukset(): List<Paattymistapaus> = KellonTestitapaus.tapaukset().flatMap { aika ->
            listOf(Paattymistapaus(aika, null, 1.5), Paattymistapaus(aika, aika.paiva.plusDays(3), 1.5), Paattymistapaus(aika, aika.paiva.minusDays(1), 1.0))
        }
        @JvmStatic
        fun liitetapaukset(): List<Liitetapaus> = KellonTestitapaus.tapaukset().flatMap { aika -> listOf(false, true).map { Liitetapaus(aika, it) } }
    }
}
