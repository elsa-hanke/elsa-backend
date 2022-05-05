package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.domain.enumeration.OpintooikeudenTila
import fi.elsapalvelu.elsa.repository.*
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
import java.time.Instant
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
    private val opintooikeusRepository: OpintooikeusRepository,
    private val verificationTokenService: VerificationTokenService,
    private val mailService: MailService,
    private val asetusRepository: AsetusRepository,
    private val opintoopasRepository: OpintoopasRepository
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

        val asetus =
            asetusRepository.findByIdOrNull(kayttajahallintaErikoistuvaLaakariDTO.asetusId!!)
        val opintoopas =
            opintoopasRepository.findByIdOrNull(kayttajahallintaErikoistuvaLaakariDTO.opintoopasId!!)

        // Asetetaan mahdollisesti muille olemassaoleville opinto-oikeuksille kaytossa = false, koska käytössä voi
        // olla vain yksi kerrallaan.
        erikoistuvaLaakari.opintooikeudet.forEach {
            it.kaytossa = false
        }

        var opintooikeus = Opintooikeus(
            opintooikeudenMyontamispaiva = kayttajahallintaErikoistuvaLaakariDTO.opintooikeusAlkaa,
            opintooikeudenPaattymispaiva = kayttajahallintaErikoistuvaLaakariDTO.opintooikeusPaattyy,
            opiskelijatunnus = kayttajahallintaErikoistuvaLaakariDTO.opiskelijatunnus,
            osaamisenArvioinninOppaanPvm = kayttajahallintaErikoistuvaLaakariDTO.osaamisenArvioinninOppaanPvm,
            erikoistuvaLaakari = erikoistuvaLaakari,
            yliopisto = yliopistoMapper.toEntity(
                yliopistoService.findOne(kayttajahallintaErikoistuvaLaakariDTO.yliopistoId!!)
                    .orElse(null)
            ),
            erikoisala = erikoisalaMapper.toEntity(
                erikoisalaService.findOne(kayttajahallintaErikoistuvaLaakariDTO.erikoisalaId!!)
                    .orElse(null)
            ),
            asetus = asetus,
            opintoopas = opintoopas,
            kaytossa = true,
            tila = OpintooikeudenTila.AKTIIVINEN,
            muokkausaika = Instant.now()
        )
        opintooikeus = opintooikeusRepository.save(opintooikeus)

        erikoistuvaLaakari.opintooikeudet.add(opintooikeus)
        erikoistuvaLaakari = erikoistuvaLaakariRepository.save(erikoistuvaLaakari)

        val token = verificationTokenService.save(user.id!!)
        mailService.sendEmailFromTemplate(
            User(email = user.email),
            templateName = "uusiErikoistuvaLaakari.html",
            titleKey = "email.uusierikoistuvalaakari.title",
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

    override fun findAllForVastuuhenkilo(kayttajaId: Long): List<ErikoistuvaLaakariDTO> {
        val erikoistuvatLaakarit: MutableSet<ErikoistuvaLaakariDTO> = mutableSetOf()
        val kayttaja = kayttajaRepository.findById(kayttajaId).orElse(null)
        kayttaja?.yliopistotAndErikoisalat?.forEach {
            erikoistuvatLaakarit.addAll(
                erikoistuvaLaakariRepository.findAllForVastuuhenkilo(
                    it.erikoisala?.id!!,
                    it.yliopisto?.id!!
                )
                    .map(erikoistuvaLaakariMapper::toDto)
            )
        }
        return erikoistuvatLaakarit.toList()
    }

    override fun resendInvitation(id: Long) {
        erikoistuvaLaakariRepository.findByIdOrNull(id)?.let { erikoistuvaLaakari ->
            verificationTokenService.findOne(erikoistuvaLaakari.kayttaja?.user?.id!!)
                ?.let { token ->
                    mailService.sendEmailFromTemplate(
                        erikoistuvaLaakari.kayttaja?.user!!,
                        templateName = "uusiErikoistuvaLaakari.html",
                        titleKey = "email.uusierikoistuvalaakari.title",
                        properties = mapOf(
                            Pair(MailProperty.ID, token),
                        )
                    )
                }
        }
    }
}
