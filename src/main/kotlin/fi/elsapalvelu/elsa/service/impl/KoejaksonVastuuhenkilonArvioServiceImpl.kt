package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.config.YEK_ERIKOISALA_ID
import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.domain.enumeration.VastuuhenkilonTehtavatyyppiEnum
import fi.elsapalvelu.elsa.repository.*
import fi.elsapalvelu.elsa.security.OPINTOHALLINNON_VIRKAILIJA
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.dto.AsiakirjaDTO
import fi.elsapalvelu.elsa.service.dto.KoejaksonVastuuhenkilonArvioDTO
import fi.elsapalvelu.elsa.service.dto.TyoskentelyjaksotTableDTO
import fi.elsapalvelu.elsa.service.dto.enumeration.KoejaksoTila
import fi.elsapalvelu.elsa.service.dto.sarakesign.SarakeSignRecipientDTO
import fi.elsapalvelu.elsa.service.dto.sarakesign.SarakeSignRecipientFieldsDTO
import fi.elsapalvelu.elsa.service.mapper.AsiakirjaMapper
import fi.elsapalvelu.elsa.service.mapper.KoejaksonVastuuhenkilonArvioMapper
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.MediaType
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
import jakarta.persistence.EntityNotFoundException
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication


@Service
@Transactional
class KoejaksonVastuuhenkilonArvioServiceImpl(
    private val koejaksonVastuuhenkilonArvioRepository: KoejaksonVastuuhenkilonArvioRepository,
    private val koejaksonVastuuhenkilonArvioMapper: KoejaksonVastuuhenkilonArvioMapper,
    private val mailService: MailService,
    private val kayttajaRepository: KayttajaRepository,
    private val userRepository: UserRepository,
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
    private val arkistointiService: ArkistointiService,
    private val keskeytysaikaService: KeskeytysaikaService,
    private val pdfService: PdfService,
    private val applicationProperties: ApplicationProperties,
    private val asiakirjaMapper: AsiakirjaMapper
) : KoejaksonVastuuhenkilonArvioService {

    override fun create(
        koejaksonVastuuhenkilonArvioDTO: KoejaksonVastuuhenkilonArvioDTO,
        opintooikeusId: Long,
        asiakirjat: Set<AsiakirjaDTO>?
    ): KoejaksonVastuuhenkilonArvioDTO? {
        return opintooikeusRepository.findByIdOrNull(opintooikeusId)?.let {
            var vastuuhenkilonArvio =
                koejaksonVastuuhenkilonArvioMapper.toEntity(koejaksonVastuuhenkilonArvioDTO)
            vastuuhenkilonArvio.opintooikeus = it
            vastuuhenkilonArvio.virkailija = null
            vastuuhenkilonArvio.vastuuhenkilo = null
            vastuuhenkilonArvio.erikoistuvanKuittausaika = LocalDate.now()

            asiakirjat?.let { asiakirja ->
                val asiakirjaEntities = asiakirja.map { asiakirjaDTO ->
                    asiakirjaMapper.toEntity(asiakirjaDTO).apply {
                        this.lisattypvm = LocalDateTime.now()
                        this.opintooikeus = it
                        this.koejaksonVastuuhenkilonArvio = vastuuhenkilonArvio
                        this.asiakirjaData?.data = asiakirjaDTO.asiakirjaData?.fileInputStream?.readAllBytes()
                    }
                }

                vastuuhenkilonArvio.asiakirjat.addAll(asiakirjaEntities)
            }

            vastuuhenkilonArvio = koejaksonVastuuhenkilonArvioRepository.save(vastuuhenkilonArvio)

            /* Sähköposti virkailijalle. Poistetaan käytöstä kommentoimalla.
            mailService.sendEmailFromTemplate(
                to = it.yliopisto?.nimi?.getOpintohallintoEmailAddress(applicationProperties),
                templateName = "virkailijaKoejaksoTarkastettavissa.html",
                titleKey = "email.virkailijakoejaksotarkastettavissa.title",
                properties = mapOf(Pair(MailProperty.ID, vastuuhenkilonArvio.id!!.toString()))
            ) */

            it.erikoistuvaLaakari?.kayttaja?.user?.let { user ->
                user.email = koejaksonVastuuhenkilonArvioDTO.erikoistuvanSahkoposti
                user.phoneNumber = koejaksonVastuuhenkilonArvioDTO.erikoistuvanPuhelinnumero
                userRepository.save(user)
            }

            koejaksonVastuuhenkilonArvioMapper.toDto(vastuuhenkilonArvio)
        }
    }

    override fun update(
        koejaksonVastuuhenkilonArvioDTO: KoejaksonVastuuhenkilonArvioDTO,
        userId: String,
        asiakirjat: Set<AsiakirjaDTO>?,
        deletedAsiakirjaIds: Set<Int>?
    ): KoejaksonVastuuhenkilonArvioDTO {
        val vastuuhenkilonArvio =
            koejaksonVastuuhenkilonArvioRepository.findById(koejaksonVastuuhenkilonArvioDTO.id!!)
                .orElseThrow { EntityNotFoundException("Vastuuhenkilön arviota ei löydy") }

        val updatedVastuuhenkilonArvio =
            koejaksonVastuuhenkilonArvioMapper.toEntity(koejaksonVastuuhenkilonArvioDTO)

        val kirjautunutErikoistuvaLaakari =
            erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)

        val vastuuhenkilo =
            kayttajaRepository.findOneByAuthoritiesYliopistoErikoisalaAndVastuuhenkilonTehtavatyyppi(
                listOf(VASTUUHENKILO),
                vastuuhenkilonArvio.opintooikeus?.yliopisto?.id,
                vastuuhenkilonArvio.opintooikeus?.erikoisala?.id,
                VastuuhenkilonTehtavatyyppiEnum.KOEJAKSOSOPIMUSTEN_JA_KOEJAKSOJEN_HYVAKSYMINEN
            )

        if (kirjautunutErikoistuvaLaakari != null
            && kirjautunutErikoistuvaLaakari == vastuuhenkilonArvio.opintooikeus?.erikoistuvaLaakari
        ) {
            handleErikoistuja(
                vastuuhenkilonArvio,
                koejaksonVastuuhenkilonArvioDTO.erikoistuvanSahkoposti,
                koejaksonVastuuhenkilonArvioDTO.erikoistuvanPuhelinnumero,
                asiakirjat,
                deletedAsiakirjaIds
            )
        } else if (vastuuhenkilo?.user?.id == userId) {
            handleVastuuhenkilo(
                vastuuhenkilonArvio,
                updatedVastuuhenkilonArvio,
                vastuuhenkilo,
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
        phoneNumber: String?,
        asiakirjat: Set<AsiakirjaDTO>?,
        deletedAsiakirjaIds: Set<Int>?
    ) {
        vastuuhenkilonArvio.erikoistuvanKuittausaika = LocalDate.now()

        asiakirjat?.let { asiakirja ->
            val asiakirjaEntities = asiakirja.map { asiakirjaDTO ->
                asiakirjaMapper.toEntity(asiakirjaDTO).apply {
                    this.lisattypvm = LocalDateTime.now()
                    this.opintooikeus = vastuuhenkilonArvio.opintooikeus
                    this.koejaksonVastuuhenkilonArvio = vastuuhenkilonArvio
                    this.asiakirjaData?.data = asiakirjaDTO.asiakirjaData?.fileInputStream?.readAllBytes()
                }
            }

            vastuuhenkilonArvio.asiakirjat.addAll(asiakirjaEntities)
        }

        deletedAsiakirjaIds?.map { x -> x.toLong() }?.let {
            vastuuhenkilonArvio.asiakirjat.removeIf { asiakirja ->
                asiakirja.id in it
            }
        }

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
        if (updated.virkailijanKorjausehdotus.isNullOrBlank()) {
            vastuuhenkilonArvio.virkailija = virkailija
            vastuuhenkilonArvio.virkailijaHyvaksynyt = true
            vastuuhenkilonArvio.virkailijanKuittausaika = LocalDate.now(ZoneId.systemDefault())
            vastuuhenkilonArvio.lisatiedotVirkailijalta = updated.lisatiedotVirkailijalta
            vastuuhenkilonArvio.virkailijanKorjausehdotus = null
            vastuuhenkilonArvio.vastuuhenkilonKorjausehdotus = null
        }
        // Palautettu korjattavaksi
        else {
            vastuuhenkilonArvio.virkailijanKorjausehdotus = updated.virkailijanKorjausehdotus
            vastuuhenkilonArvio.virkailijaHyvaksynyt = false
            vastuuhenkilonArvio.virkailijanKuittausaika = null
            vastuuhenkilonArvio.erikoistuvanKuittausaika = null
        }

        koejaksonVastuuhenkilonArvioRepository.save(vastuuhenkilonArvio)

        // Sähköposti vastuuhenkilölle
        if (vastuuhenkilonArvio.virkailijanKuittausaika != null) {
            val vastuuhenkilo =
                kayttajaRepository.findOneByAuthoritiesYliopistoErikoisalaAndVastuuhenkilonTehtavatyyppi(
                    listOf(VASTUUHENKILO),
                    vastuuhenkilonArvio.opintooikeus?.yliopisto?.id,
                    vastuuhenkilonArvio.opintooikeus?.erikoisala?.id,
                    VastuuhenkilonTehtavatyyppiEnum.KOEJAKSOSOPIMUSTEN_JA_KOEJAKSOJEN_HYVAKSYMINEN
                )
            vastuuhenkilo?.user?.let {
                mailService.sendEmailFromTemplate(
                    it,
                    templateName = "vastuuhenkilonArvioKuitattava.html",
                    titleKey = "email.vastuuhenkilonarviokuitattava.title",
                    properties = mapOf(Pair(MailProperty.ID, vastuuhenkilonArvio.id!!.toString()))
                )
            }
        }
    }

    private fun handleVastuuhenkilo(
        vastuuhenkilonArvio: KoejaksonVastuuhenkilonArvio,
        updated: KoejaksonVastuuhenkilonArvio,
        vastuuhenkilo: Kayttaja,
        email: String?,
        phoneNumber: String?
    ) {
        // Hyväksytty
        if (updated.vastuuhenkilonKorjausehdotus.isNullOrBlank()) {
            vastuuhenkilonArvio.vastuuhenkilo = vastuuhenkilo
            vastuuhenkilonArvio.vastuuhenkiloHyvaksynyt = true
            vastuuhenkilonArvio.vastuuhenkilonKuittausaika = LocalDate.now(ZoneId.systemDefault())
            vastuuhenkilonArvio.koejaksoHyvaksytty = updated.koejaksoHyvaksytty

            if (updated.koejaksoHyvaksytty == false) {
                vastuuhenkilonArvio.perusteluHylkaamiselle = updated.perusteluHylkaamiselle
                vastuuhenkilonArvio.hylattyArviointiKaytyLapiKeskustellen =
                    updated.hylattyArviointiKaytyLapiKeskustellen
            }
        }
        // Palautettu korjattavaksi
        else {
            vastuuhenkilonArvio.vastuuhenkilonKorjausehdotus = updated.vastuuhenkilonKorjausehdotus
            vastuuhenkilonArvio.vastuuhenkiloHyvaksynyt = false
            vastuuhenkilonArvio.vastuuhenkilonKuittausaika = null
            vastuuhenkilonArvio.virkailijaHyvaksynyt = false
            vastuuhenkilonArvio.virkailijanKuittausaika = null
            vastuuhenkilonArvio.erikoistuvanKuittausaika = null
            vastuuhenkilonArvio.lisatiedotVirkailijalta = null
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
                asiakirjaData = AsiakirjaData(data = outputStream.toByteArray())
            )
        )

        val yliopisto = vastuuhenkilonArvio.opintooikeus?.yliopisto?.nimi!!
        if (!sarakesignService.getApiUrl(yliopisto)
                .isNullOrBlank()
        ) {
            lahetaAllekirjoitettavaksi(vastuuhenkilonArvio, asiakirja)
        } else if (arkistointiService.onKaytossa(yliopisto)) {
            val filePath = arkistointiService.muodostaSahke(
                vastuuhenkilonArvio.opintooikeus,
                listOf(Pair(asiakirja, "-1")),
                asiaTunnus = vastuuhenkilonArvio.id!!.toString(),
                asiaTyyppi = "Koejakson arviointi",
                hyvaksyja = vastuuhenkilonArvio.vastuuhenkilo?.user?.getName(),
                hyvaksymisPaiva = vastuuhenkilonArvio.vastuuhenkilonKuittausaika
            )
            val yek = vastuuhenkilonArvio.opintooikeus?.erikoisala?.id == YEK_ERIKOISALA_ID
            arkistointiService.laheta(yliopisto, filePath, yek)
        }

    }

    private fun lahetaAllekirjoitettavaksi(
        vastuuhenkilonArvio: KoejaksonVastuuhenkilonArvio,
        asiakirja: Asiakirja
    ) {
        val recipients: MutableList<SarakeSignRecipientDTO> = mutableListOf()
        vastuuhenkilonArvio.vastuuhenkilo?.user?.let {
            recipients.add(
                lisaaVastaanottaja(
                    it,
                    false
                )
            )
        }
        vastuuhenkilonArvio.opintooikeus?.erikoistuvaLaakari?.kayttaja?.user?.let {
            recipients.add(
                lisaaVastaanottaja(it, true)
            )
        }
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
        val arvio = koejaksonVastuuhenkilonArvioRepository.findById(id).orElse(null)
            ?: return Optional.empty()
        val vastuuhenkilo =
            kayttajaRepository.findOneByAuthoritiesYliopistoErikoisalaAndVastuuhenkilonTehtavatyyppi(
                listOf(VASTUUHENKILO),
                arvio.opintooikeus?.yliopisto?.id,
                arvio.opintooikeus?.erikoisala?.id,
                VastuuhenkilonTehtavatyyppiEnum.KOEJAKSOSOPIMUSTEN_JA_KOEJAKSOJEN_HYVAKSYMINEN
            )
        if (vastuuhenkilo?.user?.id == userId) {
            tarkistaAllekirjoitus(arvio)
            return Optional.of(arvio).map(this::mapVastuuhenkilonArvio)
        }
        return Optional.empty()
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
            val response =
                sarakesignService.tarkistaAllekirjoitus(
                    koejaksonVastuuhenkilonArvio.sarakeSignRequestId,
                    yliopisto
                )

            if (response?.status == 3) { // Completed
                koejaksonVastuuhenkilonArvio.allekirjoitettu = true
                koejaksonVastuuhenkilonArvio.allekirjoitusaika = response.finished?.withZoneSameInstant(ZoneId.systemDefault())?.toLocalDate()
            } else if (response?.status == 4) { // Aborted
                koejaksonVastuuhenkilonArvio.virkailijanKorjausehdotus = "Allekirjoitus keskeytetty"
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
        val authentication =
            SecurityContextHolder.getContext().authentication as Saml2Authentication
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
                .filter { it.id != opintoOikeusId && it.erikoisalaId != YEK_ERIKOISALA_ID }
        val koulutussopimus = koulutussopimusRepository.findByOpintooikeusId(opintoOikeusId)
        result.koulutussopimusHyvaksytty =
            koulutussopimus.isPresent && koulutussopimus.get().vastuuhenkiloHyvaksynyt == true
        result.tila = KoejaksoTila.fromVastuuhenkilonArvio(
            vastuuhenkilonArvio,
            (authentication.principal as Saml2AuthenticatedPrincipal).name,
            authentication.authorities.any { it.authority == OPINTOHALLINNON_VIRKAILIJA },
            authentication.authorities.any { it.authority == VASTUUHENKILO })
        return result
    }
}
