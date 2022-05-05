package fi.elsapalvelu.elsa.service.impl

import com.itextpdf.html2pdf.HtmlConverter
import com.itextpdf.kernel.pdf.PdfWriter
import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.repository.*
import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.dto.KoejaksonKoulutussopimusDTO
import fi.elsapalvelu.elsa.service.dto.sarakesign.SarakeSignRecipientDTO
import fi.elsapalvelu.elsa.service.dto.sarakesign.SarakeSignRecipientFieldsDTO
import fi.elsapalvelu.elsa.service.mapper.KoejaksonKoulutussopimusMapper
import org.hibernate.engine.jdbc.BlobProxy
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
    private val userRepository: UserRepository,
    private val templateEngine: SpringTemplateEngine,
    private val asiakirjaRepository: AsiakirjaRepository,
    private val sarakesignService: SarakesignService,
    private val kouluttajavaltuutusService: KouluttajavaltuutusService
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
                user.email = kouluttaja?.sahkoposti
                kayttaja.nimike = kouluttaja?.nimike
                userRepository.save(user)
                kayttajaRepository.save(kayttaja)
            }
        }

        if (koulutussopimus.vastuuhenkilo?.user?.id == userId) {
            koulutussopimus = handleVastuuhenkilo(koulutussopimus, updatedKoulutussopimus)

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
                templateName = "koulutussopimusVastuuhenkilolle.html",
                titleKey = "email.koulutussopimusvastuuhenkilolle.title",
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
                templateName = "koulutussopimusHyvaksytty.html",
                titleKey = "email.koulutussopimushyvaksytty.title",
                properties = mapOf(Pair(MailProperty.ID, result.id!!.toString()))
            )

            result.kouluttajat?.forEach {
                mailService.sendEmailFromTemplate(
                    kayttajaRepository.findById(it.kouluttaja?.id!!).get().user!!,
                    templateName = "koulutussopimusHyvaksyttyKouluttaja.html",
                    titleKey = "email.koulutussopimushyvaksytty.title",
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
        val koulutussopimus =
            koejaksonKoulutussopimusRepository.findByOpintooikeusId(opintooikeusId)
        koulutussopimus.ifPresent { tarkistaAllekirjoitus(it) }
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
        koulutussopimus.ifPresent { tarkistaAllekirjoitus(it) }

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
        val koulutussopimus = koejaksonKoulutussopimusRepository.findOneByIdAndVastuuhenkiloUserId(
            id,
            userId
        )
        koulutussopimus.ifPresent { tarkistaAllekirjoitus(it) }
        return koulutussopimus.map(koejaksonKoulutussopimusMapper::toDto)
    }

    override fun delete(id: Long) {
        koejaksonKoulutussopimusRepository.deleteById(id)
    }

    override fun tarkistaAllekirjoitus(koejaksonKoulutussopimus: KoejaksonKoulutussopimus) {
        val yliopisto = koejaksonKoulutussopimus.opintooikeus?.yliopisto?.nimi!!
        if (koejaksonKoulutussopimus.vastuuhenkiloHyvaksynyt
            && !koejaksonKoulutussopimus.allekirjoitettu
            && !sarakesignService.getApiUrl(yliopisto).isNullOrBlank()
            && koejaksonKoulutussopimus.sarakeSignRequestId != null
        ) {
            val status =
                sarakesignService.tarkistaAllekirjoitus(
                    koejaksonKoulutussopimus.sarakeSignRequestId,
                    yliopisto
                )

            if (status == 3) { // Completed
                koejaksonKoulutussopimus.allekirjoitettu = true
            } else if (status == 4) { // Aborted
                koejaksonKoulutussopimus.korjausehdotus = "Allekirjoitus keskeytetty"
                koejaksonKoulutussopimus.lahetetty = false
                koejaksonKoulutussopimus.erikoistuvanAllekirjoitusaika = null
                koejaksonKoulutussopimus.vastuuhenkiloHyvaksynyt = false
                koejaksonKoulutussopimus.vastuuhenkilonKuittausaika = null

                koejaksonKoulutussopimus.kouluttajat?.forEach {
                    it.sopimusHyvaksytty = false
                    it.kuittausaika = null
                }
            }
            koejaksonKoulutussopimusRepository.save(koejaksonKoulutussopimus)
        }
    }

    private fun luoPdf(
        koulutussopimusDTO: KoejaksonKoulutussopimusDTO,
        koulutussopimus: KoejaksonKoulutussopimus
    ) {
        val locale = Locale.forLanguageTag("fi")
        val context = Context(locale).apply {
            setVariable("sopimus", koulutussopimusDTO)
        }
        val content = templateEngine.process("pdf/koulutussopimus.html", context)
        val outputStream = ByteArrayOutputStream()
        val timestamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        HtmlConverter.convertToPdf(content, PdfWriter(outputStream))
        val asiakirja = asiakirjaRepository.save(
            Asiakirja(
                opintooikeus = koulutussopimus.opintooikeus,
                nimi = "koejakson_koulutussopimus_${timestamp}.pdf",
                tyyppi = "application/pdf",
                lisattypvm = LocalDateTime.now(),
                asiakirjaData = AsiakirjaData(data = BlobProxy.generateProxy(outputStream.toByteArray()))
            )
        )

        if (!sarakesignService.getApiUrl(koulutussopimus.opintooikeus?.yliopisto?.nimi!!)
                .isNullOrBlank()
        ) {
            lahetaAllekirjoitettavaksi(koulutussopimus, asiakirja)
        }

    }

    private fun lahetaAllekirjoitettavaksi(
        koulutussopimus: KoejaksonKoulutussopimus,
        asiakirja: Asiakirja
    ) {
        val recipients: MutableList<SarakeSignRecipientDTO> = mutableListOf()
        koulutussopimus.opintooikeus?.erikoistuvaLaakari?.kayttaja?.user?.let {
            recipients.add(
                lisaaVastaanottaja(
                    it
                )
            )
        }
        koulutussopimus.kouluttajat?.forEach {
            it.kouluttaja?.user?.let { user ->
                recipients.add(
                    lisaaVastaanottaja(user)
                )
            }
        }
        koulutussopimus.vastuuhenkilo?.user?.let { recipients.add(lisaaVastaanottaja(it)) }

        koulutussopimus.sarakeSignRequestId = sarakesignService.lahetaAllekirjoitettavaksi(
            "Koejakson koulutussopimus - " + koulutussopimus.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi(),
            recipients,
            asiakirja.id!!,
            koulutussopimus.opintooikeus?.yliopisto?.nimi!!
        )
        koejaksonKoulutussopimusRepository.save(koulutussopimus)
    }

    private fun lisaaVastaanottaja(user: User): SarakeSignRecipientDTO {
        return SarakeSignRecipientDTO(
            phaseNumber = 0,
            recipient = user.email,
            fields = SarakeSignRecipientFieldsDTO(
                firstName = user.firstName,
                lastName = user.lastName,
                phoneNumber = user.phoneNumber
            )
        )
    }
}
