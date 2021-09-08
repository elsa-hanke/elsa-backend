package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.KoejaksonValiarviointi
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
        kayttajaId: String
    ): KoejaksonValiarviointiDTO {
        val kirjautunutErikoistuvaLaakari =
            erikoistuvaLaakariRepository.findOneByKayttajaId(kayttajaId)
        var valiarvointi =
            koejaksonValiarviointiMapper.toEntity(koejaksonValiarviointiDTO)
        valiarvointi.erikoistuvaLaakari = kirjautunutErikoistuvaLaakari
        valiarvointi = koejaksonValiarviointiRepository.save(valiarvointi)

        // Sähköposti kouluttajalle
        mailService.sendEmailFromTemplate(
            kayttajaRepository.findById(valiarvointi.lahikouluttaja?.id!!).get(),
            "valiarviointiKouluttajalle.html",
            "email.valiarviointikouluttajalle.title",
            properties = mapOf(Pair(MailProperty.ID, valiarvointi.id!!.toString()))
        )

        return koejaksonValiarviointiMapper.toDto(valiarvointi)
    }

    override fun update(
        koejaksonValiarviointiDTO: KoejaksonValiarviointiDTO,
        kayttajaId: String
    ): KoejaksonValiarviointiDTO {

        var valiarviointi =
            koejaksonValiarviointiRepository.findById(koejaksonValiarviointiDTO.id!!)
                .orElseThrow { EntityNotFoundException("Väliarviointia ei löydy") }

        val updatedValiarviointi = koejaksonValiarviointiMapper.toEntity(koejaksonValiarviointiDTO)

        if (valiarviointi.erikoistuvaLaakari?.kayttaja?.id == kayttajaId && valiarviointi.lahiesimiesHyvaksynyt) {
            valiarviointi = handleErikoistuva(valiarviointi)
        }

        if (valiarviointi.lahikouluttaja?.id == kayttajaId && !valiarviointi.lahiesimiesHyvaksynyt) {
            valiarviointi = handleKouluttaja(valiarviointi, updatedValiarviointi)
        }

        if (valiarviointi.lahiesimies?.id == kayttajaId && valiarviointi.lahikouluttajaHyvaksynyt) {
            valiarviointi = handleEsimies(valiarviointi, updatedValiarviointi)
        }

        return koejaksonValiarviointiMapper.toDto(valiarviointi)
    }

    private fun handleErikoistuva(valiarviointi: KoejaksonValiarviointi): KoejaksonValiarviointi {
        valiarviointi.erikoistuvaAllekirjoittanut = true
        valiarviointi.erikoistuvanAllekirjoitusaika = LocalDate.now()

        val result = koejaksonValiarviointiRepository.save(valiarviointi)

        // Sähköposti kouluttajalle ja esimiehelle allekirjoitetusta väliarvioinnista
        if (result.erikoistuvaAllekirjoittanut) {
            val erikoistuvaLaakariKayttaja =
                kayttajaRepository.findById(result.erikoistuvaLaakari?.kayttaja?.id!!)
                    .get()
            mailService.sendEmailFromTemplate(
                kayttajaRepository.findById(result.lahikouluttaja?.id!!).get(),
                "valiarviointiHyvaksytty.html",
                "email.valiarviointihyvaksytty.title",
                properties = mapOf(
                    Pair(MailProperty.ID, result.id!!.toString()),
                    Pair(
                        MailProperty.NAME,
                        "${erikoistuvaLaakariKayttaja.etunimi} ${erikoistuvaLaakariKayttaja.sukunimi}"
                    )
                )
            )
            mailService.sendEmailFromTemplate(
                kayttajaRepository.findById(result.lahiesimies?.id!!).get(),
                "valiarviointiHyvaksytty.html",
                "email.valiarviointihyvaksytty.title",
                properties = mapOf(
                    Pair(MailProperty.ID, result.id!!.toString()),
                    Pair(
                        MailProperty.NAME,
                        "${erikoistuvaLaakariKayttaja.etunimi} ${erikoistuvaLaakariKayttaja.sukunimi}"
                    )
                )
            )
        }

        return result
    }

    private fun handleKouluttaja(
        valiarviointi: KoejaksonValiarviointi,
        updated: KoejaksonValiarviointi
    ): KoejaksonValiarviointi {
        valiarviointi.edistyminenTavoitteidenMukaista = updated.edistyminenTavoitteidenMukaista
        valiarviointi.kehittamistoimenpideKategoriat = updated.kehittamistoimenpideKategoriat
        valiarviointi.muuKategoria = updated.muuKategoria
        valiarviointi.vahvuudet = updated.vahvuudet
        valiarviointi.kehittamistoimenpiteet = updated.kehittamistoimenpiteet
        valiarviointi.lahikouluttajaHyvaksynyt = true
        valiarviointi.lahikouluttajanKuittausaika = LocalDate.now(ZoneId.systemDefault())
        valiarviointi.korjausehdotus = null

        val result = koejaksonValiarviointiRepository.save(valiarviointi)

        // Sähköposti kouluttajalle ja esimiehelle allekirjoitetusta väliarvioinnista
        if (result.lahikouluttajaHyvaksynyt) {
            // Sähköposti esimiehelle kouluttajan hyväksymästä väliarvioinnista
            mailService.sendEmailFromTemplate(
                kayttajaRepository.findById(result.lahiesimies?.id!!).get(),
                "valiarviointiKuitattava.html",
                "email.valiarviointikuitattava.title",
                properties = mapOf(Pair(MailProperty.ID, result.id!!.toString()))
            )
        }

        return result
    }

    private fun handleEsimies(
        valiarviointi: KoejaksonValiarviointi,
        updated: KoejaksonValiarviointi
    ): KoejaksonValiarviointi {
        // Hyväksytty
        if (updated.korjausehdotus.isNullOrBlank()) {
            valiarviointi.lahiesimiesHyvaksynyt = true
            valiarviointi.lahiesimiehenKuittausaika = LocalDate.now(ZoneId.systemDefault())
        }
        // Palautettu korjattavaksi
        else {
            valiarviointi.korjausehdotus = updated.korjausehdotus
            valiarviointi.lahikouluttajaHyvaksynyt = false
            valiarviointi.lahikouluttajanKuittausaika = null
        }

        val result = koejaksonValiarviointiRepository.save(valiarviointi)

        // Sähköposti erikoistuvalle esimiehen hyväksymästä väliarvioinnista
        if (result.lahikouluttajaHyvaksynyt) {
            mailService.sendEmailFromTemplate(
                kayttajaRepository.findById(result.erikoistuvaLaakari?.kayttaja?.id!!)
                    .get(),
                "valiarviointiKuitattava.html",
                "email.valiarviointikuitattava.title",
                properties = mapOf(Pair(MailProperty.ID, result.id!!.toString()))
            )
        }
        // Sähköposti kouluttajalle korjattavasta väliarvioinnista
        else {
            mailService.sendEmailFromTemplate(
                kayttajaRepository.findById(result.lahikouluttaja?.id!!)
                    .get(),
                "valiarviointiPalautettu.html",
                "email.valiarviointipalautettu.title",
                properties = mapOf(Pair(MailProperty.ID, result.id!!.toString()))
            )
        }

        return result
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<KoejaksonValiarviointiDTO> {
        return koejaksonValiarviointiRepository.findById(id)
            .map(koejaksonValiarviointiMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findByErikoistuvaLaakariKayttajaId(kayttajaId: String): Optional<KoejaksonValiarviointiDTO> {
        return koejaksonValiarviointiRepository.findByErikoistuvaLaakariKayttajaId(kayttajaId)
            .map(koejaksonValiarviointiMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOneByIdAndLahikouluttajaId(
        id: Long,
        kayttajaId: String
    ): Optional<KoejaksonValiarviointiDTO> {
        return koejaksonValiarviointiRepository.findOneByIdAndLahikouluttajaId(
            id,
            kayttajaId
        ).map(koejaksonValiarviointiMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOneByIdAndLahiesimiesId(
        id: Long,
        kayttajaId: String
    ): Optional<KoejaksonValiarviointiDTO> {
        return koejaksonValiarviointiRepository.findOneByIdAndLahiesimiesId(
            id,
            kayttajaId
        ).map(koejaksonValiarviointiMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findAllByKouluttajaId(kayttajaId: String): Map<KayttajaDTO, KoejaksonValiarviointiDTO> {
        val valiarvioinnit =
            koejaksonValiarviointiRepository.findAllByLahikouluttajaIdOrLahiesimiesId(kayttajaId)
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
