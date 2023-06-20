package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.KoejaksonValiarviointi
import fi.elsapalvelu.elsa.domain.enumeration.VastuuhenkilonTehtavatyyppiEnum
import fi.elsapalvelu.elsa.repository.KayttajaRepository
import fi.elsapalvelu.elsa.repository.KoejaksonAloituskeskusteluRepository
import fi.elsapalvelu.elsa.repository.KoejaksonValiarviointiRepository
import fi.elsapalvelu.elsa.repository.OpintooikeusRepository
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.service.KoejaksonValiarviointiService
import fi.elsapalvelu.elsa.service.KouluttajavaltuutusService
import fi.elsapalvelu.elsa.service.MailProperty
import fi.elsapalvelu.elsa.service.MailService
import fi.elsapalvelu.elsa.service.dto.KoejaksonValiarviointiDTO
import fi.elsapalvelu.elsa.service.mapper.KoejaksonValiarviointiMapper
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import jakarta.persistence.EntityNotFoundException


@Service
@Transactional
class KoejaksonValiarviointiServiceImpl(
    private val koejaksonValiarviointiRepository: KoejaksonValiarviointiRepository,
    private val koejaksonValiarviointiMapper: KoejaksonValiarviointiMapper,
    private val mailService: MailService,
    private val kayttajaRepository: KayttajaRepository,
    private val opintooikeusRepository: OpintooikeusRepository,
    private val koejaksonAloituskeskusteluRepository: KoejaksonAloituskeskusteluRepository,
    private val kouluttajavaltuutusService: KouluttajavaltuutusService
) : KoejaksonValiarviointiService {

    override fun create(
        koejaksonValiarviointiDTO: KoejaksonValiarviointiDTO,
        opintooikeusId: Long
    ): KoejaksonValiarviointiDTO? {
        return opintooikeusRepository.findByIdOrNull(opintooikeusId)?.let {
            var valiarviointi =
                koejaksonValiarviointiMapper.toEntity(koejaksonValiarviointiDTO)
            valiarviointi.opintooikeus = it
            valiarviointi = koejaksonValiarviointiRepository.save(valiarviointi)

            kouluttajavaltuutusService.lisaaValtuutus(
                valiarviointi.opintooikeus?.erikoistuvaLaakari?.kayttaja?.user?.id!!,
                valiarviointi.lahikouluttaja?.id!!
            )
            kouluttajavaltuutusService.lisaaValtuutus(
                valiarviointi.opintooikeus?.erikoistuvaLaakari?.kayttaja?.user?.id!!,
                valiarviointi.lahiesimies?.id!!
            )

            // Sähköposti kouluttajalle
            mailService.sendEmailFromTemplate(
                kayttajaRepository.findById(valiarviointi.lahikouluttaja?.id!!).get().user!!,
                templateName = "valiarviointiKouluttajalle.html",
                titleKey = "email.valiarviointikouluttajalle.title",
                properties = mapOf(Pair(MailProperty.ID, valiarviointi.id!!.toString()))
            )

            koejaksonValiarviointiMapper.toDto(valiarviointi)
        }
    }

    override fun update(
        koejaksonValiarviointiDTO: KoejaksonValiarviointiDTO,
        userId: String
    ): KoejaksonValiarviointiDTO {
        var valiarviointi =
            koejaksonValiarviointiRepository.findById(koejaksonValiarviointiDTO.id!!)
                .orElseThrow { EntityNotFoundException("Väliarviointia ei löydy") }

        val updatedValiarviointi = koejaksonValiarviointiMapper.toEntity(koejaksonValiarviointiDTO)

        if (valiarviointi.lahikouluttaja?.user?.id == userId && !valiarviointi.lahiesimiesHyvaksynyt) {
            valiarviointi = handleKouluttaja(valiarviointi, updatedValiarviointi)
        }

        if (valiarviointi.lahiesimies?.user?.id == userId && valiarviointi.lahikouluttajaHyvaksynyt) {
            valiarviointi = handleEsimies(valiarviointi, updatedValiarviointi)
        }

        return koejaksonValiarviointiMapper.toDto(valiarviointi)
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
                kayttajaRepository.findById(result.lahiesimies?.id!!).get().user!!,
                templateName = "valiarviointiKuitattava.html",
                titleKey = "email.valiarviointikuitattava.title",
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
                kayttajaRepository.findById(result.opintooikeus?.erikoistuvaLaakari?.kayttaja?.id!!)
                    .get().user!!,
                templateName = "valiarviointiKuitattava.html",
                titleKey = "email.valiarviointikuitattava.title",
                properties = mapOf(Pair(MailProperty.ID, result.id!!.toString()))
            )
        }
        // Sähköposti kouluttajalle korjattavasta väliarvioinnista
        else {
            mailService.sendEmailFromTemplate(
                kayttajaRepository.findById(result.lahikouluttaja?.id!!)
                    .get().user!!,
                templateName = "valiarviointiPalautettu.html",
                titleKey = "email.valiarviointipalautettu.title",
                properties = mapOf(Pair(MailProperty.ID, result.id!!.toString()))
            )
        }

        return result
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<KoejaksonValiarviointiDTO> {
        return koejaksonValiarviointiRepository.findById(id)
            .map(this::mapValiarviointi)
    }

    @Transactional(readOnly = true)
    override fun findByOpintooikeusId(opintooikeusId: Long): Optional<KoejaksonValiarviointiDTO> {
        return koejaksonValiarviointiRepository.findByOpintooikeusId(opintooikeusId)
            .map(this::mapValiarviointi)
    }

    @Transactional(readOnly = true)
    override fun findOneByIdAndLahikouluttajaUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonValiarviointiDTO> {
        return koejaksonValiarviointiRepository.findOneByIdAndLahikouluttajaUserId(
            id,
            userId
        ).map(this::mapValiarviointi)
    }

    @Transactional(readOnly = true)
    override fun findOneByIdAndLahiesimiesUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonValiarviointiDTO> {
        return koejaksonValiarviointiRepository.findOneByIdAndLahiesimiesUserId(
            id,
            userId
        ).map(this::mapValiarviointi)
    }

    @Transactional(readOnly = true)
    override fun findOneByIdHyvaksyttyAndBelongsToVastuuhenkilo(
        id: Long,
        vastuuhenkiloUserId: String
    ): Optional<KoejaksonValiarviointiDTO> {
        val valiarviointi = koejaksonValiarviointiRepository.findOneByIdAndLahiesimiesHyvaksynytTrue(id).orElse(null)
            ?: return Optional.empty()
        val vastuuhenkilo =
            kayttajaRepository.findOneByAuthoritiesYliopistoErikoisalaAndVastuuhenkilonTehtavatyyppi(
                listOf(VASTUUHENKILO),
                valiarviointi.opintooikeus?.yliopisto?.id,
                valiarviointi.opintooikeus?.erikoisala?.id,
                VastuuhenkilonTehtavatyyppiEnum.KOEJAKSOSOPIMUSTEN_JA_KOEJAKSOJEN_HYVAKSYMINEN
            )
        if (valiarviointi.lahikouluttaja?.user?.id == vastuuhenkiloUserId
            || valiarviointi.lahiesimies?.user?.id == vastuuhenkiloUserId
            || vastuuhenkilo?.user?.id == vastuuhenkiloUserId) {
            return Optional.of(valiarviointi).map(this::mapValiarviointi)
        }
        return Optional.empty()
    }

    override fun delete(id: Long) {
        koejaksonValiarviointiRepository.deleteById(id)
    }

    private fun mapValiarviointi(valiarviointi: KoejaksonValiarviointi): KoejaksonValiarviointiDTO {
        val result = koejaksonValiarviointiMapper.toDto(valiarviointi)
        result.koejaksonOsaamistavoitteet =
            koejaksonAloituskeskusteluRepository.findByOpintooikeusId(valiarviointi.opintooikeus?.id!!)
                .get().koejaksonOsaamistavoitteet
        return result
    }
}
