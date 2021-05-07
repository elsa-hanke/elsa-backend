package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.KoejaksonKoulutussopimus
import fi.elsapalvelu.elsa.domain.KoulutussopimuksenKouluttaja
import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.KayttajaRepository
import fi.elsapalvelu.elsa.repository.KoejaksonKoulutussopimusRepository
import fi.elsapalvelu.elsa.service.KoejaksonKoulutussopimusService
import fi.elsapalvelu.elsa.service.MailProperty
import fi.elsapalvelu.elsa.service.MailService
import fi.elsapalvelu.elsa.service.dto.KayttajaDTO
import fi.elsapalvelu.elsa.service.dto.KoejaksonKoulutussopimusDTO
import fi.elsapalvelu.elsa.service.mapper.KayttajaMapper
import fi.elsapalvelu.elsa.service.mapper.KoejaksonKoulutussopimusMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import javax.persistence.EntityNotFoundException


@Service
@Transactional
class KoejaksonKoulutussopimusServiceImpl(
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository,
    private val koejaksonKoulutussopimusMapper: KoejaksonKoulutussopimusMapper,
    private val koejaksonKoulutussopimusRepository: KoejaksonKoulutussopimusRepository,
    private val mailService: MailService,
    private val kayttajaRepository: KayttajaRepository,
    private val kayttajaMapper: KayttajaMapper
) : KoejaksonKoulutussopimusService {

    override fun create(
        koejaksonKoulutussopimusDTO: KoejaksonKoulutussopimusDTO,
        userId: String
    ): KoejaksonKoulutussopimusDTO {
        val kirjautunutErikoistuvaLaakari =
            erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)
        var koulutussopimus = koejaksonKoulutussopimusMapper.toEntity(koejaksonKoulutussopimusDTO)
        koulutussopimus.muokkauspaiva = LocalDate.now(ZoneId.systemDefault())
        koulutussopimus.erikoistuvaLaakari = kirjautunutErikoistuvaLaakari
        koulutussopimus.koulutuspaikat.forEach { it.koulutussopimus = koulutussopimus }
        koulutussopimus.kouluttajat.forEach { it.koulutussopimus = koulutussopimus }
        koulutussopimus = koejaksonKoulutussopimusRepository.save(koulutussopimus)

        // Sähköposti kouluttajille allekirjoitetusta sopimuksesta
        if (koulutussopimus.lahetetty) {
            koulutussopimus.kouluttajat.forEach {
                mailService.sendEmailFromTemplate(
                    kayttajaRepository.findById(it.kouluttaja?.id!!).get().user!!,
                    "koulutussopimusKouluttajalle.html",
                    "email.koulutussopimuskouluttajalle.title",
                    properties = mapOf(Pair(MailProperty.ID, koulutussopimus.id!!.toString()))
                )
            }
        }

        return koejaksonKoulutussopimusMapper.toDto(koulutussopimus)
    }

    override fun update(
        koejaksonKoulutussopimusDTO: KoejaksonKoulutussopimusDTO,
        userId: String
    ): KoejaksonKoulutussopimusDTO {

        var koulutussopimus =
            koejaksonKoulutussopimusRepository.findById(koejaksonKoulutussopimusDTO.id!!)
                .orElseThrow { EntityNotFoundException("Koulutussopimusta ei löydy") }

        val kirjautunutErikoistuvaLaakari =
            erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)

        val updatedKoulutussopimus =
            koejaksonKoulutussopimusMapper.toEntity(koejaksonKoulutussopimusDTO)

        if (kirjautunutErikoistuvaLaakari != null && kirjautunutErikoistuvaLaakari == koulutussopimus.erikoistuvaLaakari) {
            koulutussopimus = handleErikoistuva(koulutussopimus, updatedKoulutussopimus)
        }

        koulutussopimus.kouluttajat.forEach {
            if (it.kouluttaja?.user?.id == userId) {
                koulutussopimus = handleKouluttaja(koulutussopimus, it, updatedKoulutussopimus)
            }
        }

        if (koulutussopimus.vastuuhenkilo?.user?.id == userId) {
            koulutussopimus = handleVastuuhenkilo(koulutussopimus, updatedKoulutussopimus)
        }

        koulutussopimus = koejaksonKoulutussopimusRepository.save(koulutussopimus)

        return koejaksonKoulutussopimusMapper.toDto(koulutussopimus)
    }

    private fun handleErikoistuva(
        koulutussopimus: KoejaksonKoulutussopimus,
        updated: KoejaksonKoulutussopimus
    ): KoejaksonKoulutussopimus {
        koulutussopimus.erikoistuvanNimi = updated.erikoistuvanNimi
        koulutussopimus.erikoistuvanOpiskelijatunnus = updated.erikoistuvanOpiskelijatunnus
        koulutussopimus.erikoistuvanSyntymaaika = updated.erikoistuvanSyntymaaika
        koulutussopimus.erikoistuvanYliopisto = updated.erikoistuvanYliopisto
        koulutussopimus.opintooikeudenMyontamispaiva = updated.opintooikeudenMyontamispaiva
        koulutussopimus.koejaksonAlkamispaiva = updated.koejaksonAlkamispaiva
        koulutussopimus.erikoistuvanPuhelinnumero = updated.erikoistuvanPuhelinnumero
        koulutussopimus.erikoistuvanSahkoposti = updated.erikoistuvanSahkoposti
        koulutussopimus.lahetetty = updated.lahetetty
        koulutussopimus.muokkauspaiva = LocalDate.now(ZoneId.systemDefault())
        koulutussopimus.vastuuhenkilo = updated.vastuuhenkilo
        koulutussopimus.vastuuhenkilonNimi = updated.vastuuhenkilonNimi
        koulutussopimus.vastuuhenkilonNimike = updated.vastuuhenkilonNimike
        koulutussopimus.kouluttajat.clear()
        koulutussopimus.kouluttajat.addAll(updated.kouluttajat)
        koulutussopimus.kouluttajat.forEach { it.koulutussopimus = koulutussopimus }
        koulutussopimus.koulutuspaikat.clear()
        koulutussopimus.koulutuspaikat.addAll(updated.koulutuspaikat)
        koulutussopimus.koulutuspaikat.forEach { it.koulutussopimus = koulutussopimus }

        if (updated.lahetetty) {
            koulutussopimus.korjausehdotus = null
        }

        val result = koejaksonKoulutussopimusRepository.save(koulutussopimus)

        // Sähköposti kouluttajille allekirjoitetusta sopimuksesta
        if (updated.lahetetty) {
            result.kouluttajat.forEach {
                mailService.sendEmailFromTemplate(
                    kayttajaRepository.findById(it.kouluttaja?.id!!).get().user!!,
                    "koulutussopimusKouluttajalle.html",
                    "email.koulutussopimuskouluttajalle.title",
                    properties = mapOf(Pair(MailProperty.ID, result.id!!.toString()))
                )
            }
        }

        return result
    }

    private fun handleKouluttaja(
        koulutussopimus: KoejaksonKoulutussopimus,
        kouluttaja: KoulutussopimuksenKouluttaja,
        updated: KoejaksonKoulutussopimus
    ): KoejaksonKoulutussopimus {
        val updatedKouluttaja = updated.kouluttajat.first { k -> k.id == kouluttaja.id }
        kouluttaja.nimi = updatedKouluttaja.nimi
        kouluttaja.nimike = updatedKouluttaja.nimike
        kouluttaja.toimipaikka = updatedKouluttaja.toimipaikka
        kouluttaja.lahiosoite = updatedKouluttaja.lahiosoite
        kouluttaja.postitoimipaikka = updatedKouluttaja.postitoimipaikka
        kouluttaja.puhelin = updatedKouluttaja.puhelin
        kouluttaja.sahkoposti = updatedKouluttaja.sahkoposti

        // Hyväksytty
        if (updated.korjausehdotus.isNullOrBlank()) {
            kouluttaja.sopimusHyvaksytty = true
            kouluttaja.kuittausaika = LocalDate.now(ZoneId.systemDefault())
        }
        // Palautettu korjattavaksi
        else {
            koulutussopimus.korjausehdotus = updated.korjausehdotus
            koulutussopimus.lahetetty = false

            koulutussopimus.kouluttajat.forEach {
                it.sopimusHyvaksytty = false
                it.kuittausaika = null
            }
        }

        val result = koejaksonKoulutussopimusRepository.save(koulutussopimus)

        // Sähköposti vastuuhenkilölle hyväksytystä sopimuksesta
        if (result.kouluttajat.all { it.sopimusHyvaksytty }) {
            mailService.sendEmailFromTemplate(
                kayttajaRepository.findById(result.vastuuhenkilo?.id!!).get().user!!,
                "koulutussopimusVastuuhenkilolle.html",
                "email.koulutussopimusvastuuhenkilolle.title",
                properties = mapOf(Pair(MailProperty.ID, result.id!!.toString()))
            )
        }

        // Sähköposti erikoistuvalle ja toiselle kouluttajalle palautetusta sopimuksesta
        else if (result.korjausehdotus != null) {
            val erikoistuvaLaakari =
                kayttajaRepository.findById(result.erikoistuvaLaakari?.kayttaja?.id!!)
                    .get().user!!
            mailService.sendEmailFromTemplate(
                erikoistuvaLaakari,
                "koulutussopimusPalautettu.html",
                "email.koulutussopimuspalautettu.title",
                properties = mapOf(Pair(MailProperty.ID, result.id!!.toString()))
            )

            result.kouluttajat.forEach {
                if (it.id != kouluttaja.id) {
                    mailService.sendEmailFromTemplate(
                        kayttajaRepository.findById(it.kouluttaja?.id!!).get().user!!,
                        "koulutussopimusPalautettuKouluttaja.html",
                        "email.koulutussopimuspalautettu.title",
                        properties = mapOf(
                            Pair(
                                MailProperty.NAME,
                                erikoistuvaLaakari.getName()
                            ),
                            Pair(MailProperty.TEXT, result.korjausehdotus!!)
                        )
                    )
                }
            }
        }

        return result
    }

    private fun handleVastuuhenkilo(
        koulutussopimus: KoejaksonKoulutussopimus,
        updated: KoejaksonKoulutussopimus
    ): KoejaksonKoulutussopimus {
        // Hyväksytty
        if (updated.korjausehdotus.isNullOrBlank()) {
            koulutussopimus.vastuuhenkiloHyvaksynyt = true
            koulutussopimus.vastuuhenkilonKuittausaika = LocalDate.now(ZoneId.systemDefault())
        }
        // Palautettu korjattavaksi
        else {
            koulutussopimus.korjausehdotus = updated.korjausehdotus
            koulutussopimus.lahetetty = false

            koulutussopimus.kouluttajat.forEach {
                it.sopimusHyvaksytty = false
                it.kuittausaika = null
            }
        }

        val result = koejaksonKoulutussopimusRepository.save(koulutussopimus)

        // Sähköposti erikoistujalle, kouluttajille ja opintohallinnolle hyväksytystä sopimuksesta
        if (result.vastuuhenkiloHyvaksynyt) {
            val erikoistuvaLaakari =
                kayttajaRepository.findById(result.erikoistuvaLaakari?.kayttaja?.id!!)
                    .get().user!!
            mailService.sendEmailFromTemplate(
                erikoistuvaLaakari,
                "koulutussopimusHyvaksytty.html",
                "email.koulutussopimushyvaksytty.title",
                properties = mapOf(Pair(MailProperty.ID, result.id!!.toString()))
            )

            result.kouluttajat.forEach {
                mailService.sendEmailFromTemplate(
                    kayttajaRepository.findById(it.kouluttaja?.id!!).get().user!!,
                    "koulutussopimusHyvaksyttyKouluttaja.html",
                    "email.koulutussopimushyvaksytty.title",
                    properties = mapOf(
                        Pair(MailProperty.ID, result.id!!.toString()),
                        Pair(MailProperty.NAME, erikoistuvaLaakari.getName())
                    )
                )
            }

            // TODO: Opintohallinto
        }
        // Sähköposti erikoistujalle ja kouluttajille palautetusta sopimuksesta
        else {
            val erikoistuvaLaakari =
                kayttajaRepository.findById(result.erikoistuvaLaakari?.kayttaja?.id!!)
                    .get().user!!
            mailService.sendEmailFromTemplate(
                erikoistuvaLaakari,
                "koulutussopimusPalautettu.html",
                "email.koulutussopimuspalautettu.title",
                properties = mapOf(Pair(MailProperty.ID, result.id!!.toString()))
            )

            result.kouluttajat.forEach {
                mailService.sendEmailFromTemplate(
                    kayttajaRepository.findById(it.kouluttaja?.id!!).get().user!!,
                    "koulutussopimusPalautettuKouluttaja.html",
                    "email.koulutussopimuspalautettu.title",
                    properties = mapOf(
                        Pair(MailProperty.NAME, erikoistuvaLaakari.getName()),
                        Pair(MailProperty.TEXT, result.korjausehdotus!!)
                    )
                )
            }
        }

        return result
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<KoejaksonKoulutussopimusDTO> {
        return koejaksonKoulutussopimusRepository.findById(id)
            .map(koejaksonKoulutussopimusMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findByErikoistuvaLaakariKayttajaUserId(userId: String): Optional<KoejaksonKoulutussopimusDTO> {
        return koejaksonKoulutussopimusRepository.findByErikoistuvaLaakariKayttajaUserId(userId)
            .map(koejaksonKoulutussopimusMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOneByIdAndKouluttajaKayttajaUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonKoulutussopimusDTO> {
        return koejaksonKoulutussopimusRepository.findOneByIdAndKouluttajatKouluttajaUserId(
            id,
            userId
        ).map(koejaksonKoulutussopimusMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findAllByKouluttajaKayttajaUserId(userId: String): Map<KayttajaDTO, KoejaksonKoulutussopimusDTO> {
        val sopimukset = koejaksonKoulutussopimusRepository.findAllByKouluttajatKouluttajaUserId(
            userId
        )
        return sopimukset.associate {
            kayttajaMapper.toDto(it.erikoistuvaLaakari?.kayttaja!!) to koejaksonKoulutussopimusMapper.toDto(
                it
            )
        }
    }

    @Transactional(readOnly = true)
    override fun findOneByIdAndVastuuhenkiloKayttajaUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonKoulutussopimusDTO> {
        return koejaksonKoulutussopimusRepository.findOneByIdAndVastuuhenkiloUserId(
            id,
            userId
        ).map(koejaksonKoulutussopimusMapper::toDto)
    }

    override fun delete(id: Long) {
        koejaksonKoulutussopimusRepository.deleteById(id)
    }
}
