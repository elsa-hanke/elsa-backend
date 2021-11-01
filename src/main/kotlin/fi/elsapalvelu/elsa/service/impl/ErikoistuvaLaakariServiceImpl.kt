package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.Authority
import fi.elsapalvelu.elsa.domain.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.Kayttaja
import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.KayttajaRepository
import fi.elsapalvelu.elsa.repository.UserRepository
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.dto.ErikoistuvaLaakariDTO
import fi.elsapalvelu.elsa.service.dto.kayttajahallinta.KayttajahallintaErikoistuvaLaakariDTO
import fi.elsapalvelu.elsa.service.mapper.ErikoisalaMapper
import fi.elsapalvelu.elsa.service.mapper.ErikoistuvaLaakariMapper
import fi.elsapalvelu.elsa.service.mapper.YliopistoMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional
class ErikoistuvaLaakariServiceImpl(
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository,
    private val erikoistuvaLaakariMapper: ErikoistuvaLaakariMapper,
    private val yliopistoService: YliopistoService,
    private val yliopistoMapper: YliopistoMapper,
    private val erikoisalaService: ErikoisalaService,
    private val erikoisalaMapper: ErikoisalaMapper,
    private val userRepository: UserRepository,
    private val kayttajaRepository: KayttajaRepository,
    private val verificationTokenService: VerificationTokenService,
    private val mailService: MailService
) : ErikoistuvaLaakariService {

    override fun save(
        erikoistuvaLaakariDTO: ErikoistuvaLaakariDTO
    ): ErikoistuvaLaakariDTO {
        var erikoistuvaLaakari = erikoistuvaLaakariMapper.toEntity(erikoistuvaLaakariDTO)
        erikoistuvaLaakari = erikoistuvaLaakariRepository.save(erikoistuvaLaakari)
        return erikoistuvaLaakariMapper.toDto(erikoistuvaLaakari)
    }

    override fun save(
        kayttajahallintaErikoistuvaLaakariDTO: KayttajahallintaErikoistuvaLaakariDTO
    ): ErikoistuvaLaakariDTO {
        var user = User()
        user.login = kayttajahallintaErikoistuvaLaakariDTO.sahkopostiosoite
        user.authorities.add(Authority(ERIKOISTUVA_LAAKARI))
        user.firstName = kayttajahallintaErikoistuvaLaakariDTO.etunimi
        user.lastName = kayttajahallintaErikoistuvaLaakariDTO.sukunimi
        user.email = kayttajahallintaErikoistuvaLaakariDTO.sahkopostiosoite
        user.activated = true

        user = userRepository.save(user)


        var kayttaja = Kayttaja()
        kayttaja.user = user
        kayttaja.yliopisto = yliopistoMapper.toEntity(
            yliopistoService.findOne(kayttajahallintaErikoistuvaLaakariDTO.yliopistoId!!).orElse(null)
        )

        kayttaja = kayttajaRepository.save(kayttaja)

        var erikoistuvaLaakari = ErikoistuvaLaakari()
        erikoistuvaLaakari.kayttaja = kayttaja
        erikoistuvaLaakari.opiskelijatunnus = kayttajahallintaErikoistuvaLaakariDTO.opiskelijatunnus
        erikoistuvaLaakari.opintosuunnitelmaKaytossaPvm =
            kayttajahallintaErikoistuvaLaakariDTO.opintosuunnitelmaKaytossaPvm
        erikoistuvaLaakari.opintooikeudenMyontamispaiva = kayttajahallintaErikoistuvaLaakariDTO.opiskeluoikeusAlkaa
        erikoistuvaLaakari.opintooikeudenPaattymispaiva = kayttajahallintaErikoistuvaLaakariDTO.opiskeluoikeusPaattyy
        erikoistuvaLaakari.erikoisala = erikoisalaMapper.toEntity(
            erikoisalaService.findOne(kayttajahallintaErikoistuvaLaakariDTO.erikoisalaId!!).orElse(null)
        )

        erikoistuvaLaakari = erikoistuvaLaakariRepository.save(erikoistuvaLaakari)

        val token = verificationTokenService.save(user.id!!)
        mailService.sendEmailFromTemplate(
            User(email = user.email),
            "uusiErikoistuvaLaakari.html",
            "email.uusierikoistuvalaakari.title",
            properties = mapOf(
                Pair(MailProperty.ID, token),
                Pair(MailProperty.NAME, user.firstName!!)
            )
        )

        return erikoistuvaLaakariMapper.toDto(erikoistuvaLaakari)
    }

    @Transactional(readOnly = true)
    override fun findAll(): List<ErikoistuvaLaakariDTO> {
        return erikoistuvaLaakariRepository.findAll()
            .map(erikoistuvaLaakariMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<ErikoistuvaLaakariDTO> {
        return erikoistuvaLaakariRepository.findById(id)
            .map(erikoistuvaLaakariMapper::toDto)
    }

    override fun delete(id: Long) {
        erikoistuvaLaakariRepository.deleteById(id)
    }

    override fun findOneByKayttajaUserId(
        userId: String
    ): ErikoistuvaLaakariDTO? {
        erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)?.let {
            return erikoistuvaLaakariMapper.toDto(it)
        }

        return null
    }

    override fun findOneByKayttajaId(
        kayttajaId: Long
    ): ErikoistuvaLaakariDTO? {
        erikoistuvaLaakariRepository.findOneByKayttajaId(kayttajaId)?.let {
            return erikoistuvaLaakariMapper.toDto(it)
        }

        return null
    }
}
