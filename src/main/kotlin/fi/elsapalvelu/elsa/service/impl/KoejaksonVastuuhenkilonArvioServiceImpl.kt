package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.KayttajaRepository
import fi.elsapalvelu.elsa.repository.KoejaksonVastuuhenkilonArvioRepository
import fi.elsapalvelu.elsa.service.KoejaksonVastuuhenkilonArvioService
import fi.elsapalvelu.elsa.service.MailProperty
import fi.elsapalvelu.elsa.service.MailService
import fi.elsapalvelu.elsa.service.dto.KayttajaDTO
import fi.elsapalvelu.elsa.service.dto.KoejaksonVastuuhenkilonArvioDTO
import fi.elsapalvelu.elsa.service.mapper.KayttajaMapper
import fi.elsapalvelu.elsa.service.mapper.KoejaksonVastuuhenkilonArvioMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import javax.persistence.EntityNotFoundException


@Service
@Transactional
class KoejaksonVastuuhenkilonArvioServiceImpl(
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository,
    private val koejaksonVastuuhenkilonArvioRepository: KoejaksonVastuuhenkilonArvioRepository,
    private val koejaksonVastuuhenkilonArvioMapper: KoejaksonVastuuhenkilonArvioMapper,
    private val mailService: MailService,
    private val kayttajaRepository: KayttajaRepository,
    private val kayttajaMapper: KayttajaMapper
) : KoejaksonVastuuhenkilonArvioService {

    override fun create(
        koejaksonVastuuhenkilonArvioDTO: KoejaksonVastuuhenkilonArvioDTO,
        kayttajaId: String
    ): KoejaksonVastuuhenkilonArvioDTO {
        val kirjautunutErikoistuvaLaakari =
            erikoistuvaLaakariRepository.findOneByKayttajaId(kayttajaId)
        var vastuuhenkilonArvio =
            koejaksonVastuuhenkilonArvioMapper.toEntity(koejaksonVastuuhenkilonArvioDTO)
        vastuuhenkilonArvio.erikoistuvaLaakari = kirjautunutErikoistuvaLaakari
        vastuuhenkilonArvio = koejaksonVastuuhenkilonArvioRepository.save(vastuuhenkilonArvio)

        // Sähköposti vastuuhenkilölle
        mailService.sendEmailFromTemplate(
            kayttajaRepository.findById(vastuuhenkilonArvio.vastuuhenkilo?.id!!).get(),
            "vastuuhenkilonArvioKuitattava.html",
            "email.vastuuhenkilonarviokuitattava.title",
            properties = mapOf(Pair(MailProperty.ID, vastuuhenkilonArvio.id!!.toString()))
        )

        return koejaksonVastuuhenkilonArvioMapper.toDto(vastuuhenkilonArvio)
    }

    override fun update(
        koejaksonVastuuhenkilonArvioDTO: KoejaksonVastuuhenkilonArvioDTO,
        kayttajaId: String
    ): KoejaksonVastuuhenkilonArvioDTO {

        var vastuuhenkilonArvio =
            koejaksonVastuuhenkilonArvioRepository.findById(koejaksonVastuuhenkilonArvioDTO.id!!)
                .orElseThrow { EntityNotFoundException("Vastuuhenkilön arviota ei löydy") }

        val updatedVastuuhenkilonArvio =
            koejaksonVastuuhenkilonArvioMapper.toEntity(koejaksonVastuuhenkilonArvioDTO)

        if (vastuuhenkilonArvio.erikoistuvaLaakari?.kayttaja?.id == kayttajaId
            && vastuuhenkilonArvio.vastuuhenkilonKuittausaika != null
        ) {
            vastuuhenkilonArvio.erikoistuvaAllekirjoittanut = true
            vastuuhenkilonArvio.erikoistuvanAllekirjoitusaika = LocalDate.now()
        }

        if (vastuuhenkilonArvio.vastuuhenkilo?.id == kayttajaId) {
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
        if (vastuuhenkilonArvio.erikoistuvaLaakari?.kayttaja?.id == kayttajaId
            && vastuuhenkilonArvio.erikoistuvaAllekirjoittanut
        ) {
            val erikoistuvaLaakariKayttaja =
                kayttajaRepository.findById(vastuuhenkilonArvio?.erikoistuvaLaakari?.kayttaja?.id!!)
                    .get()
            mailService.sendEmailFromTemplate(
                kayttajaRepository.findById(vastuuhenkilonArvio.vastuuhenkilo?.id!!)
                    .get(),
                "vastuuhenkilonArvioHyvaksytty.html",
                "email.vastuuhenkilonarviohyvaksytty.title",
                properties = mapOf(
                    Pair(MailProperty.ID, vastuuhenkilonArvio.id!!.toString()),
                    Pair(
                        MailProperty.NAME,
                        "${erikoistuvaLaakariKayttaja.etunimi} ${erikoistuvaLaakariKayttaja.sukunimi}"
                    )
                )
            )
        } else if (vastuuhenkilonArvio.vastuuhenkilo?.id == kayttajaId) {

            // Sähköposti erikoistuvalle vastuuhenkilon kuittaamasta arviosta
            if (vastuuhenkilonArvio.vastuuhenkilonKuittausaika != null) {
                mailService.sendEmailFromTemplate(
                    kayttajaRepository.findById(vastuuhenkilonArvio?.erikoistuvaLaakari?.kayttaja?.id!!)
                        .get(),
                    "vastuuhenkilonArvioKuitattava.html",
                    "email.vastuuhenkilonarviokuitattava.title",
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
    override fun findByErikoistuvaLaakariKayttajaId(kayttajaId: String): Optional<KoejaksonVastuuhenkilonArvioDTO> {
        return koejaksonVastuuhenkilonArvioRepository.findByErikoistuvaLaakariKayttajaId(kayttajaId)
            .map(koejaksonVastuuhenkilonArvioMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOneByIdAndVastuuhenkiloId(
        id: Long,
        kayttajaId: String
    ): Optional<KoejaksonVastuuhenkilonArvioDTO> {
        return koejaksonVastuuhenkilonArvioRepository.findOneByIdAndVastuuhenkiloId(id, kayttajaId)
            .map(koejaksonVastuuhenkilonArvioMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findAllByVastuuhenkiloId(kayttajaId: String): Map<KayttajaDTO, KoejaksonVastuuhenkilonArvioDTO> {
        val arviot =
            koejaksonVastuuhenkilonArvioRepository.findAllByVastuuhenkiloId(kayttajaId)
        return arviot.associate {
            kayttajaMapper.toDto(it.erikoistuvaLaakari?.kayttaja!!) to koejaksonVastuuhenkilonArvioMapper.toDto(
                it
            )
        }
    }

    override fun delete(id: Long) {
        koejaksonVastuuhenkilonArvioRepository.deleteById(id)
    }
}
