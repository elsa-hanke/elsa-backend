package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.Authority
import fi.elsapalvelu.elsa.domain.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.Kayttaja
import fi.elsapalvelu.elsa.domain.Yliopisto
import fi.elsapalvelu.elsa.repository.ErikoisalaRepository
import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.KayttajaRepository
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.security.KOULUTTAJA
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.service.KayttajaService
import fi.elsapalvelu.elsa.service.dto.KayttajaDTO
import fi.elsapalvelu.elsa.service.dto.KayttooikeusHakemusDTO
import fi.elsapalvelu.elsa.service.mapper.KayttajaMapper
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
import java.util.*

@Service
@Transactional
class KayttajaServiceImpl(
    private val kayttajaMapper: KayttajaMapper,
    private val kayttajaRepository: KayttajaRepository,
    private val erikoisalaRepository: ErikoisalaRepository,
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository,
) : KayttajaService {

    override fun save(kayttajaDTO: KayttajaDTO): KayttajaDTO {
        var kayttaja = kayttajaMapper.toEntity(kayttajaDTO)
        kayttaja = kayttajaRepository.save(kayttaja)
        return kayttajaMapper.toDto(kayttaja)
    }

    @Transactional(readOnly = true)
    override fun findAll(): List<KayttajaDTO> {
        return kayttajaRepository.findAll()
            .map(kayttajaMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: String): Optional<KayttajaDTO> {
        return kayttajaRepository.findById(id)
            .map(kayttajaMapper::toDto)
    }

    override fun delete(id: String) {
        kayttajaRepository.deleteById(id)
    }

    @Transactional(readOnly = true)
    override fun findKouluttajat(): List<KayttajaDTO> {
        return kayttajaRepository.findAllByAuthority(KOULUTTAJA)
            .map(kayttajaMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findVastuuhenkilot(): List<KayttajaDTO> {
        return kayttajaRepository.findAllByAuthority(VASTUUHENKILO)
            .map(kayttajaMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findKouluttajatAndVastuuhenkilot(kayttajaId: String): List<KayttajaDTO> {
        val existingKayttaja = kayttajaRepository.findById(kayttajaId).get()
        return (kayttajaRepository.findAllByAuthority(KOULUTTAJA) +
            kayttajaRepository.findAllByAuthorityAndYliopistoId(
                VASTUUHENKILO,
                existingKayttaja.yliopisto?.id
            ))
            .map(kayttajaMapper::toDto)
    }

    override fun updateKayttajaAuthorities(principal: Principal?, kayttooikeusHakemusDTO: KayttooikeusHakemusDTO) {
        // TODO: tarkista käyttöoikeus opintotietojärjestelmästä

        // Lisätään käyttäjälle erikoistuva lääkäri rooli
        val kayttajaDTO = getAuthenticatedKayttaja(principal)

        var kayttaja = kayttajaRepository.findById(kayttajaDTO.id!!).get()
        kayttaja.authorities.add(Authority(name = ERIKOISTUVA_LAAKARI))
        kayttaja.yliopisto = Yliopisto(id = kayttooikeusHakemusDTO.yliopisto)
        kayttaja = kayttajaRepository.save(kayttaja)

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

    @Transactional(readOnly = true)
    override fun getAuthenticatedKayttaja(principal: Principal?): KayttajaDTO {
        if (principal is Saml2Authentication) {
            return getKayttajaFromAuthentication(principal)
        } else {
            throw RuntimeException("Käyttäjä ei ole kirjautunut")
        }
    }

    // TODO: @Transactional(readOnly = true) should be?
    fun getKayttajaFromAuthentication(authToken: Saml2Authentication): KayttajaDTO {
        val principal = authToken.principal as Saml2AuthenticatedPrincipal

        val kayttaja = Kayttaja()
        kayttaja.id = principal.name
        kayttaja.etunimi = principal.getFirstAttribute("urn:oid:2.5.4.42")
        kayttaja.sukunimi = principal.getFirstAttribute("urn:oid:2.5.4.4")
        kayttaja.authorities = authToken.authorities.map(GrantedAuthority::getAuthority)
            .map { Authority(name = it) }
            .toMutableSet()

        return kayttajaMapper.toDto(kayttaja)
    }

    override fun existsByEmail(sahkopostiosoite: String): Boolean {
        return kayttajaRepository.findOneBySahkopostiosoite(sahkopostiosoite).isPresent
    }
}
