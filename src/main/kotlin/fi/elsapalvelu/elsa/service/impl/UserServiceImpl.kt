package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.config.ANONYMOUS_USER
import fi.elsapalvelu.elsa.config.LoginException
import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.domain.enumeration.KayttajatilinTila
import fi.elsapalvelu.elsa.repository.*
import fi.elsapalvelu.elsa.service.UserService
import fi.elsapalvelu.elsa.service.constants.KAYTTAJA_NOT_FOUND_ERROR
import fi.elsapalvelu.elsa.service.dto.OmatTiedotDTO
import fi.elsapalvelu.elsa.service.dto.UserDTO
import net.coobird.thumbnailator.Thumbnails
import net.coobird.thumbnailator.tasks.UnsupportedFormatException
import org.apache.commons.text.similarity.LevenshteinDistance
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.security.Principal
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityNotFoundException


@Service
@Transactional
class UserServiceImpl(
    private val userRepository: UserRepository,
    private val verificationTokenRepository: VerificationTokenRepository,
    private val kayttajaRepository: KayttajaRepository,
    private val kouluttajavaltuutusRepository: KouluttajavaltuutusRepository,
    private val kayttajaYliopistoErikoisalaRepository: KayttajaYliopistoErikoisalaRepository,
    private val suoritusarviointiRepository: SuoritusarviointiRepository,
    private val koejaksonKoulutussopimusRepository: KoejaksonKoulutussopimusRepository,
    private val koejaksonAloituskeskusteluRepository: KoejaksonAloituskeskusteluRepository,
    private val koejaksonValiarviointiRepository: KoejaksonValiarviointiRepository,
    private val koejaksonKehittamistoimenpiteetRepository: KoejaksonKehittamistoimenpiteetRepository,
    private val koejaksonLoppukeskusteluRepository: KoejaksonLoppukeskusteluRepository,
    private val entityManager: EntityManager
) : UserService {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(readOnly = true)
    override fun getAllManagedUsers(pageable: Pageable): Page<UserDTO> =
        userRepository.findAllByLoginNot(pageable, ANONYMOUS_USER).map { UserDTO(it) }

    @Transactional(readOnly = true)
    override fun getUserFromAuthentication(authToken: Saml2Authentication): UserDTO {
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
    override fun getUser(userId: String): UserDTO {
        return UserDTO(userRepository.findById(userId).get())
    }

    @Transactional(readOnly = true)
    override fun getAuthenticatedUser(principal: Principal?): UserDTO {
        if (principal is Saml2Authentication) {
            return getUserFromAuthentication(principal)
        } else {
            throw RuntimeException("Käyttäjä ei ole kirjautunut")
        }
    }

    override fun createUser(
        cipher: Cipher,
        originalKey: SecretKey,
        hetu: String?,
        firstName: String,
        lastName: String
    ): UserDTO {
        cipher.init(Cipher.ENCRYPT_MODE, originalKey)
        val params = cipher.parameters
        val iv = params.getParameterSpec(IvParameterSpec::class.java).iv
        val ciphertext = cipher.doFinal(hetu.toString().toByteArray(StandardCharsets.UTF_8))
        val user = userRepository.save(
            User(
                login = UUID.randomUUID().toString(),
                firstName = firstName,
                lastName = lastName,
                activated = true,
                hetu = ciphertext,
                initVector = iv
            )
        )

        return UserDTO(user)
    }

    override fun updateUserDetails(
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

    override fun updateEmail(email: String, userId: String) {
        val user = userRepository.findById(userId).orElseThrow { EntityNotFoundException(KAYTTAJA_NOT_FOUND_ERROR) }
        user.email = email
    }

    override fun createOrUpdateUserWithToken(
        tokenUser: User?,
        token: VerificationToken,
        cipher: Cipher,
        originalKey: SecretKey,
        hetu: String?,
        eppn: String?,
        firstName: String,
        lastName: String
    ) {
        tokenUser?.let {
            validateUserFirstAndLastName(tokenUser, firstName, lastName, eppn)
            val existingUser =
                findExistingUser(cipher, originalKey, hetu, eppn)

            // Yhdistä käyttäjä jos löytyy
            if (existingUser != null) {
                val existingKayttaja =
                    kayttajaRepository.findOneByUserId(existingUser.id!!)
                        .orElseThrow { EntityNotFoundException(KAYTTAJA_NOT_FOUND_ERROR) }
                val tokenKayttaja =
                    kayttajaRepository.findOneByUserId(tokenUser.id!!)
                        .orElseThrow { EntityNotFoundException(KAYTTAJA_NOT_FOUND_ERROR) }

                existingKayttaja.takeIf { t -> t.tila == KayttajatilinTila.KUTSUTTU }
                    ?.apply { KayttajatilinTila.AKTIIVINEN }

                updateKouluttajaReferences(
                    tokenKayttaja.id!!,
                    existingKayttaja.id!!
                )

                copyTokenUserAuthorities(existingUser, tokenUser)
                deleteTokenUser(tokenKayttaja, token, tokenUser)
                existingUser.email = tokenUser.email?.lowercase()
                copyTokenUserYliopistotAndErikoisalat(tokenKayttaja, existingKayttaja)
                kayttajaRepository.save(existingKayttaja)
                userRepository.save(existingUser)
            } else {
                cipher.init(Cipher.ENCRYPT_MODE, originalKey)
                val params = cipher.parameters
                val iv = params.getParameterSpec(IvParameterSpec::class.java).iv
                val ciphertext = if (hetu != null)
                    cipher.doFinal(
                        hetu.toString().toByteArray(StandardCharsets.UTF_8)
                    ) else null

                tokenUser.hetu = ciphertext
                tokenUser.eppn = eppn
                tokenUser.initVector = iv
                tokenUser.firstName = firstName
                tokenUser.lastName = lastName

                userRepository.save(tokenUser)
                val kayttaja =
                    kayttajaRepository.findOneByUserId(tokenUser.id!!)
                        .orElseThrow { EntityNotFoundException(KAYTTAJA_NOT_FOUND_ERROR) }
                kayttaja.tila = KayttajatilinTila.AKTIIVINEN
                verificationTokenRepository.delete(token)
            }
        }
    }

    @Transactional(readOnly = true)
    override fun existsByEmail(email: String): Boolean {
        return userRepository.findOneByEmail(email).isPresent
    }

    @Transactional(readOnly = true)
    override fun existsByEppn(eppn: String): Boolean {
        return userRepository.findOneByEppn(eppn).isPresent
    }

    @Transactional(readOnly = true)
    override fun findExistingUser(
        cipher: Cipher,
        originalKey: SecretKey,
        hetu: String?,
        eppn: String?
    ): User? {
        if (hetu != null) {
            userRepository.findAllWithAuthorities().filter { u -> u.hetu != null }.forEach { u ->
                cipher.init(Cipher.DECRYPT_MODE, originalKey, IvParameterSpec(u.initVector))
                val userHetu = String(cipher.doFinal(u.hetu), StandardCharsets.UTF_8)
                if (userHetu == hetu) {
                    return u
                }
            }
        } else if (eppn != null) {
            userRepository.findAllWithAuthorities().filter { u -> u.eppn != null }.forEach { u ->
                if (u.eppn == eppn) {
                    return u
                }
            }
        }

        return null
    }

    private fun updateKouluttajaReferences(oldId: Long, newId: Long) {
        kouluttajavaltuutusRepository.changeKouluttaja(oldId, newId)
        suoritusarviointiRepository.changeKouluttaja(oldId, newId)
        koejaksonKoulutussopimusRepository.changeKouluttaja(oldId, newId)
        koejaksonAloituskeskusteluRepository.changeKouluttaja(oldId, newId)
        koejaksonAloituskeskusteluRepository.changeEsimies(oldId, newId)
        koejaksonValiarviointiRepository.changeKouluttaja(oldId, newId)
        koejaksonValiarviointiRepository.changeEsimies(oldId, newId)
        koejaksonKehittamistoimenpiteetRepository.changeKouluttaja(oldId, newId)
        koejaksonKehittamistoimenpiteetRepository.changeEsimies(oldId, newId)
        koejaksonLoppukeskusteluRepository.changeKouluttaja(oldId, newId)
        koejaksonLoppukeskusteluRepository.changeEsimies(oldId, newId)
    }

    private fun validateUserFirstAndLastName(
        tokenUser: User,
        firstName: String,
        lastName: String,
        eppn: String?
    ) {
        // Kutsutun käyttäjä etu- ja sukunimi tulee olla tarpeeksi lähellä
        // kutsussa syötettyjä tietoja
        val distance = LevenshteinDistance()
        val firstNameDistFirst = distance.apply(tokenUser.firstName, firstName)
        val firstNameDistLast = distance.apply(tokenUser.firstName, lastName)
        val lastNameDistFirst = distance.apply(tokenUser.lastName, firstName)
        val lastNameDistLast = distance.apply(tokenUser.lastName, lastName)
        val treshhold = 2
        if ((firstNameDistFirst > treshhold && firstNameDistLast > treshhold)
            || (lastNameDistFirst > treshhold && lastNameDistLast > treshhold)
        ) {
            log.error(
                "Kirjautuminen epäonnistui käyttäjälle $firstName $lastName (eppn $eppn). " +
                    "Kutsussa annettu nimi ${tokenUser.firstName} ${tokenUser.lastName} ei ole" +
                    "tarpeeksi lähellä käyttäjän nimeä."
            )
            throw Exception(LoginException.VIRHEELLINEN_NIMI.name)
        }
    }

    private fun copyTokenUserAuthorities(
        existingUser: User,
        tokenUser: User
    ) {
        existingUser.authorities.clear()
        existingUser.authorities.addAll(tokenUser.authorities)
    }

    private fun deleteTokenUser(
        tokenKayttaja: Kayttaja,
        token: VerificationToken,
        tokenUser: User
    ) {
        kayttajaRepository.delete(tokenKayttaja)
        verificationTokenRepository.delete(token)
        userRepository.delete(tokenUser)
        entityManager.flush()
    }

    private fun copyTokenUserYliopistotAndErikoisalat(
        tokenKayttaja: Kayttaja,
        existingKayttaja: Kayttaja
    ) {
        tokenKayttaja.yliopistotAndErikoisalat.map {
            val movedKayttajaYliopistoErikoisala = KayttajaYliopistoErikoisala(
                kayttaja = existingKayttaja,
                yliopisto = it.yliopisto,
                erikoisala = it.erikoisala
            )
            kayttajaYliopistoErikoisalaRepository.save(movedKayttajaYliopistoErikoisala)
            existingKayttaja.yliopistotAndErikoisalat.add(movedKayttajaYliopistoErikoisala)
        }
    }
}
