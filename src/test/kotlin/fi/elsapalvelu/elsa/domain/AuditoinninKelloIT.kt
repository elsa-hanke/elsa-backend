package fi.elsapalvelu.elsa.domain

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.domain.koulutus.Koulutussuunnitelma
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper
import org.springframework.transaction.annotation.Transactional
import fi.elsapalvelu.elsa.service.helpers.KELLON_TESTITAPAUKSET
import fi.elsapalvelu.elsa.service.helpers.KellonTestitapaus
import java.util.UUID
import jakarta.persistence.EntityManager
import java.time.Clock
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.support.TransactionTemplate

@SpringBootTest(classes = [ElsaBackendApp::class], properties = [
    "spring.datasource.url=jdbc:tc:postgresql:16.9:///auditoinninKello?TC_TMPFS=/testtmpfs:rw&TC_DAEMON=true"
])
class AuditoinninKelloIT {
    @Autowired private lateinit var em: EntityManager
    @Autowired private lateinit var tapahtumat: TransactionTemplate
    @Autowired private lateinit var jdbc: JdbcTemplate
    @MockitoBean private lateinit var clock: Clock

    @ParameterizedTest
    @MethodSource(KELLON_TESTITAPAUKSET)
    @Transactional
    fun `entiteetin elinkaari paivittaa muokkauspaivan kellosta`(aika: KellonTestitapaus) {
        whenever(clock.instant()).thenReturn(aika.aikaleima())
        whenever(clock.zone).thenReturn(aika.kello().zone)
        val erikoistuja = ErikoistuvaLaakariHelper.createEntity(em)
        val suunnitelma = Koulutussuunnitelma(
            opintooikeus = erikoistuja.getOpintooikeusKaytossa()
        )
        em.persist(suunnitelma)
        em.flush()
        assertThat(suunnitelma.muokkauspaiva).isEqualTo(aika.paiva)

        whenever(clock.instant()).thenReturn(aika.aikaleima().plusSeconds(86400))
        suunnitelma.motivaatiokirje = "Päivitetty"
        em.flush()
        em.clear()

        val tallennettu = em.find(Koulutussuunnitelma::class.java, suunnitelma.id)
        assertThat(tallennettu.muokkauspaiva).isEqualTo(aika.paiva.plusDays(1))
    }

    @ParameterizedTest
    @MethodSource(KELLON_TESTITAPAUKSET)
    fun `JPA ja Envers kayttavat samaa kelloa luotaessa ja paivitettassa`(aika: KellonTestitapaus) {
        whenever(clock.instant()).thenReturn(aika.aikaleima())
        whenever(clock.zone).thenReturn(aika.kello().zone)
        val tunniste = tapahtumat.execute {
            val kayttaja = User(login = UUID.randomUUID().toString())
            em.persist(kayttaja)
            em.flush()
            assertThat(kayttaja.createdDate).isEqualTo(aika.aikaleima())
            kayttaja.id!!
        }
        val seuraavaHetki = aika.aikaleima().plusSeconds(86400)
        whenever(clock.instant()).thenReturn(seuraavaHetki)
        tapahtumat.executeWithoutResult {
            val kayttaja = em.find(User::class.java, tunniste)
            kayttaja.email = "$tunniste@example.test"
            em.flush()
            assertThat(kayttaja.createdDate).isEqualTo(aika.aikaleima())
            assertThat(kayttaja.lastModifiedDate).isEqualTo(seuraavaHetki)
        }
        val revisionTunniste = jdbc.queryForObject(
            "select max(revision_id) from jhi_user_audit where id = ?", Int::class.java, tunniste
        )!!
        tapahtumat.executeWithoutResult {
            val revisio = em.find(AuditRevisionEntity::class.java, revisionTunniste)
            assertThat(revisio.modifiedDate).isEqualTo(aika.paikallinenAika().plusDays(1))
            assertThat(revisio.timestamp).isEqualTo(seuraavaHetki.toEpochMilli())
        }
    }
}
