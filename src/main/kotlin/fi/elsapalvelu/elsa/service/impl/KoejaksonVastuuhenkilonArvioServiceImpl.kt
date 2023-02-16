package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.domain.enumeration.VastuuhenkilonTehtavatyyppiEnum
import fi.elsapalvelu.elsa.repository.*
import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.dto.KoejaksonVastuuhenkilonArvioDTO
import fi.elsapalvelu.elsa.service.dto.TyoskentelyjaksotTableDTO
import fi.elsapalvelu.elsa.service.dto.enumeration.KoejaksoTila
import fi.elsapalvelu.elsa.service.dto.sarakesign.SarakeSignRecipientDTO
import fi.elsapalvelu.elsa.service.dto.sarakesign.SarakeSignRecipientFieldsDTO
import fi.elsapalvelu.elsa.service.mapper.KoejaksonVastuuhenkilonArvioMapper
import org.hibernate.engine.jdbc.BlobProxy
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticatedPrincipal
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.thymeleaf.context.Context
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import javax.persistence.EntityNotFoundException


@Service
@Transactional
class KoejaksonVastuuhenkilonArvioServiceImpl(
    private val koejaksonVastuuhenkilonArvioRepository: KoejaksonVastuuhenkilonArvioRepository,
    private val koejaksonVastuuhenkilonArvioMapper: KoejaksonVastuuhenkilonArvioMapper,
    private val mailService: MailService,
    private val kayttajaRepository: KayttajaRepository,
    private val userRepository: UserRepository,
    private val kayttajaService: KayttajaService,
    private val opintooikeusRepository: OpintooikeusRepository,
    private val tyoskentelyjaksoService: TyoskentelyjaksoService,
    private val koejaksonAloituskeskusteluService: KoejaksonAloituskeskusteluService,
    private val koejaksonValiarviointiService: KoejaksonValiarviointiService,
    private val koejaksonKehittamistoimenpiteetService: KoejaksonKehittamistoimenpiteetService,
    private val koejaksonLoppukeskusteluService: KoejaksonLoppukeskusteluService,
    private val opintooikeusService: OpintooikeusService,
    private val koulutussopimusRepository: KoejaksonKoulutussopimusRepository,
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository,
    private val asiakirjaRepository: AsiakirjaRepository,
    private val sarakesignService: SarakesignService,
    private val keskeytysaikaService: KeskeytysaikaService,
    private val pdfService: PdfService,
    private val applicationProperties: ApplicationProperties
) : KoejaksonVastuuhenkilonArvioService {

    override fun create(
        koejaksonVastuuhenkilonArvioDTO: KoejaksonVastuuhenkilonArvioDTO,
        opintooikeusId: Long
    ): KoejaksonVastuuhenkilonArvioDTO? {
        return opintooikeusRepository.findByIdOrNull(opintooikeusId)?.let {
            validateVastuuhenkilo(
                it.erikoistuvaLaakari?.kayttaja?.user?.id!!,
                koejaksonVastuuhenkilonArvioDTO
            )
            var vastuuhenkilonArvio =
                koejaksonVastuuhenkilonArvioMapper.toEntity(koejaksonVastuuhenkilonArvioDTO)
            vastuuhenkilonArvio.opintooikeus = it
            vastuuhenkilonArvio.virkailija = null
            vastuuhenkilonArvio.erikoistuvanKuittausaika = LocalDate.now()
            vastuuhenkilonArvio = koejaksonVastuuhenkilonArvioRepository.save(vastuuhenkilonArvio)

            it.erikoistuvaLaakari?.kayttaja?.user?.let { user ->
                user.email = koejaksonVastuuhenkilonArvioDTO.erikoistuvanSahkoposti
                user.phoneNumber = koejaksonVastuuhenkilonArvioDTO.erikoistuvanPuhelinnumero
                userRepository.save(user)
            }

            koejaksonVastuuhenkilonArvioMapper.toDto(vastuuhenkilonArvio)
        }
    }

    private fun validateVastuuhenkilo(
        userId: String,
        vastuuhenkilonArvioDTO: KoejaksonVastuuhenkilonArvioDTO
    ) {
        if (kayttajaService.findVastuuhenkiloByYliopistoErikoisalaAndTehtavatyyppi(
                userId,
                VastuuhenkilonTehtavatyyppiEnum.KOEJAKSOSOPIMUSTEN_JA_KOEJAKSOJEN_HYVAKSYMINEN
            ).id != vastuuhenkilonArvioDTO.vastuuhenkilo?.id
        ) {
            throw java.lang.IllegalArgumentException("Vastuuhenkilöä ei saa vaihtaa")
        }
    }

    override fun update(
        koejaksonVastuuhenkilonArvioDTO: KoejaksonVastuuhenkilonArvioDTO,
        userId: String
    ): KoejaksonVastuuhenkilonArvioDTO {
        val vastuuhenkilonArvio =
            koejaksonVastuuhenkilonArvioRepository.findById(koejaksonVastuuhenkilonArvioDTO.id!!)
                .orElseThrow { EntityNotFoundException("Vastuuhenkilön arviota ei löydy") }

        val updatedVastuuhenkilonArvio =
            koejaksonVastuuhenkilonArvioMapper.toEntity(koejaksonVastuuhenkilonArvioDTO)

        val kirjautunutErikoistuvaLaakari =
            erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)

        if (kirjautunutErikoistuvaLaakari != null
            && kirjautunutErikoistuvaLaakari == vastuuhenkilonArvio.opintooikeus?.erikoistuvaLaakari
        ) {
            handleErikoistuja(
                vastuuhenkilonArvio,
                koejaksonVastuuhenkilonArvioDTO.erikoistuvanSahkoposti,
                koejaksonVastuuhenkilonArvioDTO.erikoistuvanPuhelinnumero
            )
        } else if (vastuuhenkilonArvio.vastuuhenkilo?.user?.id == userId) {
            handleVastuuhenkilo(
                vastuuhenkilonArvio,
                updatedVastuuhenkilonArvio,
                koejaksonVastuuhenkilonArvioDTO.vastuuhenkilonSahkoposti,
                koejaksonVastuuhenkilonArvioDTO.vastuuhenkilonPuhelinnumero
            )
        } else {
            kayttajaRepository.findOneByUserId(userId).orElse(null)?.let { k ->
                k.yliopistot.firstOrNull()?.let { yliopisto ->
                    if (yliopisto.id == vastuuhenkilonArvio.opintooikeus?.yliopisto?.id) {
                        handleVirkailija(vastuuhenkilonArvio, updatedVastuuhenkilonArvio, k)
                    }
                }
            }
        }

        return koejaksonVastuuhenkilonArvioMapper.toDto(vastuuhenkilonArvio)
    }

    private fun handleErikoistuja(
        vastuuhenkilonArvio: KoejaksonVastuuhenkilonArvio,
        email: String?,
        phoneNumber: String?
    ) {
        vastuuhenkilonArvio.korjausehdotus = null
        vastuuhenkilonArvio.erikoistuvanKuittausaika = LocalDate.now()

        koejaksonVastuuhenkilonArvioRepository.save(vastuuhenkilonArvio)

        vastuuhenkilonArvio.opintooikeus?.erikoistuvaLaakari?.kayttaja?.user?.let { user ->
            user.email = email
            user.phoneNumber = phoneNumber
            userRepository.save(user)
        }
    }

    private fun handleVirkailija(
        vastuuhenkilonArvio: KoejaksonVastuuhenkilonArvio,
        updated: KoejaksonVastuuhenkilonArvio,
        virkailija: Kayttaja
    ) {
        // Hyväksytty
        if (updated.korjausehdotus.isNullOrBlank()) {
            vastuuhenkilonArvio.virkailija = virkailija
            vastuuhenkilonArvio.virkailijaHyvaksynyt = true
            vastuuhenkilonArvio.virkailijanKuittausaika = LocalDate.now(ZoneId.systemDefault())
            vastuuhenkilonArvio.lisatiedotVirkailijalta = updated.lisatiedotVirkailijalta
        }
        // Palautettu korjattavaksi
        else {
            vastuuhenkilonArvio.korjausehdotus = updated.korjausehdotus
            vastuuhenkilonArvio.virkailijaHyvaksynyt = false
            vastuuhenkilonArvio.virkailijanKuittausaika = null
            vastuuhenkilonArvio.erikoistuvanKuittausaika = null
        }

        koejaksonVastuuhenkilonArvioRepository.save(vastuuhenkilonArvio)

        // Sähköposti vastuuhenkilölle
        if (vastuuhenkilonArvio.virkailijanKuittausaika != null) {
            mailService.sendEmailFromTemplate(
                kayttajaRepository.findById(vastuuhenkilonArvio.vastuuhenkilo?.id!!).get().user!!,
                templateName = "vastuuhenkilonArvioKuitattava.html",
                titleKey = "email.vastuuhenkilonarviokuitattava.title",
                properties = mapOf(Pair(MailProperty.ID, vastuuhenkilonArvio.id!!.toString()))
            )
        }
    }

    private fun handleVastuuhenkilo(
        vastuuhenkilonArvio: KoejaksonVastuuhenkilonArvio,
        updated: KoejaksonVastuuhenkilonArvio,
        email: String?,
        phoneNumber: String?
    ) {
        vastuuhenkilonArvio.apply {
            vastuuhenkiloHyvaksynyt = true
            vastuuhenkilonKuittausaika = LocalDate.now(ZoneId.systemDefault())
            koejaksoHyvaksytty = updated.koejaksoHyvaksytty
        }.takeIf { it.koejaksoHyvaksytty == false }?.apply {
            perusteluHylkaamiselle = updated.perusteluHylkaamiselle
            hylattyArviointiKaytyLapiKeskustellen = updated.hylattyArviointiKaytyLapiKeskustellen
        }

        val result = koejaksonVastuuhenkilonArvioRepository.save(vastuuhenkilonArvio)

        result.vastuuhenkilo?.user?.let { user ->
            user.email = email
            user.phoneNumber = phoneNumber
            userRepository.save(user)
        }

        if (vastuuhenkilonArvio.vastuuhenkilonKuittausaika != null) {
            // Sähköposti erikoistuvalle jos koejakso hylätty. Hyväksytystä koejaksosta tulee allekirjoituspyyntö.
            if (vastuuhenkilonArvio.koejaksoHyvaksytty == false) {
                mailService.sendEmailFromTemplate(
                    kayttajaRepository.findById(vastuuhenkilonArvio.opintooikeus?.erikoistuvaLaakari?.kayttaja?.id!!)
                        .get().user!!,
                    templateName = "vastuuhenkilonArvioHylatty.html",
                    titleKey = "email.vastuuhenkilonarviohylatty.title",
                    properties = mapOf(Pair(MailProperty.ID, vastuuhenkilonArvio.id!!.toString()))
                )
            }
            luoPdf(mapVastuuhenkilonArvio(result), result)
        }
    }

    private fun luoPdf(
        vastuuhenkilonArvioDTO: KoejaksonVastuuhenkilonArvioDTO,
        vastuuhenkilonArvio: KoejaksonVastuuhenkilonArvio
    ) {
        val locale = Locale.forLanguageTag("fi")
        val context = Context(locale).apply {
            setVariable("arvio", vastuuhenkilonArvioDTO)
            setVariable(
                "yhteenlaskettuKesto",
                Period.between(
                    vastuuhenkilonArvioDTO.aloituskeskustelu?.koejaksonAlkamispaiva,
                    vastuuhenkilonArvioDTO.aloituskeskustelu?.koejaksonPaattymispaiva
                )
            )
            setVariable(
                "erikoistuvanSyntymaaika",
                vastuuhenkilonArvio.opintooikeus?.erikoistuvaLaakari?.syntymaaika
            )
            setVariable(
                "opintooikeudenPaattymispaiva",
                vastuuhenkilonArvio.opintooikeus?.opintooikeudenPaattymispaiva
            )
        }
        val outputStream = ByteArrayOutputStream()
        pdfService.luoPdf("pdf/vastuuhenkilonarvio.html", context, outputStream)
        val timestamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))

        val asiakirja = asiakirjaRepository.save(
            Asiakirja(
                opintooikeus = vastuuhenkilonArvio.opintooikeus,
                nimi = "koejakson_vastuuhenkilon_arvio_${timestamp}.pdf",
                tyyppi = MediaType.APPLICATION_PDF_VALUE,
                lisattypvm = LocalDateTime.now(),
                asiakirjaData = AsiakirjaData(data = BlobProxy.generateProxy(outputStream.toByteArray()))
            )
        )

        if (!sarakesignService.getApiUrl(vastuuhenkilonArvio.opintooikeus?.yliopisto?.nimi!!)
                .isNullOrBlank()
        ) {
            lahetaAllekirjoitettavaksi(vastuuhenkilonArvio, asiakirja)
        }

    }

    private fun lahetaAllekirjoitettavaksi(
        vastuuhenkilonArvio: KoejaksonVastuuhenkilonArvio,
        asiakirja: Asiakirja
    ) {
        val recipients: MutableList<SarakeSignRecipientDTO> = mutableListOf()
        vastuuhenkilonArvio.vastuuhenkilo?.user?.let { recipients.add(lisaaVastaanottaja(it, false)) }
        vastuuhenkilonArvio.opintooikeus?.erikoistuvaLaakari?.kayttaja?.user?.let { recipients.add(lisaaVastaanottaja(it, true)) }
        recipients.add(
            SarakeSignRecipientDTO(
                phaseNumber = 0,
                recipient = vastuuhenkilonArvio.opintooikeus?.yliopisto?.nimi?.getOpintohallintoEmailAddress(
                    applicationProperties
                ),
                readonly = true
            )
        )

        vastuuhenkilonArvio.sarakeSignRequestId = sarakesignService.lahetaAllekirjoitettavaksi(
            "Koejakson vastuuhenkilön arvio - " + vastuuhenkilonArvio.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi(),
            recipients,
            asiakirja.id!!,
            vastuuhenkilonArvio.opintooikeus?.yliopisto?.nimi!!
        )
        koejaksonVastuuhenkilonArvioRepository.save(vastuuhenkilonArvio)
    }

    private fun lisaaVastaanottaja(user: User, readonly: Boolean): SarakeSignRecipientDTO {
        return SarakeSignRecipientDTO(
            phaseNumber = 0,
            recipient = user.email,
            readonly = readonly,
            fields = SarakeSignRecipientFieldsDTO(
                firstName = user.firstName,
                lastName = user.lastName,
                phoneNumber = getPhoneNumber(user.phoneNumber),
            )
        )
    }

    private fun getPhoneNumber(number: String?): String? {
        if (number?.startsWith("0") == true) {
            return number.replaceFirst("0", "+358")
        }
        return number
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<KoejaksonVastuuhenkilonArvioDTO> {
        return koejaksonVastuuhenkilonArvioRepository.findById(id)
            .map(this::mapVastuuhenkilonArvio)
    }

    @Transactional(readOnly = true)
    override fun findByOpintooikeusId(opintooikeusId: Long): Optional<KoejaksonVastuuhenkilonArvioDTO> {
        val arvio = koejaksonVastuuhenkilonArvioRepository.findByOpintooikeusId(opintooikeusId)
        arvio.ifPresent { tarkistaAllekirjoitus(it) }
        return arvio.map(this::mapVastuuhenkilonArvio)
    }

    @Transactional(readOnly = true)
    override fun findOneByIdAndVastuuhenkiloUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonVastuuhenkilonArvioDTO> {
        val arvio =
            koejaksonVastuuhenkilonArvioRepository.findOneByIdAndVastuuhenkiloUserId(id, userId)
        arvio.ifPresent { tarkistaAllekirjoitus(it) }
        return arvio.map(this::mapVastuuhenkilonArvio)
    }

    override fun existsByIdAndVastuuhenkiloUserId(
        id: Long,
        userId: String
    ): Boolean {
        return koejaksonVastuuhenkilonArvioRepository.existsByIdAndVastuuhenkiloUserId(id, userId)
    }

    override fun findOneByIdAndVirkailijaUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonVastuuhenkilonArvioDTO> {
        kayttajaRepository.findOneByUserId(userId).orElse(null)?.let { k ->
            k.yliopistot.firstOrNull()?.let { yliopisto ->
                val arvio =
                    koejaksonVastuuhenkilonArvioRepository.findOneByIdAndOpintooikeusYliopistoId(
                        id,
                        yliopisto.id!!
                    )
                arvio.ifPresent { tarkistaAllekirjoitus(it) }
                return arvio.map(this::mapVastuuhenkilonArvio)
            }
        }
        return Optional.empty()
    }

    override fun delete(id: Long) {
        koejaksonVastuuhenkilonArvioRepository.deleteById(id)
    }

    override fun tarkistaAllekirjoitus(koejaksonVastuuhenkilonArvio: KoejaksonVastuuhenkilonArvio) {
        val yliopisto = koejaksonVastuuhenkilonArvio.opintooikeus?.yliopisto?.nimi!!
        if (koejaksonVastuuhenkilonArvio.vastuuhenkiloHyvaksynyt
            && !koejaksonVastuuhenkilonArvio.allekirjoitettu
            && !sarakesignService.getApiUrl(yliopisto).isNullOrBlank()
            && koejaksonVastuuhenkilonArvio.sarakeSignRequestId != null
        ) {
            val status =
                sarakesignService.tarkistaAllekirjoitus(
                    koejaksonVastuuhenkilonArvio.sarakeSignRequestId,
                    yliopisto
                )?.status

            if (status == 3) { // Completed
                koejaksonVastuuhenkilonArvio.allekirjoitettu = true
            } else if (status == 4) { // Aborted
                koejaksonVastuuhenkilonArvio.korjausehdotus = "Allekirjoitus keskeytetty"
                koejaksonVastuuhenkilonArvio.erikoistuvanKuittausaika = null
                koejaksonVastuuhenkilonArvio.vastuuhenkiloHyvaksynyt = false
                koejaksonVastuuhenkilonArvio.vastuuhenkilonKuittausaika = null
                koejaksonVastuuhenkilonArvio.virkailijaHyvaksynyt = false
                koejaksonVastuuhenkilonArvio.virkailijanKuittausaika = null
            }
            koejaksonVastuuhenkilonArvioRepository.save(koejaksonVastuuhenkilonArvio)
        }
    }

    private fun mapVastuuhenkilonArvio(vastuuhenkilonArvio: KoejaksonVastuuhenkilonArvio): KoejaksonVastuuhenkilonArvioDTO {
        val result = koejaksonVastuuhenkilonArvioMapper.toDto(vastuuhenkilonArvio)
        val opintoOikeusId = vastuuhenkilonArvio.opintooikeus?.id!!
        val principal =
            SecurityContextHolder.getContext().authentication.principal as AuthenticatedPrincipal
        result.koejaksonSuorituspaikat = TyoskentelyjaksotTableDTO(
            tyoskentelyjaksot = tyoskentelyjaksoService.findAllByOpintooikeusIdForKoejakso(
                opintoOikeusId
            )
                .toMutableSet(),
            keskeytykset = keskeytysaikaService.findAllByTyoskentelyjaksoOpintooikeusId(
                opintoOikeusId
            ).toMutableSet()
        )
        result.aloituskeskustelu =
            koejaksonAloituskeskusteluService.findByOpintooikeusId(opintoOikeusId)
                .orElse(null)
        result.valiarviointi =
            koejaksonValiarviointiService.findByOpintooikeusId(opintoOikeusId).orElse(null)
        result.kehittamistoimenpiteet =
            koejaksonKehittamistoimenpiteetService.findByOpintooikeusId(opintoOikeusId).orElse(null)
        result.loppukeskustelu =
            koejaksonLoppukeskusteluService.findByOpintooikeusId(opintoOikeusId).orElse(null)
        result.muutOpintooikeudet =
            opintooikeusService.findAllValidByErikoistuvaLaakariKayttajaUserId(vastuuhenkilonArvio.opintooikeus?.erikoistuvaLaakari?.kayttaja?.user?.id!!)
                .filter { it.id != opintoOikeusId }
        val koulutussopimus = koulutussopimusRepository.findByOpintooikeusId(opintoOikeusId)
        result.koulutussopimusHyvaksytty =
            koulutussopimus.isPresent && koulutussopimus.get().vastuuhenkiloHyvaksynyt == true
        result.tila = KoejaksoTila.fromVastuuhenkilonArvio(vastuuhenkilonArvio, principal.name)
        return result
    }
}
