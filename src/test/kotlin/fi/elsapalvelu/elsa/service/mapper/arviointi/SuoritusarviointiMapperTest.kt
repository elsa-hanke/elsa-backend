package fi.elsapalvelu.elsa.service.mapper.arviointi

import fi.elsapalvelu.elsa.domain.arviointi.Suoritusarviointi
import fi.elsapalvelu.elsa.service.dto.arviointi.SuoritusarviointiDTO
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SuoritusarviointiMapperTest {

    private val mapper: SuoritusarviointiMapper = SuoritusarviointiMapperImpl()

    @Test
    fun partialUpdateShouldUpdateProvidedValuesAndPreserveNullValues() {
        val entity = Suoritusarviointi(
            id = 1L,
            arvioitavaTapahtuma = "Original event",
            lisatiedot = "Original details",
            lukittu = true,
            keskenerainen = true
        )
        val dto = SuoritusarviointiDTO(
            lisatiedot = "Updated details",
            lukittu = true,
            keskenerainen = true
        )

        mapper.partialUpdate(entity, dto)

        assertThat(entity.id).isEqualTo(1L)
        assertThat(entity.arvioitavaTapahtuma).isEqualTo("Original event")
        assertThat(entity.lisatiedot).isEqualTo("Updated details")
        assertThat(entity.lukittu).isTrue
        assertThat(entity.keskenerainen).isTrue
    }

    @Test
    fun fromIdShouldCreateReferenceOnlyForNonNullId() {
        assertThat(mapper.fromId(1L)?.id).isEqualTo(1L)
        assertThat(mapper.fromId(null)).isNull()
    }
}
