package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.KoejaksonLoppukeskustelu
import fi.elsapalvelu.elsa.repository.*
import fi.elsapalvelu.elsa.service.KoejaksonLoppukeskusteluService
import fi.elsapalvelu.elsa.service.MailProperty
import fi.elsapalvelu.elsa.service.MailService
import fi.elsapalvelu.elsa.service.dto.KoejaksonLoppukeskusteluDTO
import fi.elsapalvelu.elsa.service.mapper.KoejaksonLoppukeskusteluMapper
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import javax.persistence.EntityNotFoundException

@Service
@Transactional
class KoejaksonLoppukeskusteluServiceImpl(
    private val koejaksonLoppukeskusteluRepository: KoejaksonLoppukeskusteluRepository,
    private val koejaksonLoppukeskusteluMapper: KoejaksonLoppukeskusteluMapper,
    private val mailService: MailService,
    private val kayttajaRepository: KayttajaRepository,
    private val opintooikeusRepository: OpintooikeusRepository,
    private val koejaksonAloituskeskusteluRepository: KoejaksonAloituskeskusteluRepository,
    private val koejaksonValiarviointiRepository: KoejaksonValiarviointiRepository,
    private val koejaksonKehittamistoimenpiteetRepository: KoejaksonKehittamistoimenpiteetRepository
) : KoejaksonLoppukeskusteluService {

    override fun create(
        koejaksonLoppukeskusteluDTO: KoejaksonLoppukeskusteluDTO,
        opintooikeusId: Long
    ): KoejaksonLoppukeskusteluDTO? {
        return opintooikeusRepository.findByIdOrNull(opintooikeusId)?.let {
            var loppukeskustelu =
                koejaksonLoppukeskusteluMapper.toEntity(koejaksonLoppukeskusteluDTO)
            loppukeskustelu.opintooikeus = it
            loppukeskustelu = koejaksonLoppukeskusteluRepository.save(loppukeskustelu)

            // Sähköposti kouluttajalle
            mailService.sendEmailFromTemplate(
                kayttajaRepository.findById(loppukeskustelu.lahikouluttaja?.id!!).get().user!!,
                "loppukeskusteluKouluttajalle.html",
                "email.loppukeskustelukouluttajalle.title",
                properties = mapOf(Pair(MailProperty.ID, loppukeskustelu.id!!.toString()))
            )

            koejaksonLoppukeskusteluMapper.toDto(loppukeskustelu)
        }
    }

    override fun update(
        koejaksonLoppukeskusteluDTO: KoejaksonLoppukeskusteluDTO,
        userId: String
    ): KoejaksonLoppukeskusteluDTO {
        var loppukeskustelu =
            koejaksonLoppukeskusteluRepository.findById(koejaksonLoppukeskusteluDTO.id!!)
                .orElseThrow { EntityNotFoundException("Loppukeskustelua ei löydy") }

        val updatedLoppukeskustelu =
            koejaksonLoppukeskusteluMapper.toEntity(koejaksonLoppukeskusteluDTO)

        if (loppukeskustelu.lahikouluttaja?.user?.id == userId && !loppukeskustelu.lahiesimiesHyvaksynyt) {
            loppukeskustelu = handleKouluttaja(loppukeskustelu, updatedLoppukeskustelu)
        }

        if (loppukeskustelu.lahiesimies?.user?.id == userId && loppukeskustelu.lahikouluttajaHyvaksynyt) {
            loppukeskustelu = handleEsimies(loppukeskustelu, updatedLoppukeskustelu)
        }

        return koejaksonLoppukeskusteluMapper.toDto(loppukeskustelu)
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
            kayttajaRepository.findById(loppukeskustelu.lahiesimies?.id!!).get().user!!,
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
                kayttajaRepository.findById(loppukeskustelu.opintooikeus?.erikoistuvaLaakari?.kayttaja?.id!!)
                    .get().user!!,
                "loppukeskusteluKuitattava.html",
                "email.loppukeskustelukuitattava.title",
                properties = mapOf(Pair(MailProperty.ID, loppukeskustelu.id!!.toString()))
            )
        }
        // Sähköposti kouluttajalle korjattavasta loppukeskustelusta
        else {
            mailService.sendEmailFromTemplate(
                kayttajaRepository.findById(loppukeskustelu.lahikouluttaja?.id!!)
                    .get().user!!,
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
            .map(this::mapLoppukeskustelu)
    }

    @Transactional(readOnly = true)
    override fun findByOpintooikeusId(opintooikeusId: Long): Optional<KoejaksonLoppukeskusteluDTO> {
        return koejaksonLoppukeskusteluRepository.findByOpintooikeusId(opintooikeusId)
            .map(this::mapLoppukeskustelu)
    }

    @Transactional(readOnly = true)
    override fun findOneByIdAndLahikouluttajaUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonLoppukeskusteluDTO> {
        return koejaksonLoppukeskusteluRepository.findOneByIdAndLahikouluttajaUserId(id, userId)
            .map(this::mapLoppukeskustelu)
    }

    @Transactional(readOnly = true)
    override fun findOneByIdAndLahiesimiesUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonLoppukeskusteluDTO> {
        return koejaksonLoppukeskusteluRepository.findOneByIdAndLahiesimiesUserId(
            id,
            userId
        ).map(this::mapLoppukeskustelu)
    }

    @Transactional(readOnly = true)
    override fun findOneByIdHyvaksyttyAndBelongsToVastuuhenkilo(
        id: Long,
        vastuuhenkiloUserId: String
    ): Optional<KoejaksonLoppukeskusteluDTO> {
        return koejaksonLoppukeskusteluRepository.findOneByIdHyvaksyttyAndBelongsToVastuuhenkilo(
            id,
            vastuuhenkiloUserId
        )
            .map(this::mapLoppukeskustelu)
    }

    override fun delete(id: Long) {
        koejaksonLoppukeskusteluRepository.deleteById(id)
    }

    private fun mapLoppukeskustelu(loppukeskustelu: KoejaksonLoppukeskustelu): KoejaksonLoppukeskusteluDTO {
        val result = koejaksonLoppukeskusteluMapper.toDto(loppukeskustelu)
        val opintoOikeusId = loppukeskustelu.opintooikeus?.id!!
        result.koejaksonOsaamistavoitteet =
            koejaksonAloituskeskusteluRepository.findByOpintooikeusId(opintoOikeusId)
                .get().koejaksonOsaamistavoitteet
        koejaksonValiarviointiRepository.findByOpintooikeusId(opintoOikeusId).let {
            result.edistyminenTavoitteidenMukaista = it.get().edistyminenTavoitteidenMukaista
            result.kehittamistoimenpideKategoriat =
                it.get().kehittamistoimenpideKategoriat?.toList()
            result.muuKategoria = it.get().muuKategoria
            result.vahvuudet = it.get().vahvuudet
            result.kehittamistoimenpiteet = it.get().kehittamistoimenpiteet
        }
        if (result.edistyminenTavoitteidenMukaista == false) {
            result.kehittamistoimenpiteetRiittavat =
                koejaksonKehittamistoimenpiteetRepository.findByOpintooikeusId(opintoOikeusId)
                    .get().kehittamistoimenpiteetRiittavat
        }
        return result
    }
}
