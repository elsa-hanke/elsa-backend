package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.tyoskentely.Keskeytysaika
import fi.elsapalvelu.elsa.domain.tyoskentely.Tyoskentelyjakso
import fi.elsapalvelu.elsa.service.dto.tyoskentely.KeskeytysaikaDTO
import fi.elsapalvelu.elsa.service.dto.tyoskentely.TyoskentelyjaksoDTO
import fi.elsapalvelu.elsa.service.tyoskentely.OverlappingTyoskentelyjaksoValidationService
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper
import fi.elsapalvelu.elsa.web.rest.helpers.PoissaolonSyyHelper
import fi.elsapalvelu.elsa.web.rest.helpers.TyoskentelyjaksoHelper
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@SpringBootTest(classes = [ElsaBackendApp::class])
@Transactional
class OverlappingTyoskentelyjaksoValidationIT {
    @Autowired private lateinit var em: EntityManager
    @Autowired private lateinit var validator: OverlappingTyoskentelyjaksoValidationService

    @ParameterizedTest
    @ValueSource(ints = [50, 100])
    fun `period validation does not persist proposed dates or percentage`(percentage: Int) {
        val fixture = fixture()
        val proposed = TyoskentelyjaksoDTO(
            id = fixture.second.id,
            alkamispaiva = start.plusDays(1),
            paattymispaiva = end.minusDays(1),
            osaaikaprosentti = percentage
        )

        assertThat(validator.validateTyoskentelyjakso(fixture.opintooikeusId, proposed))
            .isEqualTo(percentage == 50)

        flushAndReload(fixture)
        val stored = em.find(Tyoskentelyjakso::class.java, fixture.second.id)
        assertThat(stored.alkamispaiva).isEqualTo(start)
        assertThat(stored.paattymispaiva).isEqualTo(end)
        assertThat(stored.osaaikaprosentti).isEqualTo(50)
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 100])
    fun `absence validation does not persist proposed dates or percentage`(percentage: Int) {
        val fixture = fixture()
        val proposed = KeskeytysaikaDTO(
            id = fixture.absence.id,
            alkamispaiva = start,
            paattymispaiva = end.plusDays(1),
            poissaoloprosentti = percentage,
            tyoskentelyjaksoId = fixture.first.id,
            tyoskentelyjakso = TyoskentelyjaksoDTO(alkamispaiva = start, paattymispaiva = end)
        )

        assertThat(validator.validateKeskeytysaika(fixture.opintooikeusId, proposed))
            .isEqualTo(percentage == 100)

        flushAndReload(fixture)
    }

    @Test
    fun `rejected absence deletion does not remove the orphan at flush`() {
        val fixture = fixture()

        assertThat(validator.validateKeskeytysaikaDelete(fixture.opintooikeusId, fixture.absence.id!!)).isFalse()

        flushAndReload(fixture)
        assertThat(em.find(Tyoskentelyjakso::class.java, fixture.first.id).keskeytykset)
            .extracting<Long> { it.id }.containsExactly(fixture.absence.id)
    }

    private fun flushAndReload(fixture: Fixture) {
        // The validator joins this write transaction. A readOnly hint alone cannot protect its entities.
        em.flush()
        em.clear()
        val stored = em.find(Keskeytysaika::class.java, fixture.absence.id)
        assertThat(stored).isNotNull()
        assertThat(stored.alkamispaiva).isEqualTo(start)
        assertThat(stored.paattymispaiva).isEqualTo(end)
        assertThat(stored.poissaoloprosentti).isEqualTo(50)
    }

    private fun fixture(): Fixture {
        val trainee = ErikoistuvaLaakariHelper.createEntity(em)
        val first = TyoskentelyjaksoHelper.createEntity(em, alkamispaiva = start, paattymispaiva = end)
        first.opintooikeus = trainee.getOpintooikeusKaytossa()
        first.osaaikaprosentti = 100
        em.persist(first)
        val second = TyoskentelyjaksoHelper.createEntity(em, alkamispaiva = start, paattymispaiva = end)
        second.opintooikeus = first.opintooikeus
        em.persist(second)
        val reason = PoissaolonSyyHelper.createEntity()
        em.persist(reason)
        val absence = Keskeytysaika(
            alkamispaiva = start, paattymispaiva = end, poissaoloprosentti = 50,
            poissaolonSyy = reason, tyoskentelyjakso = first
        )
        first.keskeytykset.add(absence)
        em.persist(absence)
        em.flush()
        val fixture = Fixture(first.opintooikeus!!.id!!, first, second, absence)
        em.clear()
        return fixture
    }

    private data class Fixture(
        val opintooikeusId: Long,
        val first: Tyoskentelyjakso,
        val second: Tyoskentelyjakso,
        val absence: Keskeytysaika
    )

    companion object {
        private val start = LocalDate.of(2020, 1, 1)
        private val end = LocalDate.of(2020, 1, 10)
    }
}
