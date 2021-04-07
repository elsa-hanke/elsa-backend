package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.KoejaksonAloituskeskusteluRepository
import fi.elsapalvelu.elsa.service.KoejaksonAloituskeskusteluService
import fi.elsapalvelu.elsa.service.dto.KoejaksonAloituskeskusteluDTO
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
    private val koejaksonAloituskeskusteluMapper: KoejaksonAloituskeskusteluMapper
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
        return koejaksonAloituskeskusteluMapper.toDto(aloituskeskustelu)
    }

    override fun update(
        koejaksonAloituskeskusteluDTO: KoejaksonAloituskeskusteluDTO,
        userId: String
    ): KoejaksonAloituskeskusteluDTO {

        var aloituskeskustelu =
            koejaksonAloituskeskusteluRepository.findById(koejaksonAloituskeskusteluDTO.id!!)
                .orElseThrow { EntityNotFoundException("Koulutussopimusta ei löydy") }

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
        return koejaksonAloituskeskusteluMapper.toDto(aloituskeskustelu)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<KoejaksonAloituskeskusteluDTO> {
        return koejaksonAloituskeskusteluRepository.findById(id)
            .map(koejaksonAloituskeskusteluMapper::toDto)
    }

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

    override fun findOneByIdAndLahiesimiesUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonAloituskeskusteluDTO> {
        return koejaksonAloituskeskusteluRepository.findOneByIdAndLahiesimiesUserId(
            id,
            userId
        ).map(koejaksonAloituskeskusteluMapper::toDto)
    }

    override fun delete(id: Long) {
        koejaksonAloituskeskusteluRepository.deleteById(id)
    }
}
