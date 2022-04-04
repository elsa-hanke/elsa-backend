package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.KoejaksonAloituskeskustelu
import fi.elsapalvelu.elsa.repository.*
import fi.elsapalvelu.elsa.service.KoejaksonAloituskeskusteluService
import fi.elsapalvelu.elsa.service.MailProperty
import fi.elsapalvelu.elsa.service.MailService
import fi.elsapalvelu.elsa.service.dto.KoejaksonAloituskeskusteluDTO
import fi.elsapalvelu.elsa.service.mapper.KoejaksonAloituskeskusteluMapper
import org.springframework.data.repository.findByIdOrNull
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
    private val opintooikeusRepository: OpintooikeusRepository,
    private val userRepository: UserRepository
) : KoejaksonAloituskeskusteluService {

    override fun create(
        koejaksonAloituskeskusteluDTO: KoejaksonAloituskeskusteluDTO,
        opintooikeusId: Long
    ): KoejaksonAloituskeskusteluDTO? {
        return opintooikeusRepository.findByIdOrNull(opintooikeusId)?.let {
            var aloituskeskustelu =
                koejaksonAloituskeskusteluMapper.toEntity(koejaksonAloituskeskusteluDTO)
            aloituskeskustelu.opintooikeus = it
            if (koejaksonAloituskeskusteluDTO.lahetetty == true) aloituskeskustelu.erikoistuvanKuittausaika =
                LocalDate.now()
            aloituskeskustelu = koejaksonAloituskeskusteluRepository.save(aloituskeskustelu)

            val user = it.erikoistuvaLaakari?.kayttaja?.user
            user?.email = koejaksonAloituskeskusteluDTO.erikoistuvanSahkoposti
            userRepository.save(user)

            if (aloituskeskustelu.lahetetty) {
                // Sähköposti kouluttajalle allekirjoitetusta aloituskeskustelusta
                mailService.sendEmailFromTemplate(
                    kayttajaRepository.findById(aloituskeskustelu.lahikouluttaja?.id!!)
                        .get().user!!,
                    "aloituskeskusteluKouluttajalle.html",
                    "email.aloituskeskustelukouluttajalle.title",
                    properties = mapOf(Pair(MailProperty.ID, aloituskeskustelu.id!!.toString()))
                )
            }

            koejaksonAloituskeskusteluMapper.toDto(aloituskeskustelu)
        }
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
            && kirjautunutErikoistuvaLaakari == aloituskeskustelu.opintooikeus?.erikoistuvaLaakari
        ) {
            aloituskeskustelu = handleErikoistuva(aloituskeskustelu, updatedAloituskeskustelu)

            val user = aloituskeskustelu.opintooikeus?.erikoistuvaLaakari?.kayttaja?.user
            user?.email = koejaksonAloituskeskusteluDTO.erikoistuvanSahkoposti
            userRepository.save(user)
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
        aloituskeskustelu.koejaksonSuorituspaikka = updated.koejaksonSuorituspaikka
        aloituskeskustelu.koejaksonToinenSuorituspaikka = updated.koejaksonToinenSuorituspaikka
        aloituskeskustelu.koejaksonAlkamispaiva = updated.koejaksonAlkamispaiva
        aloituskeskustelu.koejaksonPaattymispaiva = updated.koejaksonPaattymispaiva
        aloituskeskustelu.suoritettuKokoaikatyossa = updated.suoritettuKokoaikatyossa
        aloituskeskustelu.tyotunnitViikossa = updated.tyotunnitViikossa
        aloituskeskustelu.lahikouluttaja = updated.lahikouluttaja
        aloituskeskustelu.lahiesimies = updated.lahiesimies
        aloituskeskustelu.koejaksonOsaamistavoitteet = updated.koejaksonOsaamistavoitteet
        aloituskeskustelu.lahetetty = updated.lahetetty

        if (aloituskeskustelu.lahetetty) {
            aloituskeskustelu.korjausehdotus = null
            aloituskeskustelu.erikoistuvanKuittausaika = LocalDate.now()
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
            aloituskeskustelu.erikoistuvanKuittausaika = null
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
                kayttajaRepository.findById(result.opintooikeus?.erikoistuvaLaakari?.kayttaja?.id!!)
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
            aloituskeskustelu.erikoistuvanKuittausaika = null
        }

        val result = koejaksonAloituskeskusteluRepository.save(aloituskeskustelu)

        // Sähköposti erikoistuvalle ja kouluttajalle esimiehen hyväksymästä aloituskeskustelusta
        if (result.lahikouluttajaHyvaksynyt) {
            val erikoistuvaLaakari =
                kayttajaRepository.findById(result.opintooikeus?.erikoistuvaLaakari?.kayttaja?.id!!)
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
                kayttajaRepository.findById(result.opintooikeus?.erikoistuvaLaakari?.kayttaja?.id!!)
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
    override fun findByOpintooikeusId(opintooikeusId: Long): Optional<KoejaksonAloituskeskusteluDTO> {
        return koejaksonAloituskeskusteluRepository.findByOpintooikeusId(opintooikeusId)
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
    override fun findOneByIdHyvaksyttyAndBelongsToVastuuhenkilo(
        id: Long,
        vastuuhenkiloUserId: String
    ): Optional<KoejaksonAloituskeskusteluDTO> {
        return koejaksonAloituskeskusteluRepository.findOneByIdHyvaksyttyAndBelongsToVastuuhenkilo(
            id,
            vastuuhenkiloUserId
        )
            .map(koejaksonAloituskeskusteluMapper::toDto)
    }

    override fun delete(id: Long) {
        koejaksonAloituskeskusteluRepository.deleteById(id)
    }
}
