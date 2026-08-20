package fi.elsapalvelu.elsa.service.mapper.arviointi

import fi.elsapalvelu.elsa.domain.arviointi.Arviointityokalu
import fi.elsapalvelu.elsa.domain.arviointi.ArviointityokaluKysymys
import fi.elsapalvelu.elsa.domain.arviointi.ArviointityokaluKysymysVaihtoehto
import fi.elsapalvelu.elsa.domain.arviointi.SuoritusarvioinninArviointityokalunVastaus
import fi.elsapalvelu.elsa.domain.arviointi.Suoritusarviointi
import fi.elsapalvelu.elsa.service.dto.arviointi.SuoritusarvioinninArviointityokalunVastausDTO
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SuoritusarvioinninArviointityokalunVastausMapperTest {

    private val mapper: SuoritusarvioinninArviointityokalunVastausMapper =
        SuoritusarvioinninArviointityokalunVastausMapperImpl()

    @Test
    fun toDtoShouldFlattenNestedEntityIds() {
        val entity = SuoritusarvioinninArviointityokalunVastaus(
            id = 1L,
            suoritusarviointi = Suoritusarviointi(id = 2L),
            arviointityokalu = Arviointityokalu(id = 3L),
            arviointityokaluKysymys = ArviointityokaluKysymys(id = 4L),
            tekstiVastaus = "Free-form answer",
            valittuVaihtoehto = ArviointityokaluKysymysVaihtoehto(id = 5L)
        )

        val dto = mapper.toDto(entity)

        assertThat(dto).usingRecursiveComparison().isEqualTo(
            SuoritusarvioinninArviointityokalunVastausDTO(
                id = 1L,
                suoritusarviointiId = 2L,
                arviointityokaluId = 3L,
                arviointityokaluKysymysId = 4L,
                tekstiVastaus = "Free-form answer",
                valittuVaihtoehtoId = 5L
            )
        )
    }

    @Test
    fun toEntityShouldCreateNestedReferencesFromIds() {
        val dto = SuoritusarvioinninArviointityokalunVastausDTO(
            id = 1L,
            suoritusarviointiId = 2L,
            arviointityokaluId = 3L,
            arviointityokaluKysymysId = 4L,
            tekstiVastaus = "Free-form answer",
            valittuVaihtoehtoId = 5L
        )

        val entity = mapper.toEntity(dto)

        assertThat(entity.id).isEqualTo(1L)
        assertThat(entity.suoritusarviointi?.id).isEqualTo(2L)
        assertThat(entity.arviointityokalu?.id).isEqualTo(3L)
        assertThat(entity.arviointityokaluKysymys?.id).isEqualTo(4L)
        assertThat(entity.tekstiVastaus).isEqualTo("Free-form answer")
        assertThat(entity.valittuVaihtoehto?.id).isEqualTo(5L)
    }

    @Test
    fun partialUpdateShouldUpdateProvidedAnswerAndPreserveOmittedValues() {
        val suoritusarviointi = Suoritusarviointi(id = 2L)
        val arviointityokalu = Arviointityokalu(id = 3L)
        val kysymys = ArviointityokaluKysymys(id = 4L)
        val vaihtoehto = ArviointityokaluKysymysVaihtoehto(id = 5L)
        val entity = SuoritusarvioinninArviointityokalunVastaus(
            id = 1L,
            suoritusarviointi = suoritusarviointi,
            arviointityokalu = arviointityokalu,
            arviointityokaluKysymys = kysymys,
            tekstiVastaus = "Original answer",
            valittuVaihtoehto = vaihtoehto
        )

        mapper.partialUpdate(
            entity,
            SuoritusarvioinninArviointityokalunVastausDTO(tekstiVastaus = "Updated answer")
        )

        assertThat(entity.id).isEqualTo(1L)
        assertThat(entity.suoritusarviointi).isSameAs(suoritusarviointi)
        assertThat(entity.arviointityokalu).isSameAs(arviointityokalu)
        assertThat(entity.arviointityokaluKysymys).isSameAs(kysymys)
        assertThat(entity.tekstiVastaus).isEqualTo("Updated answer")
        assertThat(entity.valittuVaihtoehto).isSameAs(vaihtoehto)
    }
}
