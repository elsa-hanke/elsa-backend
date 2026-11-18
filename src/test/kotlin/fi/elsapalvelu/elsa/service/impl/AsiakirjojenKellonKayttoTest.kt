package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.kayttaja.AsiakirjaData
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.koulutus.Koulutussuunnitelma
import fi.elsapalvelu.elsa.domain.koulutus.Teoriakoulutus
import fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto
import fi.elsapalvelu.elsa.repository.kayttaja.AsiakirjaRepository
import fi.elsapalvelu.elsa.repository.kayttaja.OpintooikeusRepository
import fi.elsapalvelu.elsa.repository.koulutus.KoulutussuunnitelmaRepository
import fi.elsapalvelu.elsa.repository.koulutus.TeoriakoulutusRepository
import fi.elsapalvelu.elsa.repository.valmistuminen.ValmistumispyyntoRepository
import fi.elsapalvelu.elsa.service.dto.kayttaja.AsiakirjaDTO
import fi.elsapalvelu.elsa.service.dto.kayttaja.AsiakirjaDataDTO
import fi.elsapalvelu.elsa.service.dto.koulutus.KoulutussuunnitelmaDTO
import fi.elsapalvelu.elsa.service.dto.koulutus.TeoriakoulutusDTO
import fi.elsapalvelu.elsa.service.helpers.KELLON_TESTITAPAUKSET
import fi.elsapalvelu.elsa.service.helpers.KellonTestitapaus
import fi.elsapalvelu.elsa.service.impl.kayttaja.AsiakirjaServiceImpl
import fi.elsapalvelu.elsa.service.impl.koulutus.KoulutussuunnitelmaServiceImpl
import fi.elsapalvelu.elsa.service.impl.koulutus.TeoriakoulutusServiceImpl
import fi.elsapalvelu.elsa.service.impl.valmistuminen.ValmistumispyynnonAsiakirjanTallennusService
import fi.elsapalvelu.elsa.service.mapper.kayttaja.AsiakirjaMapper
import fi.elsapalvelu.elsa.service.mapper.koulutus.KoulutussuunnitelmaMapper
import fi.elsapalvelu.elsa.service.mapper.koulutus.TeoriakoulutusMapper
import java.time.LocalDateTime
import java.util.Optional
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class AsiakirjojenKellonKayttoTest {
    private val asiakirjat = mock<AsiakirjaRepository>()
    private val asiakirjaMapper = mock<AsiakirjaMapper>()
    private val opintooikeudet = mock<OpintooikeusRepository>()
    private val opintooikeus = Opintooikeus(id = 11L)
    private val sisalto = byteArrayOf(1, 2, 3)
    private val aiempiAika: LocalDateTime = LocalDateTime.of(2019, 1, 1, 12, 0)

    @ParameterizedTest
    @MethodSource(KELLON_TESTITAPAUKSET)
    fun `asiakirjan lisaysaika tulee kellosta ja sisalto seka opintooikeus sailyvat`(aika: KellonTestitapaus) {
        val lomake = asiakirjalomake()
        val asiakirja = Asiakirja(asiakirjaData = AsiakirjaData())
        whenever(opintooikeudet.findById(11L)).thenReturn(Optional.of(opintooikeus))
        whenever(asiakirjaMapper.toEntity(lomake)).thenReturn(asiakirja)
        whenever(asiakirjat.saveAll(any<List<Asiakirja>>())).thenAnswer { it.arguments[0] }
        whenever(asiakirjaMapper.toDto(asiakirja)).thenReturn(lomake)
        val palvelu = AsiakirjaServiceImpl(asiakirjat, asiakirjaMapper, mock(), opintooikeudet, aika.kello())

        assertThat(palvelu.create(listOf(lomake), 11L, null)).containsExactly(lomake)

        assertThat(asiakirja.lisattypvm).isEqualTo(aika.paikallinenAika())
        assertThat(asiakirja.opintooikeus).isSameAs(opintooikeus)
        assertThat(asiakirja.asiakirjaData!!.data).isEqualTo(sisalto)
    }

    @ParameterizedTest
    @MethodSource(KELLON_TESTITAPAUKSET)
    fun `valmistumisen pdf nimi ja lisaysaika tulevat kellosta`(aika: KellonTestitapaus) {
        val pyynnot = mock<ValmistumispyyntoRepository>()
        val pyynto = Valmistumispyynto(opintooikeus = opintooikeus)
        whenever(asiakirjat.save(any<Asiakirja>())).thenAnswer { it.arguments[0] }
        val palvelu = ValmistumispyynnonAsiakirjanTallennusService(asiakirjat, pyynnot, aika.kello())

        val asiakirja = palvelu.tallenna(pyynto, "valmistumisen_yhteenveto", sisalto) { tallennettava, tallennettu ->
            tallennettava.yhteenvetoAsiakirja = tallennettu
        }

        assertThat(asiakirja.nimi).isEqualTo("valmistumisen_yhteenveto_${aika.tiedostonPaiva}.pdf")
        assertThat(asiakirja.lisattypvm).isEqualTo(aika.paikallinenAika())
        assertThat(asiakirja.opintooikeus).isSameAs(opintooikeus)
        assertThat(asiakirja.asiakirjaData!!.data).isEqualTo(sisalto)
        assertThat(pyynto.yhteenvetoAsiakirja).isSameAs(asiakirja)
        verify(pyynnot).save(pyynto)
    }

    @ParameterizedTest
    @MethodSource("teoriakoulutustapaukset")
    fun `teoriakoulutuksen uusi todistus kayttaa kelloa ja vanhan todistuksen aika sailyy`(tapaus: Teoriakoulutustapaus) {
        val koulutukset = mock<TeoriakoulutusRepository>()
        val koulutusMapper = mock<TeoriakoulutusMapper>()
        val lomake = TeoriakoulutusDTO(id = if (tapaus.muokkaus) 1L else null)
        val koulutus = Teoriakoulutus(id = 1L)
        val vanhaTodistus = Asiakirja(id = 2L, lisattypvm = aiempiAika)
        val uusiTodistus = Asiakirja(id = 3L, asiakirjaData = AsiakirjaData())
        koulutus.todistukset.add(vanhaTodistus)
        val todistuslomake = asiakirjalomake()
        whenever(opintooikeudet.findById(11L)).thenReturn(Optional.of(opintooikeus))
        whenever(koulutukset.findOneByIdAndOpintooikeusId(1L, 11L)).thenReturn(koulutus)
        whenever(koulutusMapper.toEntity(lomake)).thenReturn(koulutus)
        whenever(koulutusMapper.toDto(koulutus)).thenReturn(lomake)
        whenever(koulutukset.save(koulutus)).thenReturn(koulutus)
        whenever(asiakirjaMapper.toEntity(todistuslomake)).thenReturn(uusiTodistus)
        val palvelu = TeoriakoulutusServiceImpl(koulutukset, koulutusMapper, opintooikeudet, asiakirjaMapper, mock(), mock(), tapaus.aika.kello())

        palvelu.save(lomake, setOf(todistuslomake), null, 11L)

        assertThat(uusiTodistus.lisattypvm).isEqualTo(tapaus.aika.paikallinenAika())
        assertThat(vanhaTodistus.lisattypvm).isEqualTo(aiempiAika)
        assertThat(uusiTodistus.teoriakoulutus).isSameAs(koulutus)
        assertThat(uusiTodistus.opintooikeus).isSameAs(opintooikeus)
        assertThat(uusiTodistus.asiakirjaData!!.data).isEqualTo(sisalto)
    }

    @ParameterizedTest
    @MethodSource("suunnitelmatapaukset")
    fun `vain vaihdettujen suunnitelmaliitteiden lisaysaika paivittyy kellosta`(tapaus: Suunnitelmatapaus) {
        val suunnitelmat = mock<KoulutussuunnitelmaRepository>()
        val suunnitelmaMapper = mock<KoulutussuunnitelmaMapper>()
        val vanhaSuunnitelma = Asiakirja(id = 2L, lisattypvm = aiempiAika)
        val vanhaMotivaatio = Asiakirja(id = 3L, lisattypvm = aiempiAika)
        val aiempi = Koulutussuunnitelma(id = 1L, opintooikeus = opintooikeus,
            koulutussuunnitelmaAsiakirja = vanhaSuunnitelma, motivaatiokirjeAsiakirja = vanhaMotivaatio)
        val uusiSuunnitelma = Asiakirja(id = 4L, asiakirjaData = AsiakirjaData())
        val uusiMotivaatio = Asiakirja(id = 5L, asiakirjaData = AsiakirjaData())
        val muutokset = Koulutussuunnitelma(koulutussuunnitelmaAsiakirja = uusiSuunnitelma, motivaatiokirjeAsiakirja = uusiMotivaatio)
        val lomake = KoulutussuunnitelmaDTO(koulutussuunnitelmaAsiakirjaUpdated = tapaus.suunnitelma,
            motivaatiokirjeAsiakirjaUpdated = tapaus.motivaatio, koulutussuunnitelmaAsiakirja = asiakirjalomake(), motivaatiokirjeAsiakirja = asiakirjalomake())
        whenever(suunnitelmat.findOneByOpintooikeusId(11L)).thenReturn(aiempi)
        whenever(suunnitelmaMapper.toEntity(lomake)).thenReturn(muutokset)
        whenever(suunnitelmat.save(muutokset)).thenReturn(muutokset)
        whenever(suunnitelmaMapper.toDto(muutokset)).thenReturn(lomake)
        val palvelu = KoulutussuunnitelmaServiceImpl(suunnitelmat, suunnitelmaMapper, opintooikeudet, mock(), tapaus.aika.kello())

        palvelu.save(lomake, 11L)

        val suunnitelma = muutokset.koulutussuunnitelmaAsiakirja!!
        val motivaatio = muutokset.motivaatiokirjeAsiakirja!!
        assertThat(suunnitelma.lisattypvm).isEqualTo(if (tapaus.suunnitelma) tapaus.aika.paikallinenAika() else aiempiAika)
        assertThat(motivaatio.lisattypvm).isEqualTo(if (tapaus.motivaatio) tapaus.aika.paikallinenAika() else aiempiAika)
        assertThat(suunnitelma).isSameAs(if (tapaus.suunnitelma) uusiSuunnitelma else vanhaSuunnitelma)
        assertThat(motivaatio).isSameAs(if (tapaus.motivaatio) uusiMotivaatio else vanhaMotivaatio)
        assertThat(vanhaSuunnitelma.lisattypvm).isEqualTo(aiempiAika)
        assertThat(vanhaMotivaatio.lisattypvm).isEqualTo(aiempiAika)
    }

    private fun asiakirjalomake() = AsiakirjaDTO(nimi = "todistus.pdf", asiakirjaData = AsiakirjaDataDTO(fileInputStream = sisalto.inputStream()))

    data class Teoriakoulutustapaus(val aika: KellonTestitapaus, val muokkaus: Boolean)
    data class Suunnitelmatapaus(val aika: KellonTestitapaus, val suunnitelma: Boolean, val motivaatio: Boolean)

    companion object {
        @JvmStatic
        fun teoriakoulutustapaukset(): List<Teoriakoulutustapaus> = KellonTestitapaus.tapaukset().flatMap { aika ->
            listOf(false, true).map { Teoriakoulutustapaus(aika, it) }
        }
        @JvmStatic
        fun suunnitelmatapaukset(): List<Suunnitelmatapaus> = KellonTestitapaus.tapaukset().flatMap { aika ->
            listOf(false, true).flatMap { suunnitelma -> listOf(false, true).map { Suunnitelmatapaus(aika, suunnitelma, it) } }
        }
    }
}
