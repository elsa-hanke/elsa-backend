package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.KayttajaRepository
import fi.elsapalvelu.elsa.repository.KoejaksonKehittamistoimenpiteetRepository
import fi.elsapalvelu.elsa.service.KoejaksonKehittamistoimenpiteetService
import fi.elsapalvelu.elsa.service.MailProperty
import fi.elsapalvelu.elsa.service.MailService
import fi.elsapalvelu.elsa.service.dto.KayttajaDTO
import fi.elsapalvelu.elsa.service.dto.KoejaksonKehittamistoimenpiteetDTO
import fi.elsapalvelu.elsa.service.mapper.KayttajaMapper
import fi.elsapalvelu.elsa.service.mapper.KoejaksonKehittamistoimenpiteetMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import javax.persistence.EntityNotFoundException


@Service
@Transactional
class KoejaksonKehittamistoimenpiteetServiceImpl(
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository,
    private val koejaksonKehittamistoimenpiteetRepository: KoejaksonKehittamistoimenpiteetRepository,
    private val koejaksonKehittamistoimenpiteetMapper: KoejaksonKehittamistoimenpiteetMapper,
    private val mailService: MailService,
    private val kayttajaRepository: KayttajaRepository,
    private val kayttajaMapper: KayttajaMapper
) : KoejaksonKehittamistoimenpiteetService {

    override fun create(
        koejaksonKehittamistoimenpiteetDTO: KoejaksonKehittamistoimenpiteetDTO,
        userId: String
    ): KoejaksonKehittamistoimenpiteetDTO {
        val kirjautunutErikoistuvaLaakari =
            erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)
        var kehittamistoimenpiteet =
            koejaksonKehittamistoimenpiteetMapper.toEntity(koejaksonKehittamistoimenpiteetDTO)
        kehittamistoimenpiteet.muokkauspaiva = LocalDate.now(ZoneId.systemDefault())
        kehittamistoimenpiteet.erikoistuvaLaakari = kirjautunutErikoistuvaLaakari
        kehittamistoimenpiteet =
            koejaksonKehittamistoimenpiteetRepository.save(kehittamistoimenpiteet)

        // Sähköposti kouluttajalle
        mailService.sendEmailFromTemplate(
            kayttajaRepository.findById(kehittamistoimenpiteet.lahikouluttaja?.id!!).get().user!!,
            "kehittamistoimenpiteetKouluttajalle.html",
            "email.kehittamistoimenpiteetkouluttajalle.title",
            properties = mapOf(Pair(MailProperty.ID, kehittamistoimenpiteet.id!!.toString()))
        )

        return koejaksonKehittamistoimenpiteetMapper.toDto(kehittamistoimenpiteet)
    }

    override fun update(
        koejaksonKehittamistoimenpiteetDTO: KoejaksonKehittamistoimenpiteetDTO,
        userId: String
    ): KoejaksonKehittamistoimenpiteetDTO {

        var kehittamistoimenpiteet =
            koejaksonKehittamistoimenpiteetRepository.findById(koejaksonKehittamistoimenpiteetDTO.id!!)
                .orElseThrow { EntityNotFoundException("Kehittämistoimenpiteitä ei löydy") }

        val updatedKehittamistoimenpiteet =
            koejaksonKehittamistoimenpiteetMapper.toEntity(koejaksonKehittamistoimenpiteetDTO)

        if (kehittamistoimenpiteet.erikoistuvaLaakari?.kayttaja?.user?.id == userId && kehittamistoimenpiteet.lahiesimiesHyvaksynyt) {
            kehittamistoimenpiteet.erikoistuvaAllekirjoittanut = true
            kehittamistoimenpiteet.muokkauspaiva = LocalDate.now(ZoneId.systemDefault())
        }

        if (kehittamistoimenpiteet.lahikouluttaja?.user?.id == userId && !kehittamistoimenpiteet.lahiesimiesHyvaksynyt) {
            kehittamistoimenpiteet.kehittamistoimenpiteetRiittavat =
                updatedKehittamistoimenpiteet.kehittamistoimenpiteetRiittavat
            kehittamistoimenpiteet.lahikouluttajaHyvaksynyt = true
            kehittamistoimenpiteet.lahikouluttajanKuittausaika =
                LocalDate.now(ZoneId.systemDefault())
        }

        if (kehittamistoimenpiteet.lahiesimies?.user?.id == userId && kehittamistoimenpiteet.lahikouluttajaHyvaksynyt) {
            // Hyväksytty
            if (updatedKehittamistoimenpiteet.korjausehdotus.isNullOrBlank()) {
                kehittamistoimenpiteet.lahiesimiesHyvaksynyt = true
                kehittamistoimenpiteet.lahiesimiehenKuittausaika =
                    LocalDate.now(ZoneId.systemDefault())
            }
            // Palautettu korjattavaksi
            else {
                kehittamistoimenpiteet.korjausehdotus = updatedKehittamistoimenpiteet.korjausehdotus
                kehittamistoimenpiteet.lahikouluttajaHyvaksynyt = false
                kehittamistoimenpiteet.lahikouluttajanKuittausaika = null
            }
        }

        kehittamistoimenpiteet =
            koejaksonKehittamistoimenpiteetRepository.save(kehittamistoimenpiteet)

        // Sähköposti kouluttajalle ja esimiehelle allekirjoitetusta kehittämistoimenpteistä
        if (kehittamistoimenpiteet.erikoistuvaLaakari?.kayttaja?.user?.id == userId && kehittamistoimenpiteet.erikoistuvaAllekirjoittanut) {
            val erikoistuvaLaakari =
                kayttajaRepository.findById(kehittamistoimenpiteet?.erikoistuvaLaakari?.kayttaja?.id!!)
                    .get().user!!
            mailService.sendEmailFromTemplate(
                kayttajaRepository.findById(kehittamistoimenpiteet.lahikouluttaja?.id!!)
                    .get().user!!,
                "kehittamistoimenpiteetHyvaksytty.html",
                "email.kehittamistoimenpiteethyvaksytty.title",
                properties = mapOf(
                    Pair(MailProperty.ID, kehittamistoimenpiteet.id!!.toString()),
                    Pair(MailProperty.NAME, erikoistuvaLaakari.getName())
                )
            )
            mailService.sendEmailFromTemplate(
                kayttajaRepository.findById(kehittamistoimenpiteet.lahiesimies?.id!!).get().user!!,
                "kehittamistoimenpiteetHyvaksytty.html",
                "email.kehittamistoimenpiteethyvaksytty.title",
                properties = mapOf(
                    Pair(MailProperty.ID, kehittamistoimenpiteet.id!!.toString()),
                    Pair(MailProperty.NAME, erikoistuvaLaakari.getName())
                )
            )
        } else if (kehittamistoimenpiteet.lahikouluttaja?.user?.id == userId && kehittamistoimenpiteet.lahikouluttajaHyvaksynyt) {

            // Sähköposti esimiehelle kouluttajan hyväksymästä kehittämistoimenpiteestä
            mailService.sendEmailFromTemplate(
                kayttajaRepository.findById(kehittamistoimenpiteet.lahiesimies?.id!!).get().user!!,
                "kehittamistoimenpiteetKuitattava.html",
                "email.kehittamistoimenpiteetkuitattava.title",
                properties = mapOf(Pair(MailProperty.ID, kehittamistoimenpiteet.id!!.toString()))
            )
        } else if (kehittamistoimenpiteet.lahiesimies?.user?.id == userId) {

            // Sähköposti erikoistuvalle esimiehen hyväksymästä kehittämistoimenpiteestä
            if (kehittamistoimenpiteet.lahikouluttajaHyvaksynyt) {
                mailService.sendEmailFromTemplate(
                    kayttajaRepository.findById(kehittamistoimenpiteet?.erikoistuvaLaakari?.kayttaja?.id!!)
                        .get().user!!,
                    "kehittamistoimenpiteetKuitattava.html",
                    "email.kehittamistoimenpiteetkuitattava.title",
                    properties = mapOf(
                        Pair(
                            MailProperty.ID,
                            kehittamistoimenpiteet.id!!.toString()
                        )
                    )
                )
            }
            // Sähköposti kouluttajalle korjattavasta kehittämistoimenpiteestä
            else {
                mailService.sendEmailFromTemplate(
                    kayttajaRepository.findById(kehittamistoimenpiteet.lahikouluttaja?.id!!)
                        .get().user!!,
                    "kehittamistoimenpiteetPalautettu.html",
                    "email.kehittamistoimenpiteetpalautettu.title",
                    properties = mapOf(
                        Pair(
                            MailProperty.ID,
                            kehittamistoimenpiteet.id!!.toString()
                        )
                    )
                )
            }
        }

        return koejaksonKehittamistoimenpiteetMapper.toDto(kehittamistoimenpiteet)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<KoejaksonKehittamistoimenpiteetDTO> {
        return koejaksonKehittamistoimenpiteetRepository.findById(id)
            .map(koejaksonKehittamistoimenpiteetMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findByErikoistuvaLaakariKayttajaUserId(userId: String): Optional<KoejaksonKehittamistoimenpiteetDTO> {
        return koejaksonKehittamistoimenpiteetRepository.findByErikoistuvaLaakariKayttajaUserId(
            userId
        )
            .map(koejaksonKehittamistoimenpiteetMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOneByIdAndLahikouluttajaUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonKehittamistoimenpiteetDTO> {
        return koejaksonKehittamistoimenpiteetRepository.findOneByIdAndLahikouluttajaUserId(
            id,
            userId
        ).map(koejaksonKehittamistoimenpiteetMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOneByIdAndLahiesimiesUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonKehittamistoimenpiteetDTO> {
        return koejaksonKehittamistoimenpiteetRepository.findOneByIdAndLahiesimiesUserId(
            id,
            userId
        ).map(koejaksonKehittamistoimenpiteetMapper::toDto)
    }

    override fun findByLoppukeskusteluId(id: Long): Optional<KoejaksonKehittamistoimenpiteetDTO> {
        return koejaksonKehittamistoimenpiteetRepository.findByLoppukeskusteluId(
            id
        ).map(koejaksonKehittamistoimenpiteetMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findAllByKouluttajaUserId(userId: String): Map<KayttajaDTO, KoejaksonKehittamistoimenpiteetDTO> {
        val kehittamistoimenpiteet =
            koejaksonKehittamistoimenpiteetRepository.findAllByLahikouluttajaUserIdOrLahiesimiesUserId(
                userId, userId
            )
        return kehittamistoimenpiteet.associate {
            kayttajaMapper.toDto(it.erikoistuvaLaakari?.kayttaja!!) to koejaksonKehittamistoimenpiteetMapper.toDto(
                it
            )
        }
    }

    override fun delete(id: Long) {
        koejaksonKehittamistoimenpiteetRepository.deleteById(id)
    }
}
