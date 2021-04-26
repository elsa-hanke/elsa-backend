package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.KayttajaRepository
import fi.elsapalvelu.elsa.repository.KoejaksonValiarviointiRepository
import fi.elsapalvelu.elsa.service.KoejaksonValiarviointiService
import fi.elsapalvelu.elsa.service.MailProperty
import fi.elsapalvelu.elsa.service.MailService
import fi.elsapalvelu.elsa.service.dto.KayttajaDTO
import fi.elsapalvelu.elsa.service.dto.KoejaksonValiarviointiDTO
import fi.elsapalvelu.elsa.service.mapper.KayttajaMapper
import fi.elsapalvelu.elsa.service.mapper.KoejaksonValiarviointiMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import javax.persistence.EntityNotFoundException


@Service
@Transactional
class KoejaksonValiarviointiServiceImpl(
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository,
    private val koejaksonValiarviointiRepository: KoejaksonValiarviointiRepository,
    private val koejaksonValiarviointiMapper: KoejaksonValiarviointiMapper,
    private val mailService: MailService,
    private val kayttajaRepository: KayttajaRepository,
    private val kayttajaMapper: KayttajaMapper
) : KoejaksonValiarviointiService {

    override fun create(
        koejaksonValiarviointiDTO: KoejaksonValiarviointiDTO,
        userId: String
    ): KoejaksonValiarviointiDTO {
        val kirjautunutErikoistuvaLaakari =
            erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)
        var valiarvointi =
            koejaksonValiarviointiMapper.toEntity(koejaksonValiarviointiDTO)
        valiarvointi.muokkauspaiva = LocalDate.now(ZoneId.systemDefault())
        valiarvointi.erikoistuvaLaakari = kirjautunutErikoistuvaLaakari
        valiarvointi = koejaksonValiarviointiRepository.save(valiarvointi)

        // Sähköposti kouluttajalle
        mailService.sendEmailFromTemplate(
            kayttajaRepository.findById(valiarvointi.lahikouluttaja?.id!!).get().user!!,
            "valiarviointiKouluttajalle.html",
            "email.valiarviointikouluttajalle.title",
            properties = mapOf(Pair(MailProperty.ID, valiarvointi.id!!.toString()))
        )

        return koejaksonValiarviointiMapper.toDto(valiarvointi)
    }

    override fun update(
        koejaksonValiarviointiDTO: KoejaksonValiarviointiDTO,
        userId: String
    ): KoejaksonValiarviointiDTO {

        var valiarviointi =
            koejaksonValiarviointiRepository.findById(koejaksonValiarviointiDTO.id!!)
                .orElseThrow { EntityNotFoundException("Väliarviointia ei löydy") }

        val updatedValiarviointi =
            koejaksonValiarviointiMapper.toEntity(koejaksonValiarviointiDTO)

        if (valiarviointi.erikoistuvaLaakari?.kayttaja?.user?.id == userId && valiarviointi.lahiesimiesHyvaksynyt) {
            valiarviointi.erikoistuvaAllekirjoittanut = true
            valiarviointi.muokkauspaiva = LocalDate.now(ZoneId.systemDefault())
        }

        if (valiarviointi.lahikouluttaja?.user?.id == userId && !valiarviointi.lahiesimiesHyvaksynyt) {
            valiarviointi.edistyminenTavoitteidenMukaista =
                updatedValiarviointi.edistyminenTavoitteidenMukaista
            valiarviointi.vahvuudet = updatedValiarviointi.vahvuudet
            valiarviointi.kehittamistoimenpiteet = updatedValiarviointi.kehittamistoimenpiteet
            valiarviointi.lahikouluttajaHyvaksynyt = true
            valiarviointi.lahikouluttajanKuittausaika =
                LocalDate.now(ZoneId.systemDefault())
        }

        if (valiarviointi.lahiesimies?.user?.id == userId && valiarviointi.lahikouluttajaHyvaksynyt) {
            // Hyväksytty
            if (valiarviointi.korjausehdotus.isNullOrBlank()) {
                valiarviointi.lahiesimiesHyvaksynyt = true
                valiarviointi.lahiesimiehenKuittausaika = LocalDate.now(ZoneId.systemDefault())
            }
            // Palautettu korjattavaksi
            else {
                valiarviointi.korjausehdotus = updatedValiarviointi.korjausehdotus
                valiarviointi.lahikouluttajaHyvaksynyt = false
                valiarviointi.lahikouluttajanKuittausaika = null
            }
        }

        valiarviointi = koejaksonValiarviointiRepository.save(valiarviointi)

        // Sähköposti kouluttajalle ja esimiehelle allekirjoitetusta väliarvioinnista
        if (valiarviointi.erikoistuvaLaakari?.kayttaja?.user?.id == userId && valiarviointi.erikoistuvaAllekirjoittanut) {
            val erikoistuvaLaakari =
                kayttajaRepository.findById(valiarviointi?.erikoistuvaLaakari?.kayttaja?.id!!)
                    .get().user!!
            mailService.sendEmailFromTemplate(
                kayttajaRepository.findById(valiarviointi.lahikouluttaja?.id!!).get().user!!,
                "valiarviointiHyvaksytty.html",
                "email.valiarviointihyvaksytty.title",
                properties = mapOf(
                    Pair(MailProperty.ID, valiarviointi.id!!.toString()),
                    Pair(MailProperty.NAME, erikoistuvaLaakari.getName())
                )
            )
            mailService.sendEmailFromTemplate(
                kayttajaRepository.findById(valiarviointi.lahiesimies?.id!!).get().user!!,
                "valiarviointiHyvaksytty.html",
                "email.valiarviointihyvaksytty.title",
                properties = mapOf(
                    Pair(MailProperty.ID, valiarviointi.id!!.toString()),
                    Pair(MailProperty.NAME, erikoistuvaLaakari.getName())
                )
            )
        } else if (valiarviointi.lahikouluttaja?.user?.id == userId && valiarviointi.lahikouluttajaHyvaksynyt) {

            // Sähköposti esimiehelle kouluttajan hyväksymästä väliarvioinnista
            mailService.sendEmailFromTemplate(
                kayttajaRepository.findById(valiarviointi.lahiesimies?.id!!).get().user!!,
                "valiarviointiKuitattava.html",
                "email.valiarviointikuitattava.title",
                properties = mapOf(Pair(MailProperty.ID, valiarviointi.id!!.toString()))
            )
        } else if (valiarviointi.lahiesimies?.user?.id == userId) {

            // Sähköposti erikoistuvalle esimiehen hyväksymästä väliarvioinnista
            if (valiarviointi.lahikouluttajaHyvaksynyt) {
                mailService.sendEmailFromTemplate(
                    kayttajaRepository.findById(valiarviointi?.erikoistuvaLaakari?.kayttaja?.id!!)
                        .get().user!!,
                    "valiarviointiKuitattava.html",
                    "email.valiarviointikuitattava.title",
                    properties = mapOf(Pair(MailProperty.ID, valiarviointi.id!!.toString()))
                )
            }
            // Sähköposti kouluttajalle korjattavasta väliarvioinnista
            else {
                mailService.sendEmailFromTemplate(
                    kayttajaRepository.findById(valiarviointi.lahikouluttaja?.id!!)
                        .get().user!!,
                    "valiarviointiPalautettu.html",
                    "email.valiarviointipalautettu.title",
                    properties = mapOf(Pair(MailProperty.ID, valiarviointi.id!!.toString()))
                )
            }
        }

        return koejaksonValiarviointiMapper.toDto(valiarviointi)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<KoejaksonValiarviointiDTO> {
        return koejaksonValiarviointiRepository.findById(id)
            .map(koejaksonValiarviointiMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findByErikoistuvaLaakariKayttajaUserId(userId: String): Optional<KoejaksonValiarviointiDTO> {
        return koejaksonValiarviointiRepository.findByErikoistuvaLaakariKayttajaUserId(userId)
            .map(koejaksonValiarviointiMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOneByIdAndLahikouluttajaUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonValiarviointiDTO> {
        return koejaksonValiarviointiRepository.findOneByIdAndLahikouluttajaUserId(
            id,
            userId
        ).map(koejaksonValiarviointiMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOneByIdAndLahiesimiesUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonValiarviointiDTO> {
        return koejaksonValiarviointiRepository.findOneByIdAndLahiesimiesUserId(
            id,
            userId
        ).map(koejaksonValiarviointiMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findAllByKouluttajaUserId(userId: String): Map<KayttajaDTO, KoejaksonValiarviointiDTO> {
        val valiarvioinnit =
            koejaksonValiarviointiRepository.findAllByLahikouluttajaUserIdOrLahiesimiesUserId(
                userId, userId
            )
        return valiarvioinnit.associate {
            kayttajaMapper.toDto(it.erikoistuvaLaakari?.kayttaja!!) to koejaksonValiarviointiMapper.toDto(
                it
            )
        }
    }

    override fun delete(id: Long) {
        koejaksonValiarviointiRepository.deleteById(id)
    }
}
