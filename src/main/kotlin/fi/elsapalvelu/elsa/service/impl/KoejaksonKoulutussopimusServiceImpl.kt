package fi.elsapalvelu.elsa.service.impl

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

        var sopimuksenKouluttaja = false

        if (kirjautunutErikoistuvaLaakari != null && kirjautunutErikoistuvaLaakari == koulutussopimus.erikoistuvaLaakari) {
            koulutussopimus.erikoistuvanNimi = updatedKoulutussopimus.erikoistuvanNimi
            koulutussopimus.erikoistuvanOpiskelijatunnus =
                updatedKoulutussopimus.erikoistuvanOpiskelijatunnus
            koulutussopimus.erikoistuvanSyntymaaika = updatedKoulutussopimus.erikoistuvanSyntymaaika
            koulutussopimus.erikoistuvanYliopisto = updatedKoulutussopimus.erikoistuvanYliopisto
            koulutussopimus.opintooikeudenMyontamispaiva =
                updatedKoulutussopimus.opintooikeudenMyontamispaiva
            koulutussopimus.koejaksonAlkamispaiva = updatedKoulutussopimus.koejaksonAlkamispaiva
            koulutussopimus.erikoistuvanPuhelinnumero =
                updatedKoulutussopimus.erikoistuvanPuhelinnumero
            koulutussopimus.erikoistuvanSahkoposti = updatedKoulutussopimus.erikoistuvanSahkoposti
            koulutussopimus.lahetetty = updatedKoulutussopimus.lahetetty
            koulutussopimus.muokkauspaiva = LocalDate.now(ZoneId.systemDefault())
            koulutussopimus.vastuuhenkilo = updatedKoulutussopimus.vastuuhenkilo
            koulutussopimus.vastuuhenkilonNimi = updatedKoulutussopimus.vastuuhenkilonNimi
            koulutussopimus.vastuuhenkilonNimike = updatedKoulutussopimus.vastuuhenkilonNimike
            koulutussopimus.kouluttajat.clear()
            koulutussopimus.kouluttajat.addAll(updatedKoulutussopimus.kouluttajat)
            koulutussopimus.kouluttajat.forEach { it.koulutussopimus = koulutussopimus }
            koulutussopimus.koulutuspaikat.clear()
            koulutussopimus.koulutuspaikat.addAll(updatedKoulutussopimus.koulutuspaikat)
            koulutussopimus.koulutuspaikat.forEach { it.koulutussopimus = koulutussopimus }

            if (updatedKoulutussopimus.lahetetty) {
                koulutussopimus.korjausehdotus = null
            }
        }

        koulutussopimus.kouluttajat.forEach {
            if (it.kouluttaja?.user?.id == userId) {
                sopimuksenKouluttaja = true
                val updatedKouluttaja =
                    updatedKoulutussopimus.kouluttajat.first { k -> k.id == it.id }
                it.nimi = updatedKouluttaja.nimi
                it.nimike = updatedKouluttaja.nimike
                it.toimipaikka = updatedKouluttaja.toimipaikka
                it.lahiosoite = updatedKouluttaja.lahiosoite
                it.postitoimipaikka = updatedKouluttaja.postitoimipaikka
                it.puhelin = updatedKouluttaja.puhelin
                it.sahkoposti = updatedKouluttaja.sahkoposti

                // Hyväksytty
                if (updatedKoulutussopimus.korjausehdotus.isNullOrBlank()) {
                    it.sopimusHyvaksytty = true
                    it.kuittausaika = LocalDate.now(ZoneId.systemDefault())
                }
                // Palautettu korjattavaksi
                else {
                    koulutussopimus.korjausehdotus = updatedKoulutussopimus.korjausehdotus
                    koulutussopimus.lahetetty = false
                }
            }
        }

        if (koulutussopimus.vastuuhenkilo?.user?.id == userId) {
            // Hyväksytty
            if (updatedKoulutussopimus.korjausehdotus.isNullOrBlank()) {
                koulutussopimus.vastuuhenkiloHyvaksynyt = true
                koulutussopimus.vastuuhenkilonKuittausaika = LocalDate.now(ZoneId.systemDefault())
            }
            // Palautettu korjattavaksi
            else {
                koulutussopimus.korjausehdotus = updatedKoulutussopimus.korjausehdotus
                koulutussopimus.lahetetty = false
            }
        }

        // Jos sopimus palautetaan, poistetaan kaikki aiemmat kuittaukset
        if (!koulutussopimus.lahetetty) {
            koulutussopimus.kouluttajat.forEach {
                it.sopimusHyvaksytty = false
                it.kuittausaika = null
            }
        }

        koulutussopimus = koejaksonKoulutussopimusRepository.save(koulutussopimus)

        // Sähköposti kouluttajille allekirjoitetusta sopimuksesta
        if (kirjautunutErikoistuvaLaakari != null && kirjautunutErikoistuvaLaakari == koulutussopimus.erikoistuvaLaakari && koulutussopimus.lahetetty) {
            koulutussopimus.kouluttajat.forEach {
                mailService.sendEmailFromTemplate(
                    kayttajaRepository.findById(it.kouluttaja?.id!!).get().user!!,
                    "koulutussopimusKouluttajalle.html",
                    "email.koulutussopimuskouluttajalle.title",
                    properties = mapOf(Pair(MailProperty.ID, koulutussopimus.id!!.toString()))
                )
            }
        } else if (sopimuksenKouluttaja) {

            // Sähköposti vastuuhenkilölle hyväksytystä sopimuksesta
            if (koulutussopimus.kouluttajat.all { it.sopimusHyvaksytty }) {
                mailService.sendEmailFromTemplate(
                    kayttajaRepository.findById(koulutussopimus.vastuuhenkilo?.id!!).get().user!!,
                    "koulutussopimusVastuuhenkilolle.html",
                    "email.koulutussopimusvastuuhenkilolle.title",
                    properties = mapOf(Pair(MailProperty.ID, koulutussopimus.id!!.toString()))
                )
            }

            // Sähköposti erikoistuvalle ja toiselle kouluttajalle palautetusta sopimuksesta
            else if (koulutussopimus.korjausehdotus != null) {
                val erikoistuvaLaakari =
                    kayttajaRepository.findById(koulutussopimus.erikoistuvaLaakari?.kayttaja?.id!!)
                        .get().user!!
                mailService.sendEmailFromTemplate(
                    erikoistuvaLaakari,
                    "koulutussopimusPalautettu.html",
                    "email.koulutussopimuspalautettu.title",
                    properties = mapOf(Pair(MailProperty.ID, koulutussopimus.id!!.toString()))
                )

                koulutussopimus.kouluttajat.forEach {
                    if (it.kouluttaja?.user?.id != userId) {
                        mailService.sendEmailFromTemplate(
                            kayttajaRepository.findById(it.kouluttaja?.id!!).get().user!!,
                            "koulutussopimusPalautettuKouluttaja.html",
                            "email.koulutussopimuspalautettu.title",
                            properties = mapOf(
                                Pair(
                                    MailProperty.NAME,
                                    erikoistuvaLaakari.getName()
                                ),
                                Pair(MailProperty.TEXT, koulutussopimus.korjausehdotus!!)
                            )
                        )
                    }
                }
            }
        } else if (koulutussopimus.vastuuhenkilo?.user?.id == userId) {

            // Sähköposti erikoistujalle, kouluttajille ja opintohallinnolle hyväksytystä sopimuksesta
            if (koulutussopimus.vastuuhenkiloHyvaksynyt) {
                val erikoistuvaLaakari =
                    kayttajaRepository.findById(koulutussopimus.erikoistuvaLaakari?.kayttaja?.id!!)
                        .get().user!!
                mailService.sendEmailFromTemplate(
                    erikoistuvaLaakari,
                    "koulutussopimusHyvaksytty.html",
                    "email.koulutussopimushyvaksytty.title",
                    properties = mapOf(Pair(MailProperty.ID, koulutussopimus.id!!.toString()))
                )

                koulutussopimus.kouluttajat.forEach {
                    mailService.sendEmailFromTemplate(
                        kayttajaRepository.findById(it.kouluttaja?.id!!).get().user!!,
                        "koulutussopimusHyvaksyttyKouluttaja.html",
                        "email.koulutussopimushyvaksytty.title",
                        properties = mapOf(
                            Pair(MailProperty.ID, koulutussopimus.id!!.toString()),
                            Pair(MailProperty.NAME, erikoistuvaLaakari.getName())
                        )
                    )
                }

                // TODO: Opintohallinto
            }
            // Sähköposti erikoistujalle ja kouluttajille palautetusta sopimuksesta
            else {
                val erikoistuvaLaakari =
                    kayttajaRepository.findById(koulutussopimus.erikoistuvaLaakari?.kayttaja?.id!!)
                        .get().user!!
                mailService.sendEmailFromTemplate(
                    erikoistuvaLaakari,
                    "koulutussopimusPalautettu.html",
                    "email.koulutussopimuspalautettu.title",
                    properties = mapOf(Pair(MailProperty.ID, koulutussopimus.id!!.toString()))
                )

                koulutussopimus.kouluttajat.forEach {
                    mailService.sendEmailFromTemplate(
                        kayttajaRepository.findById(it.kouluttaja?.id!!).get().user!!,
                        "koulutussopimusPalautettuKouluttaja.html",
                        "email.koulutussopimuspalautettu.title",
                        properties = mapOf(
                            Pair(MailProperty.NAME, erikoistuvaLaakari.getName()),
                            Pair(MailProperty.TEXT, koulutussopimus.korjausehdotus!!)
                        )
                    )
                }
            }
        }

        return koejaksonKoulutussopimusMapper.toDto(koulutussopimus)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<KoejaksonKoulutussopimusDTO> {
        return koejaksonKoulutussopimusRepository.findById(id)
            .map(koejaksonKoulutussopimusMapper::toDto)
    }

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
