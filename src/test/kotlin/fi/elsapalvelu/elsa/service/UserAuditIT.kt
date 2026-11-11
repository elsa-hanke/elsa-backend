package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.AuditRevisionEntity
import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.security.testSamlPrincipal
import fi.elsapalvelu.elsa.service.kayttaja.UserService
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.hibernate.envers.AuditReaderFactory
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID

@SpringBootTest(classes = [ElsaBackendApp::class])
class UserAuditIT {
    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var transactionTemplate: TransactionTemplate

    @Autowired
    private lateinit var userService: UserService

    @Test
    fun committedChangeHasRevisionAndActorButRolledBackChangeDoesNot() {
        val userId = transactionTemplate.execute {
            val user = User(login = "audit${UUID.randomUUID().toString().take(12)}", activated = true)
            em.persist(user)
            em.flush()
            requireNotNull(user.id)
        }

        try {
            TestSecurityContextHolder.getContext().authentication = Saml2Authentication(
                testSamlPrincipal(userId, emptyMap()),
                "test",
                emptyList()
            )

            val committedEmail = "committed-$userId@example.com"
            transactionTemplate.executeWithoutResult {
                userService.updateEmail(committedEmail, userId)
            }

            val committedRevisions = transactionTemplate.execute {
                val reader = AuditReaderFactory.get(em)
                val revisions = reader.getRevisions(User::class.java, userId)
                assertThat(revisions).hasSize(2)
                val revision = revisions.last()
                assertThat(reader.find(User::class.java, userId, revision).email)
                    .isEqualTo(committedEmail)
                assertThat(reader.findRevision(AuditRevisionEntity::class.java, revision).userId)
                    .isEqualTo(userId)
                assertThat(em.find(User::class.java, userId).email).isEqualTo(committedEmail)
                revisions
            }!!

            transactionTemplate.executeWithoutResult { status ->
                userService.updateEmail("rolled-back-$userId@example.com", userId)
                em.flush()
                status.setRollbackOnly()
            }

            transactionTemplate.executeWithoutResult {
                assertThat(AuditReaderFactory.get(em).getRevisions(User::class.java, userId))
                    .containsExactlyElementsOf(committedRevisions)
                assertThat(em.find(User::class.java, userId).email).isEqualTo(committedEmail)
            }
        } finally {
            TestSecurityContextHolder.clearContext()
            transactionTemplate.executeWithoutResult {
                em.find(User::class.java, userId)?.let(em::remove)
            }
        }
    }
}
