package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.config.ANONYMOUS_USER
import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.repository.ErikoisalaRepository
import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.KayttajaRepository
import fi.elsapalvelu.elsa.repository.UserRepository
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.service.dto.KayttooikeusHakemusDTO
import fi.elsapalvelu.elsa.service.dto.OmatTiedotDTO
import fi.elsapalvelu.elsa.service.dto.UserDTO
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.core.AuthenticatedPrincipal
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.Principal


@Service
@Transactional
class UserService(
    private val userRepository: UserRepository,
    private val kayttajaRepository: KayttajaRepository,
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository,
    private val erikoisalaRepository: ErikoisalaRepository,
) {

    @Transactional(readOnly = true)
    fun getAllManagedUsers(pageable: Pageable): Page<UserDTO> =
        userRepository.findAllByLoginNot(pageable, ANONYMOUS_USER).map { UserDTO(it) }

    @Transactional
    fun getUserFromAuthentication(authToken: Saml2Authentication): UserDTO {
        val principal = authToken.principal as Saml2AuthenticatedPrincipal

        val user = userRepository.findById(principal.name).get()
        user.id = principal.name
        user.firstName = principal.getFirstAttribute("urn:oid:2.5.4.42")
        user.lastName = principal.getFirstAttribute("urn:oid:2.5.4.4")
        user.authorities = authToken.authorities.map(GrantedAuthority::getAuthority)
            .map { Authority(name = it) }
            .toMutableSet()
        return UserDTO(user)
    }

    fun getAuthenticatedUser(principal: Principal?): UserDTO {
        if (principal is Saml2Authentication) {
            return getUserFromAuthentication(principal)
        } else {
            throw RuntimeException("Käyttäjä ei ole kirjautunut")
        }
    }

    fun updateUserAuthorities(
        principal: Principal?,
        kayttooikeusHakemusDTO: KayttooikeusHakemusDTO
    ) {
        // TODO: tarkista käyttöoikeus opintotietojärjestelmästä

        // Lisätään käyttäjälle erikoistuva lääkäri rooli
        val userDTO = getAuthenticatedUser(principal)
        var user = userRepository.findById(userDTO.id!!).get()
        user.authorities.add(Authority(name = ERIKOISTUVA_LAAKARI))
        user = userRepository.save(user)

        val kayttaja = kayttajaRepository.save(
            Kayttaja(
                user = user,
                yliopisto = Yliopisto(id = kayttooikeusHakemusDTO.yliopisto)
            )
        )
        // TODO: erikoisalan valinta opinto-oikeuden mukaan
        val erikoisala = erikoisalaRepository.findByIdOrNull(46)
        erikoisala?.let {
            erikoistuvaLaakariRepository.save(
                ErikoistuvaLaakari(
                    kayttaja = kayttaja,
                    erikoisala = erikoisala,
                )
            )
        } ?: erikoistuvaLaakariRepository.save(
            ErikoistuvaLaakari(
                kayttaja = kayttaja,
            )
        )

        val existingAuthentication =
            SecurityContextHolder.getContext().authentication as Saml2Authentication
        SecurityContextHolder.getContext().authentication = Saml2Authentication(
            existingAuthentication.principal as AuthenticatedPrincipal?,
            existingAuthentication.saml2Response,
            listOf(
                SimpleGrantedAuthority(
                    ERIKOISTUVA_LAAKARI
                )
            )
        )
    }

    fun updateUserDetails(omatTiedotDTO: OmatTiedotDTO, userId: String): UserDTO {
        var user = userRepository.findById(userId).get()
        user.email = omatTiedotDTO.email
        user.phoneNumber = omatTiedotDTO.phoneNumber
        user.avatar = omatTiedotDTO.avatar
        user.avatarContentType = omatTiedotDTO.avatarContentType

        user = userRepository.save(user)

        return UserDTO(user)
    }

    @Transactional
    fun existsByEmail(email: String): Boolean {
        return userRepository.findOneByEmail(email).isPresent
    }
}
