package fi.elsapalvelu.elsa.cleanup

import fi.elsapalvelu.elsa.domain.kayttaja.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.repository.kayttaja.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

interface  ErikoistuvaLaakariCleanupService {
    fun clearErikoistuvaLaakari(erikoistuvaLaakari: fi.elsapalvelu.elsa.domain.kayttaja.ErikoistuvaLaakari)
}

@Component
class ErikoistuvaLaakariCleanupServiceImpl (
    private val userRepository: UserRepository
) : ErikoistuvaLaakariCleanupService {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun clearErikoistuvaLaakari(erikoistuvaLaakari: ErikoistuvaLaakari) {

        val kayttaja = erikoistuvaLaakari.kayttaja

        if( kayttaja != null ) {
            val user = kayttaja.user

            if(user?.id != null) {
                val fullUser = userRepository.findByIdWithAuthorities(user.id!!)
                if( fullUser.isPresent ) {
                    val authorities = fullUser.get().authorities
                    if( authorities.size == 1 ) {
                        // TODO do the anonymization of fullUser
                        log.info("User with id: ${fullUser.get().id} has only ROLE_ERIKOISTUVA_LAAKARI, anonymizing user.")
                    }
                }
            } else {
                log.warn("ErikoistuvaLaakari with id: ${erikoistuvaLaakari.id} has no associated user, skipping anonymization.")
            }
        } else {
            log.warn("ErikoistuvaLaakari with id: ${erikoistuvaLaakari.id} has no associated kayttaja, skipping anonymization.")
        }
    }
}
