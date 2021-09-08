package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.KoejaksonLoppukeskustelu
import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.KayttajaRepository
import fi.elsapalvelu.elsa.repository.KoejaksonLoppukeskusteluRepository
import fi.elsapalvelu.elsa.service.KoejaksonLoppukeskusteluService
import fi.elsapalvelu.elsa.service.MailProperty
import fi.elsapalvelu.elsa.service.MailService
import fi.elsapalvelu.elsa.service.dto.KayttajaDTO
import fi.elsapalvelu.elsa.service.dto.KoejaksonLoppukeskusteluDTO
import fi.elsapalvelu.elsa.service.mapper.KayttajaMapper
import fi.elsapalvelu.elsa.service.mapper.KoejaksonLoppukeskusteluMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import javax.persistence.EntityNotFoundException


@Service
@Transactional
class KoejaksonLoppukeskusteluServiceImpl(
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository,
    private val koejaksonLoppukeskusteluRepository: KoejaksonLoppukeskusteluRepository,
    private val koejaksonLoppukeskusteluMapper: KoejaksonLoppukeskusteluMapper,
    private val mailService: MailService,
    private val kayttajaRepository: KayttajaRepository,
    private val kayttajaMapper: KayttajaMapper
) : KoejaksonLoppukeskusteluService {

    override fun create(
        koejaksonLoppukeskusteluDTO: KoejaksonLoppukeskusteluDTO,
        kayttajaId: String
    ): KoejaksonLoppukeskusteluDTO {
        val kirjautunutErikoistuvaLaakari =
            erikoistuvaLaakariRepository.findOneByKayttajaId(kayttajaId)
        var loppukeskustelu =
            koejaksonLoppukeskusteluMapper.toEntity(koejaksonLoppukeskusteluDTO)
        loppukeskustelu.erikoistuvaLaakari = kirjautunutErikoistuvaLaakari
        loppukeskustelu = koejaksonLoppukeskusteluRepository.save(loppukeskustelu)

        // Sähköposti kouluttajalle
        mailService.sendEmailFromTemplate(
            kayttajaRepository.findById(loppukeskustelu.lahikouluttaja?.id!!).get(),
            "loppukeskusteluKouluttajalle.html",
            "email.loppukeskustelukouluttajalle.title",
            properties = mapOf(Pair(MailProperty.ID, loppukeskustelu.id!!.toString()))
        )

        return koejaksonLoppukeskusteluMapper.toDto(loppukeskustelu)
    }

    override fun update(
        koejaksonLoppukeskusteluDTO: KoejaksonLoppukeskusteluDTO,
        kayttajaId: String
    ): KoejaksonLoppukeskusteluDTO {

        var loppukeskustelu =
            koejaksonLoppukeskusteluRepository.findById(koejaksonLoppukeskusteluDTO.id!!)
                .orElseThrow { EntityNotFoundException("Loppukeskustelua ei löydy") }

        val updatedLoppukeskustelu =
            koejaksonLoppukeskusteluMapper.toEntity(koejaksonLoppukeskusteluDTO)

        if (loppukeskustelu.erikoistuvaLaakari?.kayttaja?.id == kayttajaId && loppukeskustelu.lahiesimiesHyvaksynyt) {
            loppukeskustelu = handleErikoistuva(loppukeskustelu)
        }

        if (loppukeskustelu.lahikouluttaja?.id == kayttajaId && !loppukeskustelu.lahiesimiesHyvaksynyt) {
            loppukeskustelu = handleKouluttaja(loppukeskustelu, updatedLoppukeskustelu)
        }

        if (loppukeskustelu.lahiesimies?.id == kayttajaId && loppukeskustelu.lahikouluttajaHyvaksynyt) {
            loppukeskustelu = handleEsimies(loppukeskustelu, updatedLoppukeskustelu)
        }

        return koejaksonLoppukeskusteluMapper.toDto(loppukeskustelu)
    }

    private fun handleErikoistuva(loppukeskustelu: KoejaksonLoppukeskustelu): KoejaksonLoppukeskustelu {
        loppukeskustelu.erikoistuvaAllekirjoittanut = true
        loppukeskustelu.erikoistuvanAllekirjoitusaika = LocalDate.now()

        val result = koejaksonLoppukeskusteluRepository.save(loppukeskustelu)

        // Sähköposti kouluttajalle ja esimiehelle allekirjoitetusta loppukeskustelusta
        val erikoistuvaLaakariKayttaja =
            kayttajaRepository.findById(loppukeskustelu.erikoistuvaLaakari?.kayttaja?.id!!)
                .get()
        mailService.sendEmailFromTemplate(
            kayttajaRepository.findById(loppukeskustelu.lahikouluttaja?.id!!).get(),
            "loppukeskusteluHyvaksytty.html",
            "email.loppukeskusteluhyvaksytty.title",
            properties = mapOf(
                Pair(MailProperty.ID, loppukeskustelu.id!!.toString()),
                Pair(MailProperty.NAME, "${erikoistuvaLaakariKayttaja.etunimi} ${erikoistuvaLaakariKayttaja.sukunimi}")
            )
        )
        mailService.sendEmailFromTemplate(
            kayttajaRepository.findById(loppukeskustelu.lahiesimies?.id!!).get(),
            "loppukeskusteluHyvaksytty.html",
            "email.loppukeskusteluhyvaksytty.title",
            properties = mapOf(
                Pair(MailProperty.ID, loppukeskustelu.id!!.toString()),
                Pair(MailProperty.NAME, "${erikoistuvaLaakariKayttaja.etunimi} ${erikoistuvaLaakariKayttaja.sukunimi}")
            )
        )

        return result
    }

    private fun handleKouluttaja(
        loppukeskustelu: KoejaksonLoppukeskustelu,
        updated: KoejaksonLoppukeskustelu
    ): KoejaksonLoppukeskustelu {
        loppukeskustelu.esitetaanKoejaksonHyvaksymista = updated.esitetaanKoejaksonHyvaksymista
        loppukeskustelu.jatkotoimenpiteet = updated.jatkotoimenpiteet
        loppukeskustelu.lahikouluttajaHyvaksynyt = true
        loppukeskustelu.lahikouluttajanKuittausaika = LocalDate.now(ZoneId.systemDefault())
        loppukeskustelu.korjausehdotus = null

        val result = koejaksonLoppukeskusteluRepository.save(loppukeskustelu)

        // Sähköposti esimiehelle kouluttajan hyväksymästä loppukeskustelusta
        mailService.sendEmailFromTemplate(
            kayttajaRepository.findById(loppukeskustelu.lahiesimies?.id!!).get(),
            "loppukeskusteluKuitattava.html",
            "email.loppukeskustelukuitattava.title",
            properties = mapOf(Pair(MailProperty.ID, loppukeskustelu.id!!.toString()))
        )

        return result
    }

    private fun handleEsimies(
        loppukeskustelu: KoejaksonLoppukeskustelu,
        updated: KoejaksonLoppukeskustelu
    ): KoejaksonLoppukeskustelu {
        // Hyväksytty
        if (updated.korjausehdotus.isNullOrBlank()) {
            loppukeskustelu.lahiesimiesHyvaksynyt = true
            loppukeskustelu.lahiesimiehenKuittausaika =
                LocalDate.now(ZoneId.systemDefault())
        }
        // Palautettu korjattavaksi
        else {
            loppukeskustelu.korjausehdotus = updated.korjausehdotus
            loppukeskustelu.lahikouluttajaHyvaksynyt = false
            loppukeskustelu.lahikouluttajanKuittausaika = null
        }

        val result = koejaksonLoppukeskusteluRepository.save(loppukeskustelu)

        // Sähköposti erikoistuvalle esimiehen hyväksymästä loppukeskustelusta
        if (loppukeskustelu.lahikouluttajaHyvaksynyt) {
            mailService.sendEmailFromTemplate(
                kayttajaRepository.findById(loppukeskustelu.erikoistuvaLaakari?.kayttaja?.id!!)
                    .get(),
                "loppukeskusteluKuitattava.html",
                "email.loppukeskustelukuitattava.title",
                properties = mapOf(Pair(MailProperty.ID, loppukeskustelu.id!!.toString()))
            )
        }
        // Sähköposti kouluttajalle korjattavasta loppukeskustelusta
        else {
            mailService.sendEmailFromTemplate(
                kayttajaRepository.findById(loppukeskustelu.lahikouluttaja?.id!!)
                    .get(),
                "loppukeskusteluPalautettu.html",
                "email.loppukeskustelupalautettu.title",
                properties = mapOf(
                    Pair(
                        MailProperty.ID,
                        loppukeskustelu.id!!.toString()
                    )
                )
            )
        }

        return result
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<KoejaksonLoppukeskusteluDTO> {
        return koejaksonLoppukeskusteluRepository.findById(id)
            .map(koejaksonLoppukeskusteluMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findByErikoistuvaLaakariKayttajaId(kayttajaId: String): Optional<KoejaksonLoppukeskusteluDTO> {
        return koejaksonLoppukeskusteluRepository.findByErikoistuvaLaakariKayttajaId(kayttajaId)
            .map(koejaksonLoppukeskusteluMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOneByIdAndLahikouluttajaId(
        id: Long,
        kayttajaId: String
    ): Optional<KoejaksonLoppukeskusteluDTO> {
        return koejaksonLoppukeskusteluRepository.findOneByIdAndLahikouluttajaId(id, kayttajaId)
            .map(koejaksonLoppukeskusteluMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOneByIdAndLahiesimiesId(
        id: Long,
        kayttajaId: String
    ): Optional<KoejaksonLoppukeskusteluDTO> {
        return koejaksonLoppukeskusteluRepository.findOneByIdAndLahiesimiesId(
            id,
            kayttajaId
        ).map(koejaksonLoppukeskusteluMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findAllByKouluttajaId(kayttajaId: String): Map<KayttajaDTO, KoejaksonLoppukeskusteluDTO> {
        val loppukeskustelut =
            koejaksonLoppukeskusteluRepository.findAllByLahikouluttajaIdOrLahiesimiesId(
                kayttajaId
            )
        return loppukeskustelut.associate {
            kayttajaMapper.toDto(it.erikoistuvaLaakari?.kayttaja!!) to koejaksonLoppukeskusteluMapper.toDto(
                it
            )
        }
    }

    override fun delete(id: Long) {
        koejaksonLoppukeskusteluRepository.deleteById(id)
    }
}
