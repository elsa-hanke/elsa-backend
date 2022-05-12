package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.KoejaksonVastuuhenkilonArvio
import fi.elsapalvelu.elsa.domain.enumeration.VastuuhenkilonTehtavatyyppiEnum
import fi.elsapalvelu.elsa.repository.*
import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.dto.KoejaksonVastuuhenkilonArvioDTO
import fi.elsapalvelu.elsa.service.mapper.KoejaksonVastuuhenkilonArvioMapper
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import javax.persistence.EntityNotFoundException


@Service
@Transactional
class KoejaksonVastuuhenkilonArvioServiceImpl(
    private val koejaksonVastuuhenkilonArvioRepository: KoejaksonVastuuhenkilonArvioRepository,
    private val koejaksonVastuuhenkilonArvioMapper: KoejaksonVastuuhenkilonArvioMapper,
    private val mailService: MailService,
    private val kayttajaRepository: KayttajaRepository,
    private val userRepository: UserRepository,
    private val kayttajaService: KayttajaService,
    private val opintooikeusRepository: OpintooikeusRepository,
    private val tyoskentelyjaksoService: TyoskentelyjaksoService,
    private val koejaksonAloituskeskusteluService: KoejaksonAloituskeskusteluService,
    private val koejaksonValiarviointiService: KoejaksonValiarviointiService,
    private val koejaksonKehittamistoimenpiteetService: KoejaksonKehittamistoimenpiteetService,
    private val koejaksonLoppukeskusteluService: KoejaksonLoppukeskusteluService,
    private val opintooikeusService: OpintooikeusService,
    private val koulutussopimusRepository: KoejaksonKoulutussopimusRepository
) : KoejaksonVastuuhenkilonArvioService {

    override fun create(
        koejaksonVastuuhenkilonArvioDTO: KoejaksonVastuuhenkilonArvioDTO,
        opintooikeusId: Long
    ): KoejaksonVastuuhenkilonArvioDTO? {
        return opintooikeusRepository.findByIdOrNull(opintooikeusId)?.let {
            validateVastuuhenkilo(
                it.erikoistuvaLaakari?.kayttaja?.user?.id!!,
                koejaksonVastuuhenkilonArvioDTO
            )
            var vastuuhenkilonArvio =
                koejaksonVastuuhenkilonArvioMapper.toEntity(koejaksonVastuuhenkilonArvioDTO)
            vastuuhenkilonArvio.opintooikeus = it
            vastuuhenkilonArvio.virkailija = null
            vastuuhenkilonArvio = koejaksonVastuuhenkilonArvioRepository.save(vastuuhenkilonArvio)

            it.erikoistuvaLaakari?.kayttaja?.user?.let { user ->
                user.email = koejaksonVastuuhenkilonArvioDTO.erikoistuvanSahkoposti
                user.phoneNumber = koejaksonVastuuhenkilonArvioDTO.erikoistuvanPuhelinnumero
                userRepository.save(user)
            }

            // Sähköposti vastuuhenkilölle
            mailService.sendEmailFromTemplate(
                kayttajaRepository.findById(vastuuhenkilonArvio.vastuuhenkilo?.id!!).get().user!!,
                templateName = "vastuuhenkilonArvioKuitattava.html",
                titleKey = "email.vastuuhenkilonarviokuitattava.title",
                properties = mapOf(Pair(MailProperty.ID, vastuuhenkilonArvio.id!!.toString()))
            )

            koejaksonVastuuhenkilonArvioMapper.toDto(vastuuhenkilonArvio)
        }
    }

    private fun validateVastuuhenkilo(
        userId: String,
        vastuuhenkilonArvioDTO: KoejaksonVastuuhenkilonArvioDTO
    ) {
        if (kayttajaService.findVastuuhenkiloByTehtavatyyppi(
                userId,
                VastuuhenkilonTehtavatyyppiEnum.KOEJAKSOSOPIMUSTEN_JA_KOEJAKSOJEN_HYVAKSYMINEN
            ).id != vastuuhenkilonArvioDTO.vastuuhenkilo?.id
        ) {
            throw java.lang.IllegalArgumentException("Vastuuhenkilöä ei saa vaihtaa")
        }
    }

    override fun update(
        koejaksonVastuuhenkilonArvioDTO: KoejaksonVastuuhenkilonArvioDTO,
        userId: String
    ): KoejaksonVastuuhenkilonArvioDTO {
        var vastuuhenkilonArvio =
            koejaksonVastuuhenkilonArvioRepository.findById(koejaksonVastuuhenkilonArvioDTO.id!!)
                .orElseThrow { EntityNotFoundException("Vastuuhenkilön arviota ei löydy") }

        val updatedVastuuhenkilonArvio =
            koejaksonVastuuhenkilonArvioMapper.toEntity(koejaksonVastuuhenkilonArvioDTO)

        if (vastuuhenkilonArvio.vastuuhenkilo?.user?.id == userId) {
            vastuuhenkilonArvio.apply {
                vastuuhenkiloHyvaksynyt = true
                vastuuhenkilonKuittausaika = LocalDate.now(ZoneId.systemDefault())
                koejaksoHyvaksytty = updatedVastuuhenkilonArvio.koejaksoHyvaksytty
            }.takeIf { it.koejaksoHyvaksytty == false }?.apply {
                perusteluHylkaamiselle = updatedVastuuhenkilonArvio.perusteluHylkaamiselle
                hylattyArviointiKaytyLapiKeskustellen =
                    updatedVastuuhenkilonArvio.hylattyArviointiKaytyLapiKeskustellen
            }
        }

        vastuuhenkilonArvio = koejaksonVastuuhenkilonArvioRepository.save(vastuuhenkilonArvio)

        if (vastuuhenkilonArvio.vastuuhenkilo?.user?.id == userId) {

            vastuuhenkilonArvio.vastuuhenkilo?.user?.let { user ->
                user.email = koejaksonVastuuhenkilonArvioDTO.vastuuhenkilonSahkoposti
                user.phoneNumber = koejaksonVastuuhenkilonArvioDTO.vastuuhenkilonPuhelinnumero
                userRepository.save(user)
            }

            // Sähköposti erikoistuvalle vastuuhenkilon kuittaamasta arviosta
            if (vastuuhenkilonArvio.vastuuhenkilonKuittausaika != null) {
                mailService.sendEmailFromTemplate(
                    kayttajaRepository.findById(vastuuhenkilonArvio?.opintooikeus?.erikoistuvaLaakari?.kayttaja?.id!!)
                        .get().user!!,
                    templateName = "vastuuhenkilonArvioKuitattava.html",
                    titleKey = "email.vastuuhenkilonarviokuitattava.title",
                    properties = mapOf(Pair(MailProperty.ID, vastuuhenkilonArvio.id!!.toString()))
                )
            }
        }

        return koejaksonVastuuhenkilonArvioMapper.toDto(vastuuhenkilonArvio)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<KoejaksonVastuuhenkilonArvioDTO> {
        return koejaksonVastuuhenkilonArvioRepository.findById(id)
            .map(this::mapVastuuhenkilonArvio)
    }

    @Transactional(readOnly = true)
    override fun findByOpintooikeusId(opintooikeusId: Long): Optional<KoejaksonVastuuhenkilonArvioDTO> {
        return koejaksonVastuuhenkilonArvioRepository.findByOpintooikeusId(opintooikeusId)
            .map(this::mapVastuuhenkilonArvio)
    }

    @Transactional(readOnly = true)
    override fun findOneByIdAndVastuuhenkiloUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonVastuuhenkilonArvioDTO> {
        return koejaksonVastuuhenkilonArvioRepository.findOneByIdAndVastuuhenkiloUserId(id, userId)
            .map(this::mapVastuuhenkilonArvio)
    }

    override fun delete(id: Long) {
        koejaksonVastuuhenkilonArvioRepository.deleteById(id)
    }

    private fun mapVastuuhenkilonArvio(vastuuhenkilonArvio: KoejaksonVastuuhenkilonArvio): KoejaksonVastuuhenkilonArvioDTO {
        val result = koejaksonVastuuhenkilonArvioMapper.toDto(vastuuhenkilonArvio)
        val opintoOikeusId = vastuuhenkilonArvio.opintooikeus?.id!!
        result.koejaksonSuorituspaikat =
            tyoskentelyjaksoService.findAllByOpintooikeusId(opintoOikeusId)
        result.aloituskeskustelu =
            koejaksonAloituskeskusteluService.findByOpintooikeusId(opintoOikeusId)
                .orElse(null)
        result.valiarviointi =
            koejaksonValiarviointiService.findByOpintooikeusId(opintoOikeusId).orElse(null)
        result.kehittamistoimenpiteet =
            koejaksonKehittamistoimenpiteetService.findByOpintooikeusId(opintoOikeusId).orElse(null)
        result.loppukeskustelu =
            koejaksonLoppukeskusteluService.findByOpintooikeusId(opintoOikeusId).orElse(null)
        result.muutOpintooikeudet =
            opintooikeusService.findAllValidByErikoistuvaLaakariKayttajaUserId(vastuuhenkilonArvio.opintooikeus?.erikoistuvaLaakari?.kayttaja?.user?.id!!)
                .filter { it.id != opintoOikeusId }
        val koulutussopimus = koulutussopimusRepository.findByOpintooikeusId(opintoOikeusId)
        result.koulutussopimusHyvaksytty =
            koulutussopimus.isPresent && koulutussopimus.get().vastuuhenkiloHyvaksynyt == true
        return result
    }
}
