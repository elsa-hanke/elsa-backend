package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.domain.tyoskentely.Tyoskentelyjakso
import fi.elsapalvelu.elsa.repository.arviointi.SuoritusarviointiRepository
import fi.elsapalvelu.elsa.repository.kayttaja.AsiakirjaRepository
import fi.elsapalvelu.elsa.repository.koulutus.KoulutusjaksoRepository
import fi.elsapalvelu.elsa.repository.tyoskentely.TyoskentelyjaksoRepository
import fi.elsapalvelu.elsa.service.tyoskentely.TyoskentelyjaksoService
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.helpers.AsiakirjaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariTyoskentelyjaksoHelper
import fi.elsapalvelu.elsa.web.rest.helpers.KoulutusjaksoHelper
import fi.elsapalvelu.elsa.web.rest.helpers.SuoritusarviointiHelper
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.access.AccessDeniedException
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertNotNull

/**
 * Varmistaa palvelutasolla, että TyoskentelyjaksoService.delete ei muuta mitään tietoa ennen kuin
 * omistajuus on varmistettu. Suojaa sekä Erikoistuva- että YEK-rajapinnan, koska molemmat kutsuvat
 * samaa palvelumetodia.
 */
@SpringBootTest(classes = [ElsaBackendApp::class])
@Transactional
class TyoskentelyjaksoServiceDeleteIT {

    @Autowired
    private lateinit var tyoskentelyjaksoService: TyoskentelyjaksoService

    @Autowired
    private lateinit var tyoskentelyjaksoRepository: TyoskentelyjaksoRepository

    @Autowired
    private lateinit var asiakirjaRepository: AsiakirjaRepository

    @Autowired
    private lateinit var koulutusjaksoRepository: KoulutusjaksoRepository

    @Autowired
    private lateinit var suoritusarviointiRepository: SuoritusarviointiRepository

    @Autowired
    private lateinit var em: EntityManager

    private lateinit var owner: User
    private lateinit var tyoskentelyjakso: Tyoskentelyjakso
    private var tyoskentelyjaksoId: Long = 0
    private var ownerOpintooikeusId: Long = 0
    private var asiakirjaId: Long = 0
    private var koulutusjaksoId: Long = 0

    @BeforeEach
    fun setup() {
        owner = KayttajaResourceWithMockUserIT.createEntity()
        em.persist(owner)
        em.flush()

        tyoskentelyjakso = ErikoistuvaLaakariTyoskentelyjaksoHelper.createEntity(em, owner)
        tyoskentelyjakso.asiakirjat.add(AsiakirjaHelper.createEntity(em, owner, tyoskentelyjakso))
        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        tyoskentelyjaksoId = assertNotNull(tyoskentelyjakso.id)
        asiakirjaId = assertNotNull(tyoskentelyjakso.asiakirjat.first().id)
        ownerOpintooikeusId = assertNotNull(tyoskentelyjakso.opintooikeus?.id)

        val koulutusjakso = KoulutusjaksoHelper.createEntity(em, owner)
        koulutusjakso.tyoskentelyjaksot = mutableSetOf(tyoskentelyjakso)
        koulutusjaksoRepository.saveAndFlush(koulutusjakso)
        koulutusjaksoId = assertNotNull(koulutusjakso.id)
    }

    @Test
    fun deleteWithForeignOpintooikeusThrowsAndLeavesReferencesIntact() {
        val toinenUser = KayttajaResourceWithMockUserIT.createEntity()
        em.persist(toinenUser)
        em.flush()
        val toinenErikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, toinenUser)
        em.persist(toinenErikoistuvaLaakari)
        em.flush()
        val foreignOpintooikeusId =
            assertNotNull(toinenErikoistuvaLaakari.getOpintooikeusKaytossa()?.id)

        assertThrows<AccessDeniedException> {
            tyoskentelyjaksoService.delete(tyoskentelyjaksoId, foreignOpintooikeusId)
        }

        assertReferencesIntact()
    }

    @Test
    fun deleteNonExistentIdThrowsAccessDenied() {
        val tableSizeBefore = tyoskentelyjaksoRepository.findAll().size

        assertThrows<AccessDeniedException> {
            tyoskentelyjaksoService.delete(Long.MAX_VALUE, ownerOpintooikeusId)
        }

        assertThat(tyoskentelyjaksoRepository.findAll()).hasSize(tableSizeBefore)
        assertReferencesIntact()
    }

    @Test
    fun deleteOwnTyoskentelyjaksoRemovesReferencesAndKeepsEntities() {
        val result = tyoskentelyjaksoService.delete(tyoskentelyjaksoId, ownerOpintooikeusId)

        assertThat(result).isTrue()

        em.flush()
        em.clear()

        assertThat(tyoskentelyjaksoRepository.findById(tyoskentelyjaksoId)).isEmpty
        assertThat(asiakirjaRepository.findById(asiakirjaId).orElseThrow().tyoskentelyjakso).isNull()
        assertThat(
            koulutusjaksoRepository.findById(koulutusjaksoId).orElseThrow().tyoskentelyjaksot
                ?: mutableSetOf()
        ).isEmpty()
    }

    @Test
    fun deleteOwnTyoskentelyjaksoWithSuoritusarviointiReturnsFalseAndKeepsReferences() {
        em.detach(tyoskentelyjakso)
        suoritusarviointiRepository.saveAndFlush(
            SuoritusarviointiHelper.createEntity(em, owner)
        )

        val result = tyoskentelyjaksoService.delete(tyoskentelyjaksoId, ownerOpintooikeusId)

        assertThat(result).isFalse()

        em.flush()
        em.clear()

        assertReferencesIntact()
    }

    private fun assertReferencesIntact() {
        em.flush()
        em.clear()

        assertThat(tyoskentelyjaksoRepository.findById(tyoskentelyjaksoId)).isPresent
        assertThat(asiakirjaRepository.findById(asiakirjaId).orElseThrow().tyoskentelyjakso?.id)
            .isEqualTo(tyoskentelyjaksoId)
        assertThat(
            koulutusjaksoRepository.findById(koulutusjaksoId).orElseThrow().tyoskentelyjaksot?.map { it.id }
        ).containsExactly(tyoskentelyjaksoId)
    }
}
