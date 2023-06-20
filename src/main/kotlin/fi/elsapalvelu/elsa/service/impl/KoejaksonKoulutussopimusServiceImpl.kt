package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.domain.enumeration.VastuuhenkilonTehtavatyyppiEnum
import fi.elsapalvelu.elsa.repository.*
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.dto.KoejaksonKoulutussopimusDTO
import fi.elsapalvelu.elsa.service.mapper.KoejaksonKoulutussopimusMapper
import org.hibernate.engine.jdbc.BlobProxy
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.thymeleaf.context.Context
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import jakarta.persistence.EntityNotFoundException


@Service
@Transactional
class KoejaksonKoulutussopimusServiceImpl(
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository,
    private val koejaksonKoulutussopimusMapper: KoejaksonKoulutussopimusMapper,
    private val koejaksonKoulutussopimusRepository: KoejaksonKoulutussopimusRepository,
    private val mailService: MailService,
    private val kayttajaRepository: KayttajaRepository,
    private val opintooikeusRepository: OpintooikeusRepository,
    private val userRepository: UserRepository,
    private val asiakirjaRepository: AsiakirjaRepository,
    private val kouluttajavaltuutusService: KouluttajavaltuutusService,
    private val pdfService: PdfService
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
            koulutussopimus.korjausehdotus = null
            koulutussopimus.vastuuhenkilonKorjausehdotus = null
            koulutussopimus.vastuuhenkilo = null
            koulutussopimus = koejaksonKoulutussopimusRepository.save(koulutussopimus)

            it.erikoistuvaLaakari?.kayttaja?.user?.let { user ->
                user.email = koejaksonKoulutussopimusDTO.erikoistuvanSahkoposti
                user.phoneNumber = koejaksonKoulutussopimusDTO.erikoistuvanPuhelinnumero
                userRepository.save(user)
            }

            // Sähköposti kouluttajille lähetetystä sopimuksesta
            if (koulutussopimus.lahetetty) {
                koulutussopimus.kouluttajat?.forEach { kouluttaja ->
                    kouluttajavaltuutusService.lisaaValtuutus(
                        it.erikoistuvaLaakari?.kayttaja?.user?.id!!,
                        kouluttaja.kouluttaja?.id!!
                    )
                    mailService.sendEmailFromTemplate(
                        kayttajaRepository.findById(kouluttaja.kouluttaja?.id!!).get().user!!,
                        templateName = "koulutussopimusKouluttajalle.html",
                        titleKey = "email.koulutussopimuskouluttajalle.title",
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

            koulutussopimus.opintooikeus?.erikoistuvaLaakari?.kayttaja?.user?.let { user ->
                user.email = koejaksonKoulutussopimusDTO.erikoistuvanSahkoposti
                user.phoneNumber = koejaksonKoulutussopimusDTO.erikoistuvanPuhelinnumero
                userRepository.save(user)
            }
        }

        koulutussopimus.kouluttajat?.toTypedArray()?.forEach {
            if (it.kouluttaja?.user?.id == userId) {
                koulutussopimus = handleKouluttaja(koulutussopimus, it, updatedKoulutussopimus)

                val kayttaja = it.kouluttaja!!
                val user = it.kouluttaja?.user!!
                val kouluttaja =
                    koejaksonKoulutussopimusDTO.kouluttajat?.first { it.kayttajaUserId == userId }
                user.phoneNumber = kouluttaja?.puhelin
                user.email = kouluttaja?.sahkoposti?.lowercase()
                kayttaja.nimike = kouluttaja?.nimike
                userRepository.save(user)
                kayttajaRepository.save(kayttaja)
            }
        }

        val vastuuhenkilo =
            kayttajaRepository.findOneByAuthoritiesYliopistoErikoisalaAndVastuuhenkilonTehtavatyyppi(
                listOf(VASTUUHENKILO),
                koulutussopimus.opintooikeus?.yliopisto?.id,
                koulutussopimus.opintooikeus?.erikoisala?.id,
                VastuuhenkilonTehtavatyyppiEnum.KOEJAKSOSOPIMUSTEN_JA_KOEJAKSOJEN_HYVAKSYMINEN
            )
        if (vastuuhenkilo?.user?.id == userId) {
            koulutussopimus =
                handleVastuuhenkilo(koulutussopimus, updatedKoulutussopimus, vastuuhenkilo)

            koulutussopimus.vastuuhenkilo?.user?.let {
                it.phoneNumber = koejaksonKoulutussopimusDTO.vastuuhenkilo?.puhelin
                it.email = koejaksonKoulutussopimusDTO.vastuuhenkilo?.sahkoposti
                userRepository.save(it)
            }
        }

        koulutussopimus = koejaksonKoulutussopimusRepository.save(koulutussopimus)

        val dto = koejaksonKoulutussopimusMapper.toDto(koulutussopimus)
        if (dto.vastuuhenkilo?.sopimusHyvaksytty == true) {
            luoPdf(dto, koulutussopimus)
        }

        return dto
    }

    private fun handleErikoistuva(
        koulutussopimus: KoejaksonKoulutussopimus,
        updated: KoejaksonKoulutussopimus
    ): KoejaksonKoulutussopimus {
        koulutussopimus.apply {
            koejaksonAlkamispaiva = updated.koejaksonAlkamispaiva
            lahetetty = updated.lahetetty
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
                kouluttajavaltuutusService.lisaaValtuutus(
                    koulutussopimus.opintooikeus?.erikoistuvaLaakari?.kayttaja?.user?.id!!,
                    it.kouluttaja?.id!!
                )
                mailService.sendEmailFromTemplate(
                    kayttajaRepository.findById(it.kouluttaja?.id!!).get().user!!,
                    templateName = "koulutussopimusKouluttajalle.html",
                    titleKey = "email.koulutussopimuskouluttajalle.title",
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

            if (koulutussopimus.kouluttajat?.all { it.sopimusHyvaksytty } == true) {
                koulutussopimus.vastuuhenkilonKorjausehdotus = null
            }
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
            val vastuuhenkilo =
                kayttajaRepository.findOneByAuthoritiesYliopistoErikoisalaAndVastuuhenkilonTehtavatyyppi(
                    listOf(VASTUUHENKILO),
                    koulutussopimus.opintooikeus?.yliopisto?.id,
                    koulutussopimus.opintooikeus?.erikoisala?.id,
                    VastuuhenkilonTehtavatyyppiEnum.KOEJAKSOSOPIMUSTEN_JA_KOEJAKSOJEN_HYVAKSYMINEN
                )
            vastuuhenkilo?.user?.let {
                mailService.sendEmailFromTemplate(
                    it,
                    templateName = "koulutussopimusVastuuhenkilolle.html",
                    titleKey = "email.koulutussopimusvastuuhenkilolle.title",
                    properties = mapOf(Pair(MailProperty.ID, result.id!!.toString()))
                )
            }
        }

        // Sähköposti erikoistuvalle ja toiselle kouluttajalle palautetusta sopimuksesta
        else if (result.korjausehdotus != null) {
            val erikoistuvaLaakari =
                kayttajaRepository.findById(result.opintooikeus?.erikoistuvaLaakari?.kayttaja?.id!!)
                    .get().user!!
            mailService.sendEmailFromTemplate(
                erikoistuvaLaakari,
                templateName = "koulutussopimusPalautettu.html",
                titleKey = "email.koulutussopimuspalautettu.title",
                properties = mapOf(Pair(MailProperty.ID, result.id!!.toString()))
            )

            result.kouluttajat?.forEach {
                if (it.id != kouluttaja.id) {
                    mailService.sendEmailFromTemplate(
                        kayttajaRepository.findById(it.kouluttaja?.id!!).get().user!!,
                        templateName = "koulutussopimusPalautettuKouluttaja.html",
                        titleKey = "email.koulutussopimuspalautettu.title",
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
        updated: KoejaksonKoulutussopimus,
        vastuuhenkilo: Kayttaja
    ): KoejaksonKoulutussopimus {
        // Hyväksytty
        if (updated.korjausehdotus.isNullOrBlank()) {
            koulutussopimus.vastuuhenkiloHyvaksynyt = true
            koulutussopimus.vastuuhenkilonKuittausaika = LocalDate.now(ZoneId.systemDefault())
            koulutussopimus.vastuuhenkilo = vastuuhenkilo
        }
        // Palautettu korjattavaksi
        else {
            koulutussopimus.vastuuhenkilonKorjausehdotus = updated.korjausehdotus
            koulutussopimus.lahetetty = false
            koulutussopimus.erikoistuvanAllekirjoitusaika = null

            koulutussopimus.kouluttajat?.forEach {
                it.sopimusHyvaksytty = false
                it.kuittausaika = null
            }
        }

        val result = koejaksonKoulutussopimusRepository.save(koulutussopimus)

        // Sähköposti erikoistujalle hyväksytystä sopimuksesta
        if (result.vastuuhenkiloHyvaksynyt) {
            val erikoistuvaLaakari =
                kayttajaRepository.findById(result.opintooikeus?.erikoistuvaLaakari?.kayttaja?.id!!)
                    .get().user!!
            mailService.sendEmailFromTemplate(
                erikoistuvaLaakari,
                templateName = "koulutussopimusHyvaksytty.html",
                titleKey = "email.koulutussopimushyvaksytty.title",
                properties = mapOf(Pair(MailProperty.ID, result.id!!.toString()))
            )
        }
        // Sähköposti erikoistujalle ja kouluttajille palautetusta sopimuksesta
        else {
            val erikoistuvaLaakari =
                kayttajaRepository.findById(result.opintooikeus?.erikoistuvaLaakari?.kayttaja?.id!!)
                    .get().user!!
            mailService.sendEmailFromTemplate(
                erikoistuvaLaakari,
                templateName = "koulutussopimusPalautettu.html",
                titleKey = "email.koulutussopimuspalautettu.title",
                properties = mapOf(Pair(MailProperty.ID, result.id!!.toString()))
            )

            result.kouluttajat?.forEach {
                mailService.sendEmailFromTemplate(
                    kayttajaRepository.findById(it.kouluttaja?.id!!).get().user!!,
                    templateName = "koulutussopimusPalautettuKouluttaja.html",
                    titleKey = "email.koulutussopimuspalautettu.title",
                    properties = mapOf(
                        Pair(MailProperty.NAME, erikoistuvaLaakari.getName()),
                        Pair(MailProperty.TEXT, result.vastuuhenkilonKorjausehdotus!!)
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
        val koulutussopimus =
            koejaksonKoulutussopimusRepository.findByOpintooikeusId(opintooikeusId)
        return koulutussopimus.map(koejaksonKoulutussopimusMapper::toDto)
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
            )

        val koulutussopimusDTO = koulutussopimus.map(koejaksonKoulutussopimusMapper::toDto)
        val currentKayttaja = kayttajaRepository.findOneByUserId(userId).get()
        val currentKoulutussopimuksenKouluttaja = if (koulutussopimusDTO.isPresent)
            koulutussopimusDTO.get().kouluttajat?.find {
                it.kayttajaId == currentKayttaja.id
            } else null
        if (currentKoulutussopimuksenKouluttaja?.sahkoposti.isNullOrEmpty()) {
            currentKoulutussopimuksenKouluttaja?.sahkoposti = currentKayttaja.user?.email
        }
        return koulutussopimusDTO
    }

    @Transactional(readOnly = true)
    override fun findOneByIdAndVastuuhenkiloKayttajaUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonKoulutussopimusDTO> {
        val koulutussopimus = koejaksonKoulutussopimusRepository.findById(id).orElse(null)
            ?: return Optional.empty()
        val vastuuhenkilo =
            kayttajaRepository.findOneByAuthoritiesYliopistoErikoisalaAndVastuuhenkilonTehtavatyyppi(
                listOf(VASTUUHENKILO),
                koulutussopimus.opintooikeus?.yliopisto?.id,
                koulutussopimus.opintooikeus?.erikoisala?.id,
                VastuuhenkilonTehtavatyyppiEnum.KOEJAKSOSOPIMUSTEN_JA_KOEJAKSOJEN_HYVAKSYMINEN
            )
        if (vastuuhenkilo?.user?.id == userId) {
            return Optional.of(koulutussopimus).map(koejaksonKoulutussopimusMapper::toDto)
        }
        return Optional.empty()
    }

    override fun delete(id: Long) {
        koejaksonKoulutussopimusRepository.deleteById(id)
    }

    private fun luoPdf(
        koulutussopimusDTO: KoejaksonKoulutussopimusDTO,
        koulutussopimus: KoejaksonKoulutussopimus
    ) {
        val locale = Locale.forLanguageTag("fi")
        val context = Context(locale).apply {
            setVariable("sopimus", koulutussopimusDTO)
        }
        val outputStream = ByteArrayOutputStream()
        pdfService.luoPdf("pdf/koulutussopimus.html", context, outputStream)
        val timestamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))

        asiakirjaRepository.save(
            Asiakirja(
                opintooikeus = koulutussopimus.opintooikeus,
                nimi = "koejakson_koulutussopimus_${timestamp}.pdf",
                tyyppi = MediaType.APPLICATION_PDF_VALUE,
                lisattypvm = LocalDateTime.now(),
                asiakirjaData = AsiakirjaData(data = outputStream.toByteArray())
            )
        )
    }
}
