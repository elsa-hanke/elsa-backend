package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.KayttajaRepository
import fi.elsapalvelu.elsa.repository.KoejaksonVastuuhenkilonArvioRepository
import fi.elsapalvelu.elsa.repository.OpintooikeusRepository
import fi.elsapalvelu.elsa.service.KoejaksonVastuuhenkilonArvioService
import fi.elsapalvelu.elsa.service.MailProperty
import fi.elsapalvelu.elsa.service.MailService
import fi.elsapalvelu.elsa.service.dto.KoejaksonVastuuhenkilonArvioDTO
import fi.elsapalvelu.elsa.service.mapper.KayttajaMapper
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
    private val kayttajaMapper: KayttajaMapper,
    private val opintooikeusRepository: OpintooikeusRepository
) : KoejaksonVastuuhenkilonArvioService {

    override fun create(
        koejaksonVastuuhenkilonArvioDTO: KoejaksonVastuuhenkilonArvioDTO,
        opintooikeusId: Long
    ): KoejaksonVastuuhenkilonArvioDTO? {
        return opintooikeusRepository.findByIdOrNull(opintooikeusId)?.let {
            var vastuuhenkilonArvio =
                koejaksonVastuuhenkilonArvioMapper.toEntity(koejaksonVastuuhenkilonArvioDTO)
            vastuuhenkilonArvio.opintooikeus = it
            vastuuhenkilonArvio = koejaksonVastuuhenkilonArvioRepository.save(vastuuhenkilonArvio)

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

    override fun update(
        koejaksonVastuuhenkilonArvioDTO: KoejaksonVastuuhenkilonArvioDTO,
        userId: String
    ): KoejaksonVastuuhenkilonArvioDTO {
        var vastuuhenkilonArvio =
            koejaksonVastuuhenkilonArvioRepository.findById(koejaksonVastuuhenkilonArvioDTO.id!!)
                .orElseThrow { EntityNotFoundException("Vastuuhenkilön arviota ei löydy") }

        val updatedVastuuhenkilonArvio =
            koejaksonVastuuhenkilonArvioMapper.toEntity(koejaksonVastuuhenkilonArvioDTO)

        if (vastuuhenkilonArvio.opintooikeus?.erikoistuvaLaakari?.kayttaja?.user?.id == userId
            && vastuuhenkilonArvio.vastuuhenkilonKuittausaika != null
        ) {
            vastuuhenkilonArvio.erikoistuvaAllekirjoittanut = true
            vastuuhenkilonArvio.erikoistuvanAllekirjoitusaika = LocalDate.now()
        }

        if (vastuuhenkilonArvio.vastuuhenkilo?.user?.id == userId) {
            vastuuhenkilonArvio.apply {
                vastuuhenkiloAllekirjoittanut = true
                vastuuhenkilonKuittausaika = LocalDate.now(ZoneId.systemDefault())
                koejaksoHyvaksytty = updatedVastuuhenkilonArvio.koejaksoHyvaksytty
            }.takeIf { it.koejaksoHyvaksytty == false }?.apply {
                perusteluHylkaamiselle = updatedVastuuhenkilonArvio.perusteluHylkaamiselle
                hylattyArviointiKaytyLapiKeskustellen = updatedVastuuhenkilonArvio.hylattyArviointiKaytyLapiKeskustellen
            }
        }

        vastuuhenkilonArvio = koejaksonVastuuhenkilonArvioRepository.save(vastuuhenkilonArvio)

        // Sähköposti vastuuhenkilölle allekirjoitetusta loppukeskustelusta
        if (vastuuhenkilonArvio.opintooikeus?.erikoistuvaLaakari?.kayttaja?.user?.id == userId
            && vastuuhenkilonArvio.erikoistuvaAllekirjoittanut
        ) {
            val erikoistuvaLaakari =
                kayttajaRepository.findById(vastuuhenkilonArvio?.opintooikeus?.erikoistuvaLaakari?.kayttaja?.id!!)
                    .get().user!!
            mailService.sendEmailFromTemplate(
                kayttajaRepository.findById(vastuuhenkilonArvio.vastuuhenkilo?.id!!)
                    .get().user!!,
                templateName = "vastuuhenkilonArvioHyvaksytty.html",
                titleKey = "email.vastuuhenkilonarviohyvaksytty.title",
                properties = mapOf(
                    Pair(MailProperty.ID, vastuuhenkilonArvio.id!!.toString()),
                    Pair(MailProperty.NAME, erikoistuvaLaakari.getName())
                )
            )
        } else if (vastuuhenkilonArvio.vastuuhenkilo?.user?.id == userId) {

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
            .map(koejaksonVastuuhenkilonArvioMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findByOpintooikeusId(opintooikeusId: Long): Optional<KoejaksonVastuuhenkilonArvioDTO> {
        return koejaksonVastuuhenkilonArvioRepository.findByOpintooikeusId(opintooikeusId)
            .map(koejaksonVastuuhenkilonArvioMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOneByIdAndVastuuhenkiloUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonVastuuhenkilonArvioDTO> {
        return koejaksonVastuuhenkilonArvioRepository.findOneByIdAndVastuuhenkiloUserId(id, userId)
            .map(koejaksonVastuuhenkilonArvioMapper::toDto)
    }

    override fun delete(id: Long) {
        koejaksonVastuuhenkilonArvioRepository.deleteById(id)
    }
}
