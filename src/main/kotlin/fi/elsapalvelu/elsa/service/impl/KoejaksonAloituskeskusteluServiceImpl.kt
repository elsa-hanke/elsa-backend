package fi.elsapalvelu.elsa.service.impl

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
        aloituskeskustelu.muokkauspaiva = LocalDate.now(ZoneId.systemDefault())
        aloituskeskustelu.erikoistuvaLaakari = kirjautunutErikoistuvaLaakari
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

        if (kirjautunutErikoistuvaLaakari != null && kirjautunutErikoistuvaLaakari == aloituskeskustelu.erikoistuvaLaakari) {
            aloituskeskustelu.erikoistuvanNimi = updatedAloituskeskustelu.erikoistuvanNimi
            aloituskeskustelu.erikoistuvanErikoisala =
                updatedAloituskeskustelu.erikoistuvanErikoisala
            aloituskeskustelu.erikoistuvanOpiskelijatunnus =
                updatedAloituskeskustelu.erikoistuvanOpiskelijatunnus
            aloituskeskustelu.erikoistuvanYliopisto = updatedAloituskeskustelu.erikoistuvanYliopisto
            aloituskeskustelu.erikoistuvanSahkoposti =
                updatedAloituskeskustelu.erikoistuvanSahkoposti
            aloituskeskustelu.koejaksonSuorituspaikka =
                updatedAloituskeskustelu.koejaksonSuorituspaikka
            aloituskeskustelu.koejaksonToinenSuorituspaikka =
                updatedAloituskeskustelu.koejaksonToinenSuorituspaikka
            aloituskeskustelu.koejaksonAlkamispaiva = updatedAloituskeskustelu.koejaksonAlkamispaiva
            aloituskeskustelu.koejaksonPaattymispaiva =
                updatedAloituskeskustelu.koejaksonPaattymispaiva
            aloituskeskustelu.suoritettuKokoaikatyossa =
                updatedAloituskeskustelu.suoritettuKokoaikatyossa
            aloituskeskustelu.tyotunnitViikossa = updatedAloituskeskustelu.tyotunnitViikossa
            aloituskeskustelu.lahikouluttaja = updatedAloituskeskustelu.lahikouluttaja
            aloituskeskustelu.lahikouluttajanNimi = updatedAloituskeskustelu.lahikouluttajanNimi
            aloituskeskustelu.lahiesimies = updatedAloituskeskustelu.lahiesimies
            aloituskeskustelu.lahiesimiehenNimi = updatedAloituskeskustelu.lahiesimiehenNimi
            aloituskeskustelu.koejaksonOsaamistavoitteet =
                updatedAloituskeskustelu.koejaksonOsaamistavoitteet
            aloituskeskustelu.lahetetty = updatedAloituskeskustelu.lahetetty
            aloituskeskustelu.muokkauspaiva = LocalDate.now(ZoneId.systemDefault())

            if (aloituskeskustelu.lahetetty) {
                aloituskeskustelu.korjausehdotus = null
            }
        }

        if (aloituskeskustelu.lahikouluttaja?.user?.id == userId && !aloituskeskustelu.lahiesimiesHyvaksynyt) {
            // Hyväksytty
            if (updatedAloituskeskustelu.korjausehdotus.isNullOrBlank()) {
                aloituskeskustelu.lahikouluttajaHyvaksynyt = true
                aloituskeskustelu.lahikouluttajanKuittausaika =
                    LocalDate.now(ZoneId.systemDefault())
            }
            // Palautettu korjattavaksi
            else {
                aloituskeskustelu.korjausehdotus = updatedAloituskeskustelu.korjausehdotus
                aloituskeskustelu.lahetetty = false
            }
        }

        if (aloituskeskustelu.lahiesimies?.user?.id == userId && aloituskeskustelu.lahikouluttajaHyvaksynyt) {
            // Hyväksytty
            if (updatedAloituskeskustelu.korjausehdotus.isNullOrBlank()) {
                aloituskeskustelu.lahiesimiesHyvaksynyt = true
                aloituskeskustelu.lahiesimiehenKuittausaika = LocalDate.now(ZoneId.systemDefault())
            }
            // Palautettu korjattavaksi
            else {
                aloituskeskustelu.korjausehdotus = updatedAloituskeskustelu.korjausehdotus
                aloituskeskustelu.lahetetty = false
                aloituskeskustelu.lahikouluttajaHyvaksynyt = false
                aloituskeskustelu.lahikouluttajanKuittausaika = null
            }
        }

        aloituskeskustelu = koejaksonAloituskeskusteluRepository.save(aloituskeskustelu)

        // Sähköposti kouluttajalle allekirjoitetusta aloituskeskustelusta
        if (aloituskeskustelu.erikoistuvaLaakari?.kayttaja?.user?.id == userId && aloituskeskustelu.lahetetty) {
            mailService.sendEmailFromTemplate(
                kayttajaRepository.findById(aloituskeskustelu.lahikouluttaja?.id!!).get().user!!,
                "aloituskeskusteluKouluttajalle.html",
                "email.aloituskeskustelukouluttajalle.title",
                properties = mapOf(Pair(MailProperty.ID, aloituskeskustelu.id!!.toString()))
            )
        } else if (aloituskeskustelu.lahikouluttaja?.user?.id == userId) {

            // Sähköposti esimiehelle kouluttajan hyväksymästä aloituskeskustelusta
            if (aloituskeskustelu.lahikouluttajaHyvaksynyt) {
                mailService.sendEmailFromTemplate(
                    kayttajaRepository.findById(aloituskeskustelu.lahiesimies?.id!!).get().user!!,
                    "aloituskeskusteluKouluttajalle.html",
                    "email.aloituskeskustelukouluttajalle.title",
                    properties = mapOf(Pair(MailProperty.ID, aloituskeskustelu.id!!.toString()))
                )
            }
            // Sähköposti erikoistuvalle korjattavasta aloituskeskustelusta
            else {
                mailService.sendEmailFromTemplate(
                    kayttajaRepository.findById(aloituskeskustelu?.erikoistuvaLaakari?.kayttaja?.id!!)
                        .get().user!!,
                    "aloituskeskusteluPalautettu.html",
                    "email.aloituskeskustelupalautettu.title",
                    properties = mapOf(Pair(MailProperty.ID, aloituskeskustelu.id!!.toString()))
                )
            }
        } else if (aloituskeskustelu.lahiesimies?.user?.id == userId) {

            // Sähköposti erikoistuvalle ja kouluttajalle esimiehen hyväksymästä aloituskeskustelusta
            if (aloituskeskustelu.lahikouluttajaHyvaksynyt) {
                val erikoistuvaLaakari =
                    kayttajaRepository.findById(aloituskeskustelu?.erikoistuvaLaakari?.kayttaja?.id!!)
                        .get().user!!
                mailService.sendEmailFromTemplate(
                    erikoistuvaLaakari,
                    "aloituskeskusteluHyvaksytty.html",
                    "email.aloituskeskusteluhyvaksytty.title",
                    properties = mapOf(Pair(MailProperty.ID, aloituskeskustelu.id!!.toString()))
                )
                mailService.sendEmailFromTemplate(
                    kayttajaRepository.findById(aloituskeskustelu.lahikouluttaja?.id!!)
                        .get().user!!,
                    "aloituskeskusteluHyvaksyttyKouluttaja.html",
                    "email.aloituskeskusteluhyvaksytty.title",
                    properties = mapOf(
                        Pair(MailProperty.ID, aloituskeskustelu.id!!.toString()),
                        Pair(MailProperty.NAME, erikoistuvaLaakari.getName())
                    )
                )
            }
            // Sähköposti erikoistuvalle ja kouluttajalle korjattavasta aloituskeskustelusta
            else {
                val erikoistuvaLaakari =
                    kayttajaRepository.findById(aloituskeskustelu?.erikoistuvaLaakari?.kayttaja?.id!!)
                        .get().user!!
                mailService.sendEmailFromTemplate(
                    erikoistuvaLaakari,
                    "aloituskeskusteluPalautettu.html",
                    "email.aloituskeskustelupalautettu.title",
                    properties = mapOf(Pair(MailProperty.ID, aloituskeskustelu.id!!.toString()))
                )
                mailService.sendEmailFromTemplate(
                    kayttajaRepository.findById(aloituskeskustelu.lahikouluttaja?.id!!)
                        .get().user!!,
                    "aloituskeskusteluPalautettuKouluttaja.html",
                    "email.aloituskeskustelupalautettu.title",
                    properties = mapOf(
                        Pair(MailProperty.NAME, erikoistuvaLaakari.getName()),
                        Pair(MailProperty.TEXT, aloituskeskustelu.korjausehdotus!!)
                    )
                )
            }
        }

        return koejaksonAloituskeskusteluMapper.toDto(aloituskeskustelu)
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

    override fun findByValiarviointiId(id: Long): Optional<KoejaksonAloituskeskusteluDTO> {
        return koejaksonAloituskeskusteluRepository.findByValiarviointiId(
            id
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
