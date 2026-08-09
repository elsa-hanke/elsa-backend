package fi.elsapalvelu.elsa.service.arkistointi.sahke

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.kayttaja.AsiakirjaData
import fi.elsapalvelu.elsa.domain.kayttaja.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.kayttaja.Kayttaja
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.domain.perustiedot.Erikoisala
import fi.elsapalvelu.elsa.domain.perustiedot.Yliopisto
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.arkistointi.ArkistointiConfiguration
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordProperties
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import org.apache.commons.codec.digest.DigestUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SahkeMetadataBuilderTest {
    private val builder = SahkeMetadataBuilder()

    @Test
    fun `build includes only configured documents in metadata and package input`() {
        val opintooikeus = createOpintooikeus()
        val included = createRecordProperties(opintooikeus, "yhteenveto.pdf", RecordType.YHTEENVETO)
        val excluded = createRecordProperties(opintooikeus, "liite.pdf", RecordType.LIITE)
        val case = ApplicationProperties.Arkistointi.Case().apply {
            title = "Valmistumisen asiakirjat"
            type = "VALMISTUMINEN"
            function = "04.01.04"
            documents = mapOf(
                RecordType.YHTEENVETO.name.lowercase() to
                    ApplicationProperties.Arkistointi.DocumentMetadata().apply {
                        retentionPeriod = "10"
                    }
            )
        }
        val metadata = ApplicationProperties.Arkistointi.Metadata().apply {
            organisation = "Testiorganisaatio"
            cases = emptyMap()
        }

        val result = builder.build(
            request = SahkeMetadataRequest(
                opintooikeus = opintooikeus,
                asiakirjat = listOf(included, excluded),
                caseId = "CASE-123",
                tarkastaja = "Testi Tarkastaja",
                tarkastusPaiva = LocalDate.of(2024, 1, 2),
                hyvaksyja = "Testi Hyväksyjä",
                hyvaksymisPaiva = LocalDate.of(2024, 1, 3)
            ),
            configuration = ArkistointiConfiguration(metadata, case)
        )

        assertThat(result.asiakirjat).containsExactly(included)
        assertThat(result.metadata.caseFile.action.record).hasSize(1)
        val record = result.metadata.caseFile.action.record.single()
        assertThat(record.document.file.path).isEqualTo("pdf/yhteenveto.pdf")
        assertThat(record.document.hashValue).isEqualTo(
            DigestUtils.sha256Hex(included.asiakirja.asiakirjaData?.data)
        )
        assertThat(record.custom.asiakirjatyyppi).isEqualTo(case.title)
        assertThat(record.custom.tarkastaja).isEqualTo("Testi Tarkastaja")
        assertThat(record.custom.hyvaksyja).isEqualTo("Testi Hyväksyjä")
    }

    private fun createOpintooikeus(): Opintooikeus {
        val user = User(firstName = "Matti", lastName = "Meikäläinen")
        return Opintooikeus(
            id = 123L,
            erikoistuvaLaakari = ErikoistuvaLaakari(
                kayttaja = Kayttaja(user = user),
                syntymaaika = LocalDate.of(1990, 12, 31)
            ),
            yliopisto = Yliopisto(nimi = YliopistoEnum.TAMPEREEN_YLIOPISTO),
            erikoisala = Erikoisala(nimi = "Kirurgia"),
            opiskelijatunnus = "OP123"
        )
    }

    private fun createRecordProperties(
        opintooikeus: Opintooikeus,
        name: String,
        recordType: RecordType
    ): RecordProperties = RecordProperties(
        asiakirja = Asiakirja(
            id = 321L,
            opintooikeus = opintooikeus,
            nimi = name,
            tyyppi = "application/pdf",
            asiakirjaData = AsiakirjaData(data = name.toByteArray())
        ),
        type = recordType
    )
}
