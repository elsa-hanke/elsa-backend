package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.config.ANONYMOUS_USER
import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.repository.*
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.service.dto.KayttooikeusHakemusDTO
import fi.elsapalvelu.elsa.service.dto.OmatTiedotDTO
import fi.elsapalvelu.elsa.service.dto.UserDTO
import net.coobird.thumbnailator.Thumbnails
import net.coobird.thumbnailator.tasks.UnsupportedFormatException
import org.slf4j.LoggerFactory
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
import java.io.ByteArrayOutputStream
import java.security.Principal
import java.time.LocalDate
import java.time.ZoneId


@Service
@Transactional
class UserService(
    private val userRepository: UserRepository,
    private val kayttajaRepository: KayttajaRepository,
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository,
    private val opintooikeusRepository: OpintooikeusRepository,
    private val erikoisalaRepository: ErikoisalaRepository,
    private val yliopistoRepository: YliopistoRepository,
    private val opintoopasRepository: OpintoopasRepository,
    private val asetusRepository: AsetusRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(readOnly = true)
    fun getAllManagedUsers(pageable: Pageable): Page<UserDTO> =
        userRepository.findAllByLoginNot(pageable, ANONYMOUS_USER).map { UserDTO(it) }

    @Transactional(readOnly = true)
    fun getUserFromAuthentication(authToken: Saml2Authentication): UserDTO {
        val principal = authToken.principal as Saml2AuthenticatedPrincipal

        val user = User()
        user.id = principal.name
        user.firstName = principal.getFirstAttribute("urn:oid:2.5.4.42")
        user.lastName = principal.getFirstAttribute("urn:oid:2.5.4.4")
        user.authorities = authToken.authorities.map(GrantedAuthority::getAuthority)
            .map { Authority(name = it) }
            .toMutableSet()
        return UserDTO(user)
    }

    @Transactional(readOnly = true)
    fun getUser(userId: String): UserDTO {
        return UserDTO(userRepository.findById(userId).get())
    }

    @Transactional(readOnly = true)
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
            )
        )

        var erikoistuvaLaakari = ErikoistuvaLaakari(kayttaja = kayttaja)
        erikoistuvaLaakari = erikoistuvaLaakariRepository.save(erikoistuvaLaakari)

        // TODO: opinto-oikeus opintotietojärjestelmstä
        val validOpintooikeusExists = opintooikeusRepository.existsByErikoistuvaLaakariKayttajaUserId(
            user.id!!,
            LocalDate.now()
        )
        val yliopisto = yliopistoRepository.findByIdOrNull(kayttooikeusHakemusDTO.yliopisto)
        val erikoisala = erikoisalaRepository.findByIdOrNull(15)
        val opintoopas = opintoopasRepository.findByIdOrNull(15)
        val asetus = asetusRepository.findByIdOrNull(5)
        var opintooikeus = Opintooikeus(
            opintooikeudenMyontamispaiva = LocalDate.now(ZoneId.systemDefault()),
            opintooikeudenPaattymispaiva = LocalDate.now(ZoneId.systemDefault()),
            opiskelijatunnus = "123456",
            asetus = asetus,
            osaamisenArvioinninOppaanPvm = LocalDate.now(ZoneId.systemDefault()),
            erikoistuvaLaakari = erikoistuvaLaakari,
            yliopisto = yliopisto,
            erikoisala = erikoisala,
            opintoopas = opintoopas,
            kaytossa = !validOpintooikeusExists
        )
        opintooikeus = opintooikeusRepository.save(opintooikeus)

        erikoistuvaLaakari.opintooikeudet.add(opintooikeus)
        erikoistuvaLaakariRepository.save(erikoistuvaLaakari)


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

    fun updateUserDetails(
        omatTiedotDTO: OmatTiedotDTO,
        userId: String
    ): UserDTO {
        var user = userRepository.findById(userId).get()
        user.email = omatTiedotDTO.email
        user.phoneNumber = omatTiedotDTO.phoneNumber

        try {
            if (omatTiedotDTO.avatarUpdated) {
                omatTiedotDTO.avatar?.inputStream?.let {
                    val outputStream = ByteArrayOutputStream()
                    Thumbnails.of(it)
                        .size(256, 256)
                        .outputQuality(0.8)
                        .outputFormat("jpg")
                        .toOutputStream(outputStream)
                    user.avatar = outputStream.toByteArray()
                    it.close()
                } ?: run {
                    user.avatar = null
                }
            }
        } catch (ex: UnsupportedFormatException) {
            log.debug("Päivitettävä profiilikuva ei ole tuettu")
        }

        user = userRepository.save(user)

        return UserDTO(user)
    }

    @Transactional(readOnly = true)
    fun existsByEmail(email: String): Boolean {
        return userRepository.findOneByEmail(email).isPresent
    }
}
