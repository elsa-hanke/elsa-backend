package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.KayttajaRepository
import fi.elsapalvelu.elsa.repository.OpiskeluoikeusRepository
import fi.elsapalvelu.elsa.repository.UserRepository
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.dto.ErikoistuvaLaakariDTO
import fi.elsapalvelu.elsa.service.dto.kayttajahallinta.KayttajahallintaErikoistuvaLaakariDTO
import fi.elsapalvelu.elsa.service.mapper.ErikoisalaMapper
import fi.elsapalvelu.elsa.service.mapper.ErikoistuvaLaakariMapper
import fi.elsapalvelu.elsa.service.mapper.YliopistoMapper
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId
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
    private val opiskeluoikeusRepository: OpiskeluoikeusRepository,
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
        var user = User(
            login = kayttajahallintaErikoistuvaLaakariDTO.sahkopostiosoite,
            firstName = kayttajahallintaErikoistuvaLaakariDTO.etunimi,
            lastName = kayttajahallintaErikoistuvaLaakariDTO.sukunimi,
            email = kayttajahallintaErikoistuvaLaakariDTO.sahkopostiosoite,
            activated = true,
            authorities = mutableSetOf(Authority(ERIKOISTUVA_LAAKARI))
        )
        user = userRepository.save(user)


        var kayttaja = Kayttaja(
            user = user,
        )
        kayttaja = kayttajaRepository.save(kayttaja)

        var erikoistuvaLaakari = ErikoistuvaLaakari(
            kayttaja = kayttaja,
        )
        erikoistuvaLaakari = erikoistuvaLaakariRepository.save(erikoistuvaLaakari)

        var opiskeluoikeus = Opiskeluoikeus(
            opintooikeudenMyontamispaiva = kayttajahallintaErikoistuvaLaakariDTO.opiskeluoikeusAlkaa,
            opintooikeudenPaattymispaiva = kayttajahallintaErikoistuvaLaakariDTO.opiskeluoikeusPaattyy,
            opintosuunnitelmaKaytossaPvm = kayttajahallintaErikoistuvaLaakariDTO.opintosuunnitelmaKaytossaPvm,
            opiskelijatunnus = kayttajahallintaErikoistuvaLaakariDTO.opiskelijatunnus,
            erikoistuvaLaakari = erikoistuvaLaakari,
            yliopisto = yliopistoMapper.toEntity(
                yliopistoService.findOne(kayttajahallintaErikoistuvaLaakariDTO.yliopistoId!!).orElse(null)
            ),
            erikoisala = erikoisalaMapper.toEntity(
                erikoisalaService.findOne(kayttajahallintaErikoistuvaLaakariDTO.erikoisalaId!!).orElse(null)
            )
        )
        opiskeluoikeus = opiskeluoikeusRepository.save(opiskeluoikeus)

        erikoistuvaLaakari.opiskeluoikeudet.add(opiskeluoikeus)
        erikoistuvaLaakari = erikoistuvaLaakariRepository.save(erikoistuvaLaakari)

        val token = verificationTokenService.save(user.id!!)
        mailService.sendEmailFromTemplate(
            User(email = user.email),
            "uusiErikoistuvaLaakari.html",
            "email.uusierikoistuvalaakari.title",
            properties = mapOf(
                Pair(MailProperty.ID, token),
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

    override fun resendInvitation(id: Long) {
        erikoistuvaLaakariRepository.findByIdOrNull(id)?.let { erikoistuvaLaakari ->
            verificationTokenService.findOne(erikoistuvaLaakari.kayttaja?.user?.id!!)?.let { token ->
                mailService.sendEmailFromTemplate(
                    erikoistuvaLaakari.kayttaja?.user!!,
                    "uusiErikoistuvaLaakari.html",
                    "email.uusierikoistuvalaakari.title",
                    properties = mapOf(
                        Pair(MailProperty.ID, token),
                    )
                )
            }
        }
    }

    override fun fixOpiskeluoikeudet() {
        erikoistuvaLaakariRepository.findAll().forEach {
            if (it.opiskeluoikeudet.isEmpty()) {
                var opiskeluoikeus = Opiskeluoikeus(
                    opintooikeudenMyontamispaiva = LocalDate.now(ZoneId.systemDefault()),
                    opintooikeudenPaattymispaiva = LocalDate.now(ZoneId.systemDefault()),
                    opiskelijatunnus = "123456",
                    opintosuunnitelmaKaytossaPvm = LocalDate.now(ZoneId.systemDefault()),
                    erikoistuvaLaakari = it,
                    yliopisto = yliopistoMapper.toEntity(
                        yliopistoService.findAll().last()
                    ),
                    erikoisala = erikoisalaMapper.toEntity(
                        erikoisalaService.findOne(46).orElse(null)
                    )
                )
                opiskeluoikeus = opiskeluoikeusRepository.save(opiskeluoikeus)

                it.opiskeluoikeudet.add(opiskeluoikeus)
            }
        }
    }
}
