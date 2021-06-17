package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.KoejaksonAloituskeskustelu
import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.KayttajaRepository
import fi.elsapalvelu.elsa.repository.KoejaksonAloituskeskusteluRepository
import fi.elsapalvelu.elsa.service.KoejaksonAloituskeskusteluService
import fi.elsapalvelu.elsa.service.MailProperty
import fi.elsapalvelu.elsa.service.MailService
import fi.elsapalvelu.elsa.service.dto.KayttajaDTO
import fi.elsapalvelu.elsa.service.dto.KoejaksonAloituskeskusteluDTO
import fi.elsapalvelu.elsa.service.mapper.KayttajaMapper
import fi.elsapalvelu.elsa.service.mapper.KoejaksonAloituskeskusteluMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import javax.persistence.EntityNotFoundException


@Service
@Transactional
class KoejaksonAloituskeskusteluServiceImpl(
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository,
    private val koejaksonAloituskeskusteluRepository: KoejaksonAloituskeskusteluRepository,
    private val koejaksonAloituskeskusteluMapper: KoejaksonAloituskeskusteluMapper,
    private val mailService: MailService,
    private val kayttajaRepository: KayttajaRepository,
    private val kayttajaMapper: KayttajaMapper
) : KoejaksonAloituskeskusteluService {

    override fun create(
        koejaksonAloituskeskusteluDTO: KoejaksonAloituskeskusteluDTO,
        userId: String
    ): KoejaksonAloituskeskusteluDTO {
        val kirjautunutErikoistuvaLaakari =
            erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)
        var aloituskeskustelu =
            koejaksonAloituskeskusteluMapper.toEntity(koejaksonAloituskeskusteluDTO)
        aloituskeskustelu.erikoistuvaLaakari = kirjautunutErikoistuvaLaakari
        if (koejaksonAloituskeskusteluDTO.lahetetty == true) aloituskeskustelu.erikoistuvanAllekirjoitusaika =
            LocalDate.now()
        aloituskeskustelu = koejaksonAloituskeskusteluRepository.save(aloituskeskustelu)

        if (aloituskeskustelu.lahetetty) {
            // Sähköposti kouluttajalle allekirjoitetusta aloituskeskustelusta
            mailService.sendEmailFromTemplate(
                kayttajaRepository.findById(aloituskeskustelu.lahikouluttaja?.id!!).get().user!!,
                "aloituskeskusteluKouluttajalle.html",
                "email.aloituskeskustelukouluttajalle.title",
                properties = mapOf(Pair(MailProperty.ID, aloituskeskustelu.id!!.toString()))
            )
        }

        return koejaksonAloituskeskusteluMapper.toDto(aloituskeskustelu)
    }

    override fun update(
        koejaksonAloituskeskusteluDTO: KoejaksonAloituskeskusteluDTO,
        userId: String
    ): KoejaksonAloituskeskusteluDTO {

        var aloituskeskustelu =
            koejaksonAloituskeskusteluRepository.findById(koejaksonAloituskeskusteluDTO.id!!)
                .orElseThrow { EntityNotFoundException("Aloituskeskustelua ei löydy") }

        val kirjautunutErikoistuvaLaakari =
            erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)

        val updatedAloituskeskustelu =
            koejaksonAloituskeskusteluMapper.toEntity(koejaksonAloituskeskusteluDTO)

        if (kirjautunutErikoistuvaLaakari != null
            && kirjautunutErikoistuvaLaakari == aloituskeskustelu.erikoistuvaLaakari
        ) {
            aloituskeskustelu = handleErikoistuva(aloituskeskustelu, updatedAloituskeskustelu)
        }

        if (aloituskeskustelu.lahikouluttaja?.user?.id == userId && !aloituskeskustelu.lahiesimiesHyvaksynyt) {
            aloituskeskustelu = handleKouluttaja(aloituskeskustelu, updatedAloituskeskustelu)
        }

        if (aloituskeskustelu.lahiesimies?.user?.id == userId && aloituskeskustelu.lahikouluttajaHyvaksynyt) {
            aloituskeskustelu = handleEsimies(aloituskeskustelu, updatedAloituskeskustelu)
        }

        return koejaksonAloituskeskusteluMapper.toDto(aloituskeskustelu)
    }

    private fun handleErikoistuva(
        aloituskeskustelu: KoejaksonAloituskeskustelu,
        updated: KoejaksonAloituskeskustelu
    ): KoejaksonAloituskeskustelu {
        aloituskeskustelu.erikoistuvanNimi = updated.erikoistuvanNimi
        aloituskeskustelu.erikoistuvanErikoisala = updated.erikoistuvanErikoisala
        aloituskeskustelu.erikoistuvanOpiskelijatunnus = updated.erikoistuvanOpiskelijatunnus
        aloituskeskustelu.erikoistuvanYliopisto = updated.erikoistuvanYliopisto
        aloituskeskustelu.erikoistuvanSahkoposti = updated.erikoistuvanSahkoposti
        aloituskeskustelu.koejaksonSuorituspaikka = updated.koejaksonSuorituspaikka
        aloituskeskustelu.koejaksonToinenSuorituspaikka = updated.koejaksonToinenSuorituspaikka
        aloituskeskustelu.koejaksonAlkamispaiva = updated.koejaksonAlkamispaiva
        aloituskeskustelu.koejaksonPaattymispaiva = updated.koejaksonPaattymispaiva
        aloituskeskustelu.suoritettuKokoaikatyossa = updated.suoritettuKokoaikatyossa
        aloituskeskustelu.tyotunnitViikossa = updated.tyotunnitViikossa
        aloituskeskustelu.lahikouluttaja = updated.lahikouluttaja
        aloituskeskustelu.lahikouluttajanNimi = updated.lahikouluttajanNimi
        aloituskeskustelu.lahiesimies = updated.lahiesimies
        aloituskeskustelu.lahiesimiehenNimi = updated.lahiesimiehenNimi
        aloituskeskustelu.koejaksonOsaamistavoitteet = updated.koejaksonOsaamistavoitteet
        aloituskeskustelu.lahetetty = updated.lahetetty

        if (aloituskeskustelu.lahetetty) {
            aloituskeskustelu.korjausehdotus = null
            aloituskeskustelu.erikoistuvanAllekirjoitusaika = LocalDate.now()
        }

        val result = koejaksonAloituskeskusteluRepository.save(aloituskeskustelu)

        // Sähköposti kouluttajalle allekirjoitetusta aloituskeskustelusta
        if (result.lahetetty) {
            mailService.sendEmailFromTemplate(
                kayttajaRepository.findById(result.lahikouluttaja?.id!!).get().user!!,
                "aloituskeskusteluKouluttajalle.html",
                "email.aloituskeskustelukouluttajalle.title",
                properties = mapOf(Pair(MailProperty.ID, result.id!!.toString()))
            )
        }

        return result
    }

    private fun handleKouluttaja(
        aloituskeskustelu: KoejaksonAloituskeskustelu,
        updated: KoejaksonAloituskeskustelu
    ): KoejaksonAloituskeskustelu {
        // Hyväksytty
        if (updated.korjausehdotus.isNullOrBlank()) {
            aloituskeskustelu.lahikouluttajaHyvaksynyt = true
            aloituskeskustelu.lahikouluttajanKuittausaika = LocalDate.now(ZoneId.systemDefault())
        }
        // Palautettu korjattavaksi
        else {
            aloituskeskustelu.korjausehdotus = updated.korjausehdotus
            aloituskeskustelu.lahetetty = false
            aloituskeskustelu.erikoistuvanAllekirjoitusaika = null
        }

        val result = koejaksonAloituskeskusteluRepository.save(aloituskeskustelu)

        // Sähköposti esimiehelle kouluttajan hyväksymästä aloituskeskustelusta
        if (result.lahikouluttajaHyvaksynyt) {
            mailService.sendEmailFromTemplate(
                kayttajaRepository.findById(result.lahiesimies?.id!!).get().user!!,
                "aloituskeskusteluKouluttajalle.html",
                "email.aloituskeskustelukouluttajalle.title",
                properties = mapOf(Pair(MailProperty.ID, result.id!!.toString()))
            )
        }
        // Sähköposti erikoistuvalle korjattavasta aloituskeskustelusta
        else {
            mailService.sendEmailFromTemplate(
                kayttajaRepository.findById(result.erikoistuvaLaakari?.kayttaja?.id!!)
                    .get().user!!,
                "aloituskeskusteluPalautettu.html",
                "email.aloituskeskustelupalautettu.title",
                properties = mapOf(Pair(MailProperty.ID, result.id!!.toString()))
            )
        }

        return result
    }

    private fun handleEsimies(
        aloituskeskustelu: KoejaksonAloituskeskustelu,
        updated: KoejaksonAloituskeskustelu
    ): KoejaksonAloituskeskustelu {
        // Hyväksytty
        if (updated.korjausehdotus.isNullOrBlank()) {
            aloituskeskustelu.lahiesimiesHyvaksynyt = true
            aloituskeskustelu.lahiesimiehenKuittausaika = LocalDate.now(ZoneId.systemDefault())
        }
        // Palautettu korjattavaksi
        else {
            aloituskeskustelu.korjausehdotus = updated.korjausehdotus
            aloituskeskustelu.lahetetty = false
            aloituskeskustelu.lahikouluttajaHyvaksynyt = false
            aloituskeskustelu.lahikouluttajanKuittausaika = null
            aloituskeskustelu.erikoistuvanAllekirjoitusaika = null
        }

        val result = koejaksonAloituskeskusteluRepository.save(aloituskeskustelu)

        // Sähköposti erikoistuvalle ja kouluttajalle esimiehen hyväksymästä aloituskeskustelusta
        if (result.lahikouluttajaHyvaksynyt) {
            val erikoistuvaLaakari =
                kayttajaRepository.findById(result.erikoistuvaLaakari?.kayttaja?.id!!)
                    .get().user!!
            mailService.sendEmailFromTemplate(
                erikoistuvaLaakari,
                "aloituskeskusteluHyvaksytty.html",
                "email.aloituskeskusteluhyvaksytty.title",
                properties = mapOf(Pair(MailProperty.ID, result.id!!.toString()))
            )
            mailService.sendEmailFromTemplate(
                kayttajaRepository.findById(result.lahikouluttaja?.id!!)
                    .get().user!!,
                "aloituskeskusteluHyvaksyttyKouluttaja.html",
                "email.aloituskeskusteluhyvaksytty.title",
                properties = mapOf(
                    Pair(MailProperty.ID, result.id!!.toString()),
                    Pair(MailProperty.NAME, erikoistuvaLaakari.getName())
                )
            )
        }
        // Sähköposti erikoistuvalle ja kouluttajalle korjattavasta aloituskeskustelusta
        else {
            val erikoistuvaLaakari =
                kayttajaRepository.findById(result.erikoistuvaLaakari?.kayttaja?.id!!)
                    .get().user!!
            mailService.sendEmailFromTemplate(
                erikoistuvaLaakari,
                "aloituskeskusteluPalautettu.html",
                "email.aloituskeskustelupalautettu.title",
                properties = mapOf(Pair(MailProperty.ID, result.id!!.toString()))
            )
            mailService.sendEmailFromTemplate(
                kayttajaRepository.findById(result.lahikouluttaja?.id!!)
                    .get().user!!,
                "aloituskeskusteluPalautettuKouluttaja.html",
                "email.aloituskeskustelupalautettu.title",
                properties = mapOf(
                    Pair(MailProperty.NAME, erikoistuvaLaakari.getName()),
                    Pair(MailProperty.TEXT, result.korjausehdotus!!)
                )
            )
        }

        return result
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<KoejaksonAloituskeskusteluDTO> {
        return koejaksonAloituskeskusteluRepository.findById(id)
            .map(koejaksonAloituskeskusteluMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findByErikoistuvaLaakariKayttajaUserId(userId: String): Optional<KoejaksonAloituskeskusteluDTO> {
        return koejaksonAloituskeskusteluRepository.findByErikoistuvaLaakariKayttajaUserId(userId)
            .map(koejaksonAloituskeskusteluMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOneByIdAndLahikouluttajaUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonAloituskeskusteluDTO> {
        return koejaksonAloituskeskusteluRepository.findOneByIdAndLahikouluttajaUserId(
            id,
            userId
        ).map(koejaksonAloituskeskusteluMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOneByIdAndLahiesimiesUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonAloituskeskusteluDTO> {
        return koejaksonAloituskeskusteluRepository.findOneByIdAndLahiesimiesUserId(
            id,
            userId
        ).map(koejaksonAloituskeskusteluMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findAllByKouluttajaUserId(userId: String): Map<KayttajaDTO, KoejaksonAloituskeskusteluDTO> {
        val aloituskeskustelut =
            koejaksonAloituskeskusteluRepository.findAllByLahikouluttajaUserIdOrLahiesimiesUserId(
                userId, userId
            )
        return aloituskeskustelut.associate {
            kayttajaMapper.toDto(it.erikoistuvaLaakari?.kayttaja!!) to koejaksonAloituskeskusteluMapper.toDto(
                it
            )
        }
    }

    override fun delete(id: Long) {
        koejaksonAloituskeskusteluRepository.deleteById(id)
    }
}
