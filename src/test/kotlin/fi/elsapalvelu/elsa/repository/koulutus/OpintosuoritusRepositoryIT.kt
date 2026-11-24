package fi.elsapalvelu.elsa.repository.koulutus

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.annotation.Transactional
import java.sql.Date
import java.time.LocalDate

@SpringBootTest(classes = [ElsaBackendApp::class])
@Transactional
class OpintosuoritusRepositoryIT {

    @Autowired private lateinit var repository: OpintosuoritusRepository
    @Autowired private lateinit var entityManager: EntityManager
    @Autowired private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `finds opintooikeus ids strictly more than one month past viewing period`() {
        val cutoff = requireNotNull(
            jdbcTemplate.queryForObject("select (current_date - interval '1 month')::date", Date::class.java)
        ).toLocalDate()
        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(entityManager)
        val expired = erikoistuvaLaakari.opintooikeudet.single()
        expired.viimeinenKatselupaiva = cutoff.minusDays(1)
        fun newOpintooikeus(id: String, date: LocalDate) = Opintooikeus(
            yliopistoOpintooikeusId = id,
            opintooikeudenMyontamispaiva = expired.opintooikeudenMyontamispaiva,
            opintooikeudenPaattymispaiva = expired.opintooikeudenPaattymispaiva,
            viimeinenKatselupaiva = date,
            osaamisenArvioinninOppaanPvm = expired.osaamisenArvioinninOppaanPvm,
            tila = expired.tila,
            erikoistuvaLaakari = expired.erikoistuvaLaakari,
            yliopisto = expired.yliopisto,
            erikoisala = expired.erikoisala,
            opintoopas = expired.opintoopas,
            asetus = expired.asetus
        )
        val anotherExpired = newOpintooikeus("another-expired", cutoff.minusDays(2))
        val atCutoff = newOpintooikeus("at-cutoff", cutoff)
        val recent = newOpintooikeus("after-cutoff", cutoff.plusDays(1))
        entityManager.persist(anotherExpired)
        entityManager.persist(atCutoff)
        entityManager.persist(recent)
        entityManager.flush()
    }
}
