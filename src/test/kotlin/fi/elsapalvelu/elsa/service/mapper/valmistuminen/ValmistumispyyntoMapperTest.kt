package fi.elsapalvelu.elsa.service.mapper.valmistuminen

import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.kayttaja.AsiakirjaData
import fi.elsapalvelu.elsa.domain.kayttaja.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.kayttaja.Kayttaja
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.domain.perustiedot.Asetus
import fi.elsapalvelu.elsa.domain.perustiedot.Erikoisala
import fi.elsapalvelu.elsa.domain.perustiedot.Yliopisto
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto
import fi.elsapalvelu.elsa.service.dto.valmistuminen.ValmistumispyyntoDTO
import fi.elsapalvelu.elsa.service.dto.valmistuminen.ValmistumispyyntoOsaamisenArviointiDTO
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ValmistumispyyntoMapperTest {

    private val mapper: ValmistumispyyntoMapper = ValmistumispyyntoMapperImpl()
    private val osaamisenArviointiMapper: ValmistumispyyntoOsaamisenArviointiMapper =
        ValmistumispyyntoOsaamisenArviointiMapperImpl()

    @Test
    fun toDtoShouldMapNestedTraineeReviewersAndDocumentIds() {
        val entity = createValmistumispyynto()

        val dto = mapper.toDto(entity)

        assertThat(dto.id).isEqualTo(1L)
        assertThat(dto.erikoistujanNimi).isEqualTo("Erika Erikoistuja")
        assertThat(dto.erikoistujanAvatar).containsExactly(1, 2, 3)
        assertThat(dto.erikoistujanOpiskelijatunnus).isEqualTo("student-123")
        assertThat(dto.erikoistujanSyntymaaika).isEqualTo(LocalDate.of(1990, 2, 3))
        assertThat(dto.erikoistujanYliopisto).isEqualTo(YliopistoEnum.HELSINGIN_YLIOPISTO)
        assertThat(dto.erikoistujanErikoisala).isEqualTo("Cardiology")
        assertThat(dto.erikoistujanLaillistamispaiva).isEqualTo(LocalDate.of(2020, 4, 5))
        assertThat(dto.erikoistujanLaillistamistodistus).containsExactly(4, 5, 6)
        assertThat(dto.erikoistujanLaillistamistodistusNimi).isEqualTo("licence.pdf")
        assertThat(dto.erikoistujanLaillistamistodistusTyyppi).isEqualTo("application/pdf")
        assertThat(dto.erikoistujanAsetus).isEqualTo("Regulation 2020")
        assertThat(dto.opintooikeusId).isEqualTo(2L)
        assertThat(dto.opintooikeudenMyontamispaiva).isEqualTo(LocalDate.of(2021, 8, 1))
        assertThat(dto.vastuuhenkiloOsaamisenArvioijaNimi).isEqualTo("Olivia Reviewer")
        assertThat(dto.vastuuhenkiloOsaamisenArvioijaNimike).isEqualTo("Professor")
        assertThat(dto.virkailijaNimi).isEqualTo("Victor Official")
        assertThat(dto.vastuuhenkiloHyvaksyjaNimi).isEqualTo("Hanna Approver")
        assertThat(dto.vastuuhenkiloHyvaksyjaNimike).isEqualTo("Chief physician")
        assertThat(dto.yhteenvetoAsiakirjaId).isEqualTo(10L)
        assertThat(dto.liitteetAsiakirjaId).isEqualTo(11L)
        assertThat(dto.erikoistujanTiedotAsiakirjaId).isEqualTo(12L)
    }

    @Test
    fun osaamisenArviointiToDtoShouldMapNestedFieldsAndFormatBirthDate() {
        val entity = createValmistumispyynto()

        val dto = osaamisenArviointiMapper.toDto(entity)

        assertThat(dto.erikoistujanNimi).isEqualTo("Erika Erikoistuja")
        assertThat(dto.erikoistujanAvatar).containsExactly(1, 2, 3)
        assertThat(dto.erikoistujanSyntymaaika).isEqualTo("1990-02-03")
        assertThat(dto.erikoistujanYliopisto).isEqualTo(YliopistoEnum.HELSINGIN_YLIOPISTO)
        assertThat(dto.erikoistujanErikoisala).isEqualTo("Cardiology")
        assertThat(dto.erikoistujanLaillistamistodistus).containsExactly(4, 5, 6)
        assertThat(dto.erikoistujanAsetus).isEqualTo("Regulation 2020")
        assertThat(dto.opintooikeusId).isEqualTo(2L)
        assertThat(dto.vastuuhenkiloOsaamisenArvioijaNimi).isEqualTo("Olivia Reviewer")
        assertThat(dto.vastuuhenkiloOsaamisenArvioijaNimike).isEqualTo("Professor")
    }

    @Test
    fun partialUpdateShouldUpdateProvidedValuesAndPreserveOmittedAndNestedValues() {
        val entity = createValmistumispyynto()
        val originalOpintooikeus = entity.opintooikeus
        val originalKuittausaika = entity.erikoistujanKuittausaika

        mapper.partialUpdate(
            entity,
            ValmistumispyyntoDTO(
                virkailijanSaate = "Updated cover note",
                vastuuhenkiloHyvaksyjaKorjausehdotus = "Updated correction"
            )
        )

        assertThat(entity.id).isEqualTo(1L)
        assertThat(entity.opintooikeus).isSameAs(originalOpintooikeus)
        assertThat(entity.selvitysVanhentuneistaSuorituksista).isEqualTo("Original explanation")
        assertThat(entity.erikoistujanKuittausaika).isEqualTo(originalKuittausaika)
        assertThat(entity.virkailijanSaate).isEqualTo("Updated cover note")
        assertThat(entity.vastuuhenkiloHyvaksyjaKorjausehdotus).isEqualTo("Updated correction")
    }

    @Test
    fun osaamisenArviointiPartialUpdateShouldPreserveNullValues() {
        val entity = createValmistumispyynto()
        val originalPalautusaika = entity.vastuuhenkiloOsaamisenArvioijaPalautusaika
        val originalVirkailijanKorjausehdotus = entity.virkailijanKorjausehdotus

        osaamisenArviointiMapper.partialUpdate(
            entity,
            ValmistumispyyntoOsaamisenArviointiDTO(
                vastuuhenkiloOsaamisenArvioijaKorjausehdotus = "Updated reviewer correction"
            )
        )

        assertThat(entity.id).isEqualTo(1L)
        assertThat(entity.vastuuhenkiloOsaamisenArvioijaPalautusaika).isEqualTo(originalPalautusaika)
        assertThat(entity.vastuuhenkiloOsaamisenArvioijaKorjausehdotus).isEqualTo("Updated reviewer correction")
        assertThat(entity.virkailijanKorjausehdotus).isEqualTo(originalVirkailijanKorjausehdotus)
    }

    private fun createValmistumispyynto(): Valmistumispyynto {
        val erikoistuvaLaakari = ErikoistuvaLaakari(
            syntymaaika = LocalDate.of(1990, 2, 3),
            kayttaja = createKayttaja("Erika", "Erikoistuja", avatar = byteArrayOf(1, 2, 3)),
            laillistamispaiva = LocalDate.of(2020, 4, 5),
            laillistamistodistus = AsiakirjaData(data = byteArrayOf(4, 5, 6)),
            laillistamispaivanLiitetiedostonNimi = "licence.pdf",
            laillistamispaivanLiitetiedostonTyyppi = "application/pdf"
        )
        val opintooikeus = Opintooikeus(
            id = 2L,
            opintooikeudenMyontamispaiva = LocalDate.of(2021, 8, 1),
            opiskelijatunnus = "student-123",
            erikoistuvaLaakari = erikoistuvaLaakari,
            yliopisto = Yliopisto(nimi = YliopistoEnum.HELSINGIN_YLIOPISTO),
            erikoisala = Erikoisala(nimi = "Cardiology"),
            asetus = Asetus(nimi = "Regulation 2020")
        )

        return Valmistumispyynto(
            id = 1L,
            opintooikeus = opintooikeus,
            selvitysVanhentuneistaSuorituksista = "Original explanation",
            vastuuhenkiloOsaamisenArvioijaKuittausaika = LocalDate.of(2024, 1, 10),
            vastuuhenkiloOsaamisenArvioijaPalautusaika = LocalDate.of(2024, 1, 11),
            vastuuhenkiloOsaamisenArvioijaKorjausehdotus = "Original reviewer correction",
            vastuuhenkiloOsaamisenArvioija = createKayttaja("Olivia", "Reviewer", "Professor"),
            virkailijanKorjausehdotus = "Original official correction",
            virkailija = createKayttaja("Victor", "Official"),
            virkailijanSaate = "Original cover note",
            vastuuhenkiloHyvaksyjaKorjausehdotus = "Original approver correction",
            vastuuhenkiloHyvaksyja = createKayttaja("Hanna", "Approver", "Chief physician"),
            erikoistujanKuittausaika = LocalDate.of(2024, 1, 9),
            muokkauspaiva = LocalDate.of(2024, 1, 12),
            yhteenvetoAsiakirja = Asiakirja(id = 10L),
            liitteetAsiakirja = Asiakirja(id = 11L),
            erikoistujanTiedotAsiakirja = Asiakirja(id = 12L)
        )
    }

    private fun createKayttaja(
        firstName: String,
        lastName: String,
        nimike: String? = null,
        avatar: ByteArray? = null
    ) = Kayttaja(
        nimike = nimike,
        user = User(firstName = firstName, lastName = lastName, avatar = avatar)
    )
}
