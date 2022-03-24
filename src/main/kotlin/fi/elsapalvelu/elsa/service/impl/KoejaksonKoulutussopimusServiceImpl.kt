package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.KoejaksonKoulutussopimus
import fi.elsapalvelu.elsa.domain.KoulutussopimuksenKouluttaja
import fi.elsapalvelu.elsa.repository.*
import fi.elsapalvelu.elsa.service.KoejaksonKoulutussopimusService
import fi.elsapalvelu.elsa.service.MailProperty
import fi.elsapalvelu.elsa.service.MailService
import fi.elsapalvelu.elsa.service.dto.KoejaksonKoulutussopimusDTO
import fi.elsapalvelu.elsa.service.mapper.KoejaksonKoulutussopimusMapper
import org.springframework.data.repository.findByIdOrNull
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
    private val opintooikeusRepository: OpintooikeusRepository,
    private val userRepository: UserRepository
) : KoejaksonKoulutussopimusService {

    override fun create(
        koejaksonKoulutussopimusDTO: KoejaksonKoulutussopimusDTO,
        opintooikeusId: Long
    ): KoejaksonKoulutussopimusDTO? {
        return opintooikeusRepository.findByIdOrNull(opintooikeusId)?.let {
            var koulutussopimus =
                koejaksonKoulutussopimusMapper.toEntity(koejaksonKoulutussopimusDTO)
            koulutussopimus.opintooikeus = it
            koulutussopimus.koulutuspaikat?.forEach { paikka ->
                paikka.koulutussopimus = koulutussopimus
                if (paikka.koulutussopimusOmanYliopistonKanssa == true) {
                    paikka.yliopisto = null
                }
            }
            koulutussopimus.kouluttajat?.forEach { it.koulutussopimus = koulutussopimus }
            if (koulutussopimus.lahetetty) koulutussopimus.erikoistuvanAllekirjoitusaika =
                LocalDate.now()
            koulutussopimus = koejaksonKoulutussopimusRepository.save(koulutussopimus)

            val user = it.erikoistuvaLaakari?.kayttaja?.user
            user?.email = koejaksonKoulutussopimusDTO.erikoistuvanSahkoposti
            user?.phoneNumber = koejaksonKoulutussopimusDTO.erikoistuvanPuhelinnumero
            userRepository.save(user)

            // Sähköposti kouluttajille allekirjoitetusta sopimuksesta
            if (koulutussopimus.lahetetty) {
                koulutussopimus.kouluttajat?.forEach {
                    mailService.sendEmailFromTemplate(
                        kayttajaRepository.findById(it.kouluttaja?.id!!).get().user!!,
                        "koulutussopimusKouluttajalle.html",
                        "email.koulutussopimuskouluttajalle.title",
                        properties = mapOf(Pair(MailProperty.ID, koulutussopimus.id!!.toString()))
                    )
                }
            }

            koejaksonKoulutussopimusMapper.toDto(koulutussopimus)
        }
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

        if (kirjautunutErikoistuvaLaakari != null
            && kirjautunutErikoistuvaLaakari == koulutussopimus.opintooikeus?.erikoistuvaLaakari
        ) {
            koulutussopimus = handleErikoistuva(koulutussopimus, updatedKoulutussopimus)

            val user = koulutussopimus.opintooikeus?.erikoistuvaLaakari?.kayttaja?.user
            user?.email = koejaksonKoulutussopimusDTO.erikoistuvanSahkoposti
            user?.phoneNumber = koejaksonKoulutussopimusDTO.erikoistuvanPuhelinnumero
            userRepository.save(user)
        }

        koulutussopimus.kouluttajat?.toTypedArray()?.forEach {
            if (it.kouluttaja?.user?.id == userId) {
                koulutussopimus = handleKouluttaja(koulutussopimus, it, updatedKoulutussopimus)

                val kayttaja = it.kouluttaja
                val user = it.kouluttaja?.user
                val kouluttaja =
                    koejaksonKoulutussopimusDTO.kouluttajat?.first { it.kayttajaUserId == userId }
                user?.phoneNumber = kouluttaja?.puhelin
                user?.email = kouluttaja?.sahkoposti
                kayttaja?.nimike = kouluttaja?.nimike
                userRepository.save(user)
                kayttajaRepository.save(kayttaja)
            }
        }

        if (koulutussopimus.vastuuhenkilo?.user?.id == userId) {
            koulutussopimus = handleVastuuhenkilo(koulutussopimus, updatedKoulutussopimus)

            val user = koulutussopimus.vastuuhenkilo?.user
            user?.phoneNumber = koejaksonKoulutussopimusDTO.vastuuhenkilo?.puhelin
            user?.email = koejaksonKoulutussopimusDTO.vastuuhenkilo?.sahkoposti
            userRepository.save(user)
        }

        koulutussopimus = koejaksonKoulutussopimusRepository.save(koulutussopimus)

        return koejaksonKoulutussopimusMapper.toDto(koulutussopimus)
    }

    private fun handleErikoistuva(
        koulutussopimus: KoejaksonKoulutussopimus,
        updated: KoejaksonKoulutussopimus
    ): KoejaksonKoulutussopimus {
        koulutussopimus.apply {
            koejaksonAlkamispaiva = updated.koejaksonAlkamispaiva
            lahetetty = updated.lahetetty
            vastuuhenkilo = updated.vastuuhenkilo
        }
        koulutussopimus.kouluttajat?.clear()
        updated.kouluttajat?.let { koulutussopimus.kouluttajat?.addAll(it) }
        koulutussopimus.kouluttajat?.forEach { it.koulutussopimus = koulutussopimus }
        koulutussopimus.koulutuspaikat?.clear()
        updated.koulutuspaikat?.let { koulutussopimus.koulutuspaikat?.addAll(it) }
        koulutussopimus.koulutuspaikat?.forEach {
            it.koulutussopimus = koulutussopimus
            if (it.koulutussopimusOmanYliopistonKanssa == true) {
                it.yliopisto = null
            }
        }

        if (updated.lahetetty) {
            koulutussopimus.korjausehdotus = null
            koulutussopimus.erikoistuvanAllekirjoitusaika = LocalDate.now()
        }

        val result = koejaksonKoulutussopimusRepository.save(koulutussopimus)

        // Sähköposti kouluttajille allekirjoitetusta sopimuksesta
        if (updated.lahetetty) {
            result.kouluttajat?.forEach {
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
        val updatedKouluttaja = updated.kouluttajat?.first { k -> k.id == kouluttaja.id }
        kouluttaja.toimipaikka = updatedKouluttaja?.toimipaikka
        kouluttaja.lahiosoite = updatedKouluttaja?.lahiosoite
        kouluttaja.postitoimipaikka = updatedKouluttaja?.postitoimipaikka

        // Hyväksytty
        if (updated.korjausehdotus.isNullOrBlank()) {
            kouluttaja.sopimusHyvaksytty = true
            kouluttaja.kuittausaika = LocalDate.now(ZoneId.systemDefault())
        }
        // Palautettu korjattavaksi
        else {
            koulutussopimus.korjausehdotus = updated.korjausehdotus
            koulutussopimus.lahetetty = false
            koulutussopimus.erikoistuvanAllekirjoitusaika = null

            koulutussopimus.kouluttajat?.forEach {
                it.sopimusHyvaksytty = false
                it.kuittausaika = null
            }
        }

        val result = koejaksonKoulutussopimusRepository.save(koulutussopimus)

        // Sähköposti vastuuhenkilölle hyväksytystä sopimuksesta
        if (result.kouluttajat?.all { it.sopimusHyvaksytty } == true) {
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
                kayttajaRepository.findById(result.opintooikeus?.erikoistuvaLaakari?.kayttaja?.id!!)
                    .get().user!!
            mailService.sendEmailFromTemplate(
                erikoistuvaLaakari,
                "koulutussopimusPalautettu.html",
                "email.koulutussopimuspalautettu.title",
                properties = mapOf(Pair(MailProperty.ID, result.id!!.toString()))
            )

            result.kouluttajat?.forEach {
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
            koulutussopimus.erikoistuvanAllekirjoitusaika = null

            koulutussopimus.kouluttajat?.forEach {
                it.sopimusHyvaksytty = false
                it.kuittausaika = null
            }
        }

        val result = koejaksonKoulutussopimusRepository.save(koulutussopimus)

        // Sähköposti erikoistujalle, kouluttajille ja opintohallinnolle hyväksytystä sopimuksesta
        if (result.vastuuhenkiloHyvaksynyt) {
            val erikoistuvaLaakari =
                kayttajaRepository.findById(result.opintooikeus?.erikoistuvaLaakari?.kayttaja?.id!!)
                    .get().user!!
            mailService.sendEmailFromTemplate(
                erikoistuvaLaakari,
                "koulutussopimusHyvaksytty.html",
                "email.koulutussopimushyvaksytty.title",
                properties = mapOf(Pair(MailProperty.ID, result.id!!.toString()))
            )

            result.kouluttajat?.forEach {
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
            // Sähköposti opintohallinnolle
        }
        // Sähköposti erikoistujalle ja kouluttajille palautetusta sopimuksesta
        else {
            val erikoistuvaLaakari =
                kayttajaRepository.findById(result.opintooikeus?.erikoistuvaLaakari?.kayttaja?.id!!)
                    .get().user!!
            mailService.sendEmailFromTemplate(
                erikoistuvaLaakari,
                "koulutussopimusPalautettu.html",
                "email.koulutussopimuspalautettu.title",
                properties = mapOf(Pair(MailProperty.ID, result.id!!.toString()))
            )

            result.kouluttajat?.forEach {
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
    override fun findByOpintooikeusId(opintooikeusId: Long): Optional<KoejaksonKoulutussopimusDTO> {
        return koejaksonKoulutussopimusRepository.findByOpintooikeusId(opintooikeusId)
            .map(koejaksonKoulutussopimusMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOneByIdAndKouluttajaKayttajaUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonKoulutussopimusDTO> {
        val koulutussopimus =
            koejaksonKoulutussopimusRepository.findOneByIdAndKouluttajatKouluttajaUserId(
                id,
                userId
            ).map(koejaksonKoulutussopimusMapper::toDto)

        val currentKayttaja = kayttajaRepository.findOneByUserId(userId).get()
        val currentKoulutussopimuksenKouluttaja = if (koulutussopimus.isPresent)
            koulutussopimus.get().kouluttajat?.find {
                it.kayttajaId == currentKayttaja.id
            } else null
        if (currentKoulutussopimuksenKouluttaja?.sahkoposti.isNullOrEmpty()) {
            currentKoulutussopimuksenKouluttaja?.sahkoposti = currentKayttaja.user?.email
        }
        return koulutussopimus
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
