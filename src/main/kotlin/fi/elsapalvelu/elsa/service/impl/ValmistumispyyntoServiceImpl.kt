package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.domain.Valmistumispyynto.Companion.fromValmistumispyyntoArvioija
import fi.elsapalvelu.elsa.domain.Valmistumispyynto.Companion.fromValmistumispyyntoArvioijaHyvaksyja
import fi.elsapalvelu.elsa.domain.Valmistumispyynto.Companion.fromValmistumispyyntoErikoistuja
import fi.elsapalvelu.elsa.domain.Valmistumispyynto.Companion.fromValmistumispyyntoHyvaksyja
import fi.elsapalvelu.elsa.domain.Valmistumispyynto.Companion.fromValmistumispyyntoVirkailija
import fi.elsapalvelu.elsa.domain.enumeration.*
import fi.elsapalvelu.elsa.extensions.format
import fi.elsapalvelu.elsa.extensions.toDays
import fi.elsapalvelu.elsa.extensions.toMonths
import fi.elsapalvelu.elsa.extensions.toYears
import fi.elsapalvelu.elsa.repository.*
import fi.elsapalvelu.elsa.security.OPINTOHALLINNON_VIRKAILIJA
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.constants.*
import fi.elsapalvelu.elsa.service.criteria.NimiErikoisalaAndAvoinCriteria
import fi.elsapalvelu.elsa.service.dto.*
import fi.elsapalvelu.elsa.service.dto.enumeration.ValmistumispyynnonHyvaksyjaRole
import fi.elsapalvelu.elsa.service.dto.enumeration.ValmistumispyynnonTila
import fi.elsapalvelu.elsa.service.dto.sarakesign.SarakeSignRecipientDTO
import fi.elsapalvelu.elsa.service.dto.sarakesign.SarakeSignRecipientFieldsDTO
import fi.elsapalvelu.elsa.service.mapper.*
import org.hibernate.engine.jdbc.BlobProxy
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.thymeleaf.context.Context
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*
import jakarta.persistence.EntityNotFoundException
import org.springframework.web.multipart.MultipartFile

private const val VANHENTUNUT_KUULUSTELU_YEARS = 4L
private const val VANHENTUNUT_SUORITUS_YEARS_EL = 10L
private const val VANHENTUNUT_SUORITUS_YEARS_EHL = 6L
private const val ARVIOINTI_VAHINTAAN = 4

@Service
@Transactional
class ValmistumispyyntoServiceImpl(
    private val valmistumispyyntoRepository: ValmistumispyyntoRepository,
    private val valmistumispyyntoQueryService: ValmistumispyyntoQueryService,
    private val arvioitavaKokonaisuusRepository: ArvioitavaKokonaisuusRepository,
    private val suoritusarviointiRepository: SuoritusarviointiRepository,
    private val kayttajaRepository: KayttajaRepository,
    private val kayttajaMapper: KayttajaMapper,
    private val valmistumispyyntoMapper: ValmistumispyyntoMapper,
    private val valmistumispyyntoOsaamisenArviointiMapper: ValmistumispyyntoOsaamisenArviointiMapper,
    private val opintooikeusRepository: OpintooikeusRepository,
    private val tyoskentelyjaksoRepository: TyoskentelyjaksoRepository,
    private val tyoskentelyjaksoMapper: TyoskentelyjaksoMapper,
    private val opintosuoritusRepository: OpintosuoritusRepository,
    private val mailService: MailService,
    private val applicationProperties: ApplicationProperties,
    private val clock: Clock,
    private val valmistumispyynnonTarkistusRepository: ValmistumispyynnonTarkistusRepository,
    private val valmistumispyynnonTarkistusMapper: ValmistumispyynnonTarkistusMapper,
    private val valmistumispyynnonTarkistusUpdateMapper: ValmistumispyynnonTarkistusUpdateMapper,
    private val tyoskentelyjaksoService: TyoskentelyjaksoService,
    private val terveyskeskuskoulutusjaksonHyvaksyntaRepository: TerveyskeskuskoulutusjaksonHyvaksyntaRepository,
    private val teoriakoulutusRepository: TeoriakoulutusRepository,
    private val opintosuoritusMapper: OpintosuoritusMapper,
    private val koejaksonVastuuhenkilonArvioRepository: KoejaksonVastuuhenkilonArvioRepository,
    private val pdfService: PdfService,
    private val asiakirjaRepository: AsiakirjaRepository,
    private val sarakesignService: SarakesignService,
    private val teoriakoulutusService: TeoriakoulutusService,
    private val suoritusarviointiMapper: SuoritusarviointiMapper,
    private val arviointiasteikkoService: ArviointiasteikkoService,
    private val asiakirjaMapper: AsiakirjaMapper,
    private val koulutussuunnitelmaRepository: KoulutussuunnitelmaRepository,
    private val suoritemerkintaRepository: SuoritemerkintaRepository,
    private val suoritteenKategoriaRepository: SuoritteenKategoriaRepository,
    private val suoritemerkintaMapper: SuoritemerkintaMapper,
    private val paivakirjamerkintaRepository: PaivakirjamerkintaRepository,
    private val seurantajaksoService: SeurantajaksoService,
    private val userRepository: UserRepository,
    private val erikoistuvaLaakariService: ErikoistuvaLaakariService
) : ValmistumispyyntoService {

    @Transactional(readOnly = true)
    override fun findErikoisalaTyyppiByOpintooikeusId(opintooikeusId: Long): ErikoisalaTyyppi =
        getOpintooikeus(opintooikeusId).erikoisala?.tyyppi ?: throw EntityNotFoundException(ERIKOISALA_NOT_FOUND_ERROR)

    @Transactional(readOnly = true)
    override fun findOneByOpintooikeusId(opintooikeusId: Long): ValmistumispyyntoDTO? {
        val valmistumispyynto = valmistumispyyntoRepository.findByOpintooikeusId(opintooikeusId)
        val tila = fromValmistumispyyntoErikoistuja(valmistumispyynto)
        val opintooikeus = getOpintooikeus(opintooikeusId)
        val yliopistoId = opintooikeus.yliopisto?.id!!
        val erikoisalaId = opintooikeus.erikoisala?.id!!
        val vastuuhenkiloOsaamisenarvioija =
            getVastuuhenkiloOsaamisenArvioija(yliopistoId, erikoisalaId)
        val vastuuhenkiloHyvaksyja = getVastuuhenkiloHyvaksyja(yliopistoId, erikoisalaId)
        valmistumispyynto?.let { tarkistaAllekirjoitus(it) }
        val valmistumispyyntoDTO =
            valmistumispyynto?.let { valmistumispyyntoMapper.toDto(it) } ?: ValmistumispyyntoDTO().apply {
                erikoistujanLaillistamispaiva = opintooikeus.erikoistuvaLaakari?.laillistamispaiva
                erikoistujanLaillistamistodistus = opintooikeus.erikoistuvaLaakari?.laillistamispaivanLiitetiedosto
                erikoistujanLaillistamistodistusNimi =
                    opintooikeus.erikoistuvaLaakari?.laillistamispaivanLiitetiedostonNimi
                erikoistujanLaillistamistodistusTyyppi =
                    opintooikeus.erikoistuvaLaakari?.laillistamispaivanLiitetiedostonTyyppi
            }

        return valmistumispyyntoDTO.apply {
            this.tila = tila
            vastuuhenkiloOsaamisenArvioijaNimi = vastuuhenkiloOsaamisenarvioija.getNimi()
            vastuuhenkiloOsaamisenArvioijaNimike = vastuuhenkiloOsaamisenarvioija.nimike
            vastuuhenkiloHyvaksyjaNimi = vastuuhenkiloHyvaksyja.getNimi()
            vastuuhenkiloHyvaksyjaNimike = vastuuhenkiloHyvaksyja.nimike
        }
    }

    @Transactional(readOnly = true)
    override fun findSuoritustenTila(
        opintooikeusId: Long,
        erikoisalaTyyppi: ErikoisalaTyyppi
    ): VanhentuneetSuorituksetDTO {
        val vanhentunutSuoritusYears =
            if (erikoisalaTyyppi == ErikoisalaTyyppi.LAAKETIEDE) VANHENTUNUT_SUORITUS_YEARS_EL
            else VANHENTUNUT_SUORITUS_YEARS_EHL
        val vanhojaTyoskentelyjaksojaExists =
            tyoskentelyjaksoRepository.findAllByOpintooikeusId(opintooikeusId).asSequence().filter {
                it.tyoskentelypaikka?.tyyppi != TyoskentelyjaksoTyyppi.TERVEYSKESKUS
            }.any {
                it.alkamispaiva?.isBefore(LocalDate.now(clock).minusYears(vanhentunutSuoritusYears)) == true
            }
        val opintosuoritukset = opintosuoritusRepository.findAllByOpintooikeusId(opintooikeusId).asSequence()
        val vanhojaSuorituksiaExists =
            opintosuoritukset.filter { it.tyyppi?.nimi != OpintosuoritusTyyppiEnum.VALTAKUNNALLINEN_KUULUSTELU }.any {
                it.suorituspaiva?.isBefore(LocalDate.now(clock).minusYears(vanhentunutSuoritusYears)) == true
            }
        val kuulusteluVanhentunut =
            opintosuoritukset.filter { it.tyyppi?.nimi == OpintosuoritusTyyppiEnum.VALTAKUNNALLINEN_KUULUSTELU }.any {
                it.suorituspaiva?.isBefore(LocalDate.now(clock).minusYears(VANHENTUNUT_KUULUSTELU_YEARS)) == true
            }

        return VanhentuneetSuorituksetDTO(
            vanhojaTyoskentelyjaksojaOrSuorituksiaExists = vanhojaTyoskentelyjaksojaExists || vanhojaSuorituksiaExists,
            kuulusteluVanhentunut = kuulusteluVanhentunut
        )
    }

    override fun create(
        opintooikeusId: Long,
        uusiValmistumispyyntoDTO: UusiValmistumispyyntoDTO
    ): ValmistumispyyntoDTO {
        getOpintooikeus(opintooikeusId).let { opintooikeus ->
            opintooikeus.erikoistuvaLaakari?.kayttaja?.user?.let { user ->
                user.email = uusiValmistumispyyntoDTO.erikoistujanSahkoposti
                user.phoneNumber = uusiValmistumispyyntoDTO.erikoistujanPuhelinnumero
                userRepository.save(user)
            }
            val valmistumispyynto = Valmistumispyynto(
                opintooikeus = opintooikeus,
                selvitysVanhentuneistaSuorituksista = uusiValmistumispyyntoDTO.selvitysVanhentuneistaSuorituksista,
                erikoistujanKuittausaika = LocalDate.now()
            )
            val vastuuhenkiloOsaamisenArvioijaUser = getVastuuhenkiloOsaamisenArvioija(
                opintooikeus.yliopisto?.id!!,
                opintooikeus.erikoisala?.id!!
            ).user!!
            valmistumispyyntoRepository.save(valmistumispyynto).let { saved ->
                sendMailNotificationUusiValmistumispyynto(vastuuhenkiloOsaamisenArvioijaUser, saved)
                return valmistumispyyntoMapper.toDto(saved)
                    .apply { tila = ValmistumispyynnonTila.ODOTTAA_VASTUUHENKILON_TARKASTUSTA }
            }
        }
    }

    override fun update(
        opintooikeusId: Long,
        uusiValmistumispyyntoDTO: UusiValmistumispyyntoDTO
    ): ValmistumispyyntoDTO {
        getOpintooikeus(opintooikeusId).let { opintooikeus ->
            opintooikeus.erikoistuvaLaakari?.kayttaja?.user?.let { user ->
                user.email = uusiValmistumispyyntoDTO.erikoistujanSahkoposti
                user.phoneNumber = uusiValmistumispyyntoDTO.erikoistujanPuhelinnumero
                userRepository.save(user)
            }
            val vastuuhenkiloOsaamisenArvioijaUser =
                getVastuuhenkiloOsaamisenArvioija(opintooikeus.yliopisto?.id!!, opintooikeus.erikoisala?.id!!).user!!
            getValmistumispyyntoByOpintooikeusId(opintooikeusId).apply {
                vastuuhenkiloOsaamisenArvioijaKorjausehdotus = null
                vastuuhenkiloOsaamisenArvioijaPalautusaika = null
                erikoistujanKuittausaika = LocalDate.now()
                this.selvitysVanhentuneistaSuorituksista = uusiValmistumispyyntoDTO.selvitysVanhentuneistaSuorituksista
            }.let {
                valmistumispyyntoRepository.save(it).let { saved ->
                    sendMailNotificationUusiValmistumispyynto(vastuuhenkiloOsaamisenArvioijaUser, saved)
                    return valmistumispyyntoMapper.toDto(saved)
                        .apply { tila = ValmistumispyynnonTila.ODOTTAA_VASTUUHENKILON_TARKASTUSTA }
                }
            }
        }
    }

    @Transactional
    override fun updateOsaamisenArviointiByOsaamisenArvioijaUserId(
        id: Long,
        userId: String,
        osaamisenArviointiDTO: ValmistumispyyntoOsaamisenArviointiFormDTO
    ): ValmistumispyyntoDTO {
        val kayttaja = getKayttaja(userId)
        val yliopisto = getYliopisto(kayttaja)
        val valmistumispyynto = getValmistumispyynnonHyvaksyjaRoleForVastuuhenkilo(kayttaja).takeIf {
            it == ValmistumispyynnonHyvaksyjaRole.VASTUUHENKILO_OSAAMISEN_ARVIOIJA_HYVAKSYJA ||
                it == ValmistumispyynnonHyvaksyjaRole.VASTUUHENKILO_OSAAMISEN_ARVIOIJA
        }?.let {
            val valmistumispyynto =
                getValmistumispyyntoByYliopistoIdAndErikoisalaIdsOrThrow(
                    id,
                    yliopisto.id!!,
                    getErikoisalaIds(kayttaja)
                )
            valmistumispyynto.vastuuhenkiloOsaamisenArvioija = kayttaja

            if (osaamisenArviointiDTO.osaaminenRiittavaValmistumiseen == true) {
                valmistumispyynto.vastuuhenkiloOsaamisenArvioijaKuittausaika = LocalDate.now()
                valmistumispyynto.virkailijanPalautusaika = null
                valmistumispyynto.virkailijanKorjausehdotus = null
                sendMailNotificationOdottaaVirkailijanTarkastusta(
                    yliopisto.nimi!!,
                    valmistumispyynto.id!!
                )
            } else {
                valmistumispyynto.vastuuhenkiloOsaamisenArvioijaPalautusaika = LocalDate.now()
                valmistumispyynto.vastuuhenkiloOsaamisenArvioijaKorjausehdotus = osaamisenArviointiDTO.korjausehdotus
                valmistumispyynto.erikoistujanKuittausaika = null
                sendMailNotificationOsaamisenArvioijaPalauttanut(valmistumispyynto)
            }
            valmistumispyynto
        } ?: throw getValmistumispyyntoNotFoundException()

        return valmistumispyyntoMapper.toDto(valmistumispyynto).apply {
            tila = getValmistumispyynnonTilaForArvioija(valmistumispyynto)
        }
    }

    override fun updateValmistumispyyntoByHyvaksyjaUserId(
        id: Long,
        userId: String,
        hyvaksyntaFormDTO: ValmistumispyyntoHyvaksyntaFormDTO
    ): ValmistumispyynnonTarkistusDTO {
        val kayttaja = getKayttaja(userId)

        kayttaja?.user?.let { user ->
            user.email = hyvaksyntaFormDTO.sahkoposti
            user.phoneNumber = hyvaksyntaFormDTO.puhelinnumero
            userRepository.save(user)
        }

        val yliopisto = getYliopisto(kayttaja)
        val valmistumispyynto = getValmistumispyynnonHyvaksyjaRoleForVastuuhenkilo(kayttaja).takeIf {
            it == ValmistumispyynnonHyvaksyjaRole.VASTUUHENKILO_OSAAMISEN_ARVIOIJA_HYVAKSYJA ||
                it == ValmistumispyynnonHyvaksyjaRole.VASTUUHENKILO_HYVAKSYJA
        }?.let {
            val valmistumispyynto =
                getValmistumispyyntoByYliopistoIdAndErikoisalaIdsOrThrow(
                    id,
                    yliopisto.id!!,
                    getErikoisalaIds(kayttaja)
                )
            valmistumispyynto.vastuuhenkiloHyvaksyja = kayttaja

            if (hyvaksyntaFormDTO.korjausehdotus != null) {
                valmistumispyynto.vastuuhenkiloHyvaksyjaPalautusaika = LocalDate.now()
                valmistumispyynto.vastuuhenkiloHyvaksyjaKorjausehdotus = hyvaksyntaFormDTO.korjausehdotus
                valmistumispyynto.erikoistujanKuittausaika = null
                valmistumispyynto.vastuuhenkiloOsaamisenArvioijaKuittausaika = null
                valmistumispyynto.virkailijanKuittausaika = null
                sendMailNotificationHyvaksyjaPalauttanut(valmistumispyynto)
            } else {
                valmistumispyynto.vastuuhenkiloHyvaksyjaKuittausaika = LocalDate.now()
                val result = valmistumispyyntoRepository.save(valmistumispyynto)
                result.valmistumispyynnonTarkistus?.let {
                    luoYhteenvetoPdf(mapValmistumispyynnonTarkistus(valmistumispyynnonTarkistusMapper.toDto(it)), valmistumispyynto)
                    luoLiitteetPdf(valmistumispyynto)
                    luoErikoistujanTiedotPdf(valmistumispyynto)
                }
            }
            valmistumispyynto
        } ?: throw getValmistumispyyntoNotFoundException()

        val valmistumispyynnonTarkistus = valmistumispyynnonTarkistusRepository.findByValmistumispyyntoId(valmistumispyynto.id!!)

        return valmistumispyynnonTarkistusMapper.toDto(valmistumispyynnonTarkistus!!).apply {
            this.kommentitVirkailijoille = null
            this.valmistumispyynto?.tila = getValmistumispyynnonTilaForHyvaksyja(valmistumispyynto)
        }
    }

    override fun updateTarkistusByVirkailijaUserId(
        id: Long,
        userId: String,
        valmistumispyynnonTarkistusDTO: ValmistumispyynnonTarkistusUpdateDTO,
        laillistamistodistus: MultipartFile?
    ): ValmistumispyynnonTarkistusDTO? {
        val kayttaja = getKayttaja(userId)
        val yliopisto = kayttaja.yliopistot.first()
        var tarkistus = valmistumispyynnonTarkistusRepository.findByValmistumispyyntoIdAndValmistumispyyntoOpintooikeusYliopistoId(
            id,
            yliopisto.id!!
        )

        if (tarkistus != null) {
            tarkistus.yekSuoritettu = valmistumispyynnonTarkistusDTO.yekSuoritettu
            tarkistus.yekSuorituspaiva = valmistumispyynnonTarkistusDTO.yekSuorituspaiva
            tarkistus.ptlSuoritettu = valmistumispyynnonTarkistusDTO.ptlSuoritettu
            tarkistus.ptlSuorituspaiva = valmistumispyynnonTarkistusDTO.ptlSuorituspaiva
            tarkistus.aiempiElKoulutusSuoritettu =
                valmistumispyynnonTarkistusDTO.aiempiElKoulutusSuoritettu
            tarkistus.aiempiElKoulutusSuorituspaiva =
                valmistumispyynnonTarkistusDTO.aiempiElKoulutusSuorituspaiva
            tarkistus.ltTutkintoSuoritettu = valmistumispyynnonTarkistusDTO.ltTutkintoSuoritettu
            tarkistus.ltTutkintoSuorituspaiva = valmistumispyynnonTarkistusDTO.ltTutkintoSuorituspaiva
            tarkistus.yliopistosairaalanUlkopuolinenTyoTarkistettu =
                valmistumispyynnonTarkistusDTO.yliopistosairaalanUlkopuolinenTyoTarkistettu
            tarkistus.yliopistosairaalatyoTarkistettu =
                valmistumispyynnonTarkistusDTO.yliopistosairaalatyoTarkistettu
            tarkistus.kokonaistyoaikaTarkistettu =
                valmistumispyynnonTarkistusDTO.kokonaistyoaikaTarkistettu
            tarkistus.teoriakoulutusTarkistettu = valmistumispyynnonTarkistusDTO.teoriakoulutusTarkistettu
            tarkistus.kommentitVirkailijoille = valmistumispyynnonTarkistusDTO.kommentitVirkailijoille
            tarkistus.koejaksoEiVaadittu = valmistumispyynnonTarkistusDTO.koejaksoEiVaadittu
        } else {
            valmistumispyyntoRepository.findByIdAndOpintooikeusYliopistoId(id, yliopisto.id!!)
                ?.let {
                    tarkistus = valmistumispyynnonTarkistusUpdateMapper.toEntity(valmistumispyynnonTarkistusDTO)
                    tarkistus?.valmistumispyynto = it
                }
        }

        tarkistus?.let {
            valmistumispyynnonTarkistusRepository.save(it)

            if (laillistamistodistus != null || valmistumispyynnonTarkistusDTO.laillistamispaiva != null) {
                erikoistuvaLaakariService.updateLaillistamispaiva(
                    it.valmistumispyynto?.opintooikeus?.erikoistuvaLaakari?.kayttaja?.user?.id!!,
                    valmistumispyynnonTarkistusDTO.laillistamispaiva,
                    laillistamistodistus?.bytes,
                    laillistamistodistus?.originalFilename,
                    laillistamistodistus?.contentType
                )
            }

            if (valmistumispyynnonTarkistusDTO.keskenerainen != true) {
                it.valmistumispyynto?.virkailija = kayttaja

                if (valmistumispyynnonTarkistusDTO.korjausehdotus != null) {
                    val osaamisenArvioija = it.valmistumispyynto?.vastuuhenkiloOsaamisenArvioija
                    it.valmistumispyynto?.virkailijanKorjausehdotus = valmistumispyynnonTarkistusDTO.korjausehdotus
                    it.valmistumispyynto?.virkailijanPalautusaika = LocalDate.now(clock)
                    it.valmistumispyynto?.vastuuhenkiloOsaamisenArvioija = null
                    it.valmistumispyynto?.vastuuhenkiloOsaamisenArvioijaKuittausaika = null
                    it.valmistumispyynto?.erikoistujanKuittausaika = null
                    sendMailNotificationVirkailijaPalauttanut(it.valmistumispyynto!!, osaamisenArvioija)
                } else {
                    it.valmistumispyynto?.virkailijanSaate = valmistumispyynnonTarkistusDTO.lisatiedotVastuuhenkilolle
                    it.valmistumispyynto?.virkailijanKuittausaika = LocalDate.now(clock)
                    it.valmistumispyynto?.vastuuhenkiloHyvaksyjaKorjausehdotus = null
                    it.valmistumispyynto?.vastuuhenkiloHyvaksyjaPalautusaika = null
                    sendMailNotificationOdottaaHyvaksyntaa(it.valmistumispyynto!!)
                }

                it.valmistumispyynto?.let { pyynto -> valmistumispyyntoRepository.save(pyynto) }
            }

            return valmistumispyynnonTarkistusMapper.toDto(it)
        }

        return null
    }

    @Transactional(readOnly = true)
    override fun findAllForVastuuhenkiloByCriteria(
        userId: String,
        valmistumispyyntoCriteria: NimiErikoisalaAndAvoinCriteria,
        pageable: Pageable
    ): Page<ValmistumispyyntoListItemDTO> {
        val kayttaja = getKayttaja(userId)
        val yliopisto = getYliopisto(kayttaja)
        val hyvaksyjaRole = getValmistumispyynnonHyvaksyjaRoleForVastuuhenkilo(kayttaja) ?: return Page.empty()
        return valmistumispyyntoQueryService.findValmistumispyynnotByCriteria(
            valmistumispyyntoCriteria,
            hyvaksyjaRole,
            pageable,
            yliopisto.id!!,
            getErikoisalaIds(kayttaja),
            kayttaja.user?.langKey
        ).map {
            val isAvoin = valmistumispyyntoCriteria.avoin == true
            tarkistaAllekirjoitus(it)
            mapValmistumispyyntoListItem(it, hyvaksyjaRole, isAvoin)
        }
    }

    override fun findAllForVirkailijaByCriteria(
        userId: String,
        valmistumispyyntoCriteria: NimiErikoisalaAndAvoinCriteria,
        pageable: Pageable
    ): Page<ValmistumispyyntoListItemDTO> {
        val kayttaja = getKayttaja(userId)
        val hyvaksyjaRole = ValmistumispyynnonHyvaksyjaRole.VIRKAILIJA
        return valmistumispyyntoQueryService.findValmistumispyynnotByCriteria(
            valmistumispyyntoCriteria,
            hyvaksyjaRole,
            pageable,
            kayttaja.yliopistot.first().id!!,
            listOf(),
            kayttaja.user?.langKey
        ).map {
            val isAvoin = valmistumispyyntoCriteria.avoin == true
            tarkistaAllekirjoitus(it)
            mapValmistumispyyntoListItem(it, hyvaksyjaRole, isAvoin)
        }
    }

    @Transactional(readOnly = true)
    override fun findOneByIdAndVastuuhenkiloOsaamisenArvioijaUserId(
        id: Long,
        userId: String
    ): ValmistumispyyntoOsaamisenArviointiDTO? {
        val kayttaja = getKayttaja(userId)
        val yliopisto = getYliopisto(kayttaja)
        val valmistumispyynto = getValmistumispyynnonHyvaksyjaRoleForVastuuhenkilo(kayttaja).takeIf {
            it == ValmistumispyynnonHyvaksyjaRole.VASTUUHENKILO_OSAAMISEN_ARVIOIJA_HYVAKSYJA ||
                it == ValmistumispyynnonHyvaksyjaRole.VASTUUHENKILO_OSAAMISEN_ARVIOIJA
        }?.let {
            getValmistumispyyntoByYliopistoIdAndErikoisalaIdsOrThrow(
                id,
                yliopisto.id!!,
                getErikoisalaIds(kayttaja)
            )
        } ?: throw getValmistumispyyntoNotFoundException()

        tarkistaAllekirjoitus(valmistumispyynto)

        return valmistumispyyntoOsaamisenArviointiMapper.toDto(valmistumispyynto).apply {
            tila = getValmistumispyynnonTilaForArvioija(valmistumispyynto)
        }
    }

    override fun findOneByIdAndVirkailijaUserId(
        id: Long,
        userId: String
    ): ValmistumispyynnonTarkistusDTO? {
        val kayttaja = getKayttaja(userId)
        val yliopisto = kayttaja.yliopistot.first()
        valmistumispyynnonTarkistusRepository.findByValmistumispyyntoIdAndValmistumispyyntoOpintooikeusYliopistoId(
            id,
            yliopisto.id!!
        )?.let {
            val result = mapValmistumispyynnonTarkistus(valmistumispyynnonTarkistusMapper.toDto(it))
            it.valmistumispyynto?.let { pyynto ->
                result.valmistumispyynto?.tila = getValmistumispyynnonTilaForVirkailija(pyynto)
            }
            return result
        }

        val valmistumispyynto =
            valmistumispyyntoRepository.findByIdAndOpintooikeusYliopistoId(id, yliopisto.id!!)
                ?: throw getValmistumispyyntoNotFoundException()

        tarkistaAllekirjoitus(valmistumispyynto)

        return mapValmistumispyynnonTarkistus(ValmistumispyynnonTarkistusDTO(
            valmistumispyynto = valmistumispyyntoMapper.toDto(
                valmistumispyynto
            ).apply { tila = getValmistumispyynnonTilaForVirkailija(valmistumispyynto) }
        ))
    }

    override fun findOneByIdAndVastuuhenkiloHyvaksyjaUserId(
        id: Long,
        userId: String
    ): ValmistumispyynnonTarkistusDTO? {
        val kayttaja = getKayttaja(userId)
        val yliopisto = getYliopisto(kayttaja)
        val tarkistus = getValmistumispyynnonHyvaksyjaRoleForVastuuhenkilo(kayttaja).takeIf {
            it == ValmistumispyynnonHyvaksyjaRole.VASTUUHENKILO_HYVAKSYJA ||
                it == ValmistumispyynnonHyvaksyjaRole.VASTUUHENKILO_OSAAMISEN_ARVIOIJA_HYVAKSYJA
        }?.let {
            valmistumispyynnonTarkistusRepository.findByValmistumispyyntoIdForHyvaksyja(
                id,
                yliopisto.id!!,
                getErikoisalaIds(kayttaja))
        } ?: throw getValmistumispyyntoNotFoundException()

        tarkistus.valmistumispyynto?.let { tarkistaAllekirjoitus(it) }

        val result = mapValmistumispyynnonTarkistus(valmistumispyynnonTarkistusMapper.toDto(tarkistus))
        result.kommentitVirkailijoille = null
        tarkistus.valmistumispyynto?.let { pyynto ->
            result.valmistumispyynto?.tila = getValmistumispyynnonTilaForHyvaksyja(pyynto)
        }
        return result
    }

    @Transactional(readOnly = true)
    override fun existsByOpintooikeusId(opintooikeusId: Long): Boolean {
        return valmistumispyyntoRepository.existsByOpintooikeusId(opintooikeusId)
    }

    @Transactional(readOnly = true)
    override fun findArviointienTilaByIdAndOsaamisenArvioijaUserId(
        id: Long,
        userId: String
    ): ValmistumispyyntoArviointienTilaDTO? {
        val kayttaja = getKayttaja(userId)
        val arviointienTilaDTO = getValmistumispyynnonHyvaksyjaRoleForVastuuhenkilo(kayttaja).takeIf {
            it == ValmistumispyynnonHyvaksyjaRole.VASTUUHENKILO_OSAAMISEN_ARVIOIJA_HYVAKSYJA ||
                it == ValmistumispyynnonHyvaksyjaRole.VASTUUHENKILO_OSAAMISEN_ARVIOIJA
        }?.let {
            getArviointienTila(id, kayttaja)
        } ?: throw getValmistumispyyntoNotFoundException()

        return arviointienTilaDTO
    }

    @Transactional(readOnly = true)
    override fun getValmistumispyynnonHyvaksyjaRole(userId: String): ValmistumispyynnonHyvaksyjaRole? {
        val kayttaja = getKayttaja(userId)
        return getValmistumispyynnonHyvaksyjaRoleForVastuuhenkilo(kayttaja)
    }

    override fun getValmistumispyynnonAsiakirja(
        userId: String,
        valmistumispyyntoId: Long,
        asiakirjaId: Long
    ): AsiakirjaDTO? {
        valmistumispyyntoRepository.findByIdOrNull(valmistumispyyntoId)?.let {
            if ((it.opintooikeus?.erikoistuvaLaakari?.kayttaja?.user?.id == userId ||
                it.vastuuhenkiloOsaamisenArvioija?.user?.id == userId ||
                it.vastuuhenkiloHyvaksyja?.user?.id == userId ||
                it.virkailija?.user?.id == userId) &&
                (it.yhteenvetoAsiakirja?.id == asiakirjaId || it.liitteetAsiakirja?.id == asiakirjaId)) {
                asiakirjaRepository.findByIdOrNull(asiakirjaId)?.let { asiakirja ->
                    val result = asiakirjaMapper.toDto(asiakirja)
                    result.asiakirjaData?.fileInputStream =
                        ByteArrayInputStream(asiakirja.asiakirjaData?.data)
                    return result
                }
            }
        }
        return null
    }

    private fun tarkistaAllekirjoitus(valmistumispyynto: Valmistumispyynto) {
        val yliopisto = valmistumispyynto.opintooikeus?.yliopisto?.nimi!!
        if (valmistumispyynto.vastuuhenkiloHyvaksyjaKuittausaika != null
            && valmistumispyynto.allekirjoitusaika == null
            && !sarakesignService.getApiUrl(yliopisto).isNullOrBlank()
            && valmistumispyynto.sarakeSignRequestId != null
        ) {
            val response =
                sarakesignService.tarkistaAllekirjoitus(
                    valmistumispyynto.sarakeSignRequestId,
                    yliopisto
                )

            if (response?.status == 3) { // Completed
                valmistumispyynto.allekirjoitusaika = response.finished?.withZoneSameInstant(ZoneId.systemDefault())?.toLocalDate()
            } else if (response?.status == 4) { // Aborted
                valmistumispyynto.vastuuhenkiloHyvaksyjaKuittausaika = null
            }
            valmistumispyyntoRepository.save(valmistumispyynto)
        }
    }

    private fun getKayttaja(userId: String) =
        kayttajaRepository.findOneByUserId(userId).orElseThrow { EntityNotFoundException(KAYTTAJA_NOT_FOUND_ERROR) }

    private fun getOpintooikeus(id: Long) =
        opintooikeusRepository.findByIdOrNull(id) ?: throw EntityNotFoundException(OPINTOOIKEUS_NOT_FOUND_ERROR)

    private fun getYliopisto(kayttaja: Kayttaja) =
        kayttaja.yliopistotAndErikoisalat.firstOrNull()?.yliopisto ?: throw EntityNotFoundException(
            KAYTTAJA_YLIOPISTO_ERIKOISALA_NOT_FOUND_ERROR
        )

    private fun getErikoisalaIds(kayttaja: Kayttaja) = kayttaja.yliopistotAndErikoisalat.map { it.erikoisala?.id!! }

    private fun getValmistumispyyntoByYliopistoIdAndErikoisalaIdsOrThrow(
        id: Long,
        yliopistoId: Long,
        erikoisalaIds: List<Long>
    ) =
        valmistumispyyntoRepository.findByIdAndOpintooikeusYliopistoIdAndOpintooikeusErikoisalaIdIn(
            id,
            yliopistoId,
            erikoisalaIds
        ) ?: throw getValmistumispyyntoNotFoundException()

    private fun getValmistumispyyntoByOpintooikeusId(opintooikeusId: Long) =
        valmistumispyyntoRepository.findByOpintooikeusId(opintooikeusId) ?: throw EntityNotFoundException(
            VALMISTUMISPYYNTO_NOT_FOUND_ERROR
        )

    private fun getValmistumispyyntoNotFoundException() = EntityNotFoundException(
        "Valmistumispyyntöä ei löydy tai sinulla ei ole oikeuksia tarkastella kyseistä valmistumispyyntöä."
    )

    private fun getValmistumispyynnonTilaForArvioija(valmistumispyynto: Valmistumispyynto): ValmistumispyynnonTila {
        val isAvoin =
            valmistumispyynto.erikoistujanKuittausaika != null &&
                valmistumispyynto.vastuuhenkiloOsaamisenArvioijaKuittausaika == null &&
                valmistumispyynto.vastuuhenkiloOsaamisenArvioijaPalautusaika == null
        return fromValmistumispyyntoArvioija(valmistumispyynto, isAvoin)
    }

    private fun getValmistumispyynnonTilaForVirkailija(valmistumispyynto: Valmistumispyynto): ValmistumispyynnonTila {
        val isAvoin =
            valmistumispyynto.vastuuhenkiloOsaamisenArvioijaKuittausaika != null &&
                valmistumispyynto.virkailijanKuittausaika == null &&
                valmistumispyynto.virkailijanPalautusaika == null
        return fromValmistumispyyntoVirkailija(valmistumispyynto, isAvoin)
    }

    private fun getValmistumispyynnonTilaForHyvaksyja(valmistumispyynto: Valmistumispyynto): ValmistumispyynnonTila {
        val isAvoin =
            valmistumispyynto.virkailijanKuittausaika != null &&
                valmistumispyynto.vastuuhenkiloHyvaksyjaKuittausaika == null &&
                valmistumispyynto.vastuuhenkiloHyvaksyjaPalautusaika == null
        return fromValmistumispyyntoHyvaksyja(valmistumispyynto, isAvoin)
    }

    private fun mapValmistumispyyntoListItem(
        it: Valmistumispyynto,
        hyvaksyjaRole: ValmistumispyynnonHyvaksyjaRole,
        isAvoin: Boolean
    ): ValmistumispyyntoListItemDTO {
        val tila = getValmistumispyynnonTilaForHyvaksyja(it, hyvaksyjaRole, isAvoin)
        return ValmistumispyyntoListItemDTO(
            id = it.id,
            erikoistujanNimi = it.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi(),
            tila = tila,
            tapahtumanAjankohta = getPalautettuDateIfExists(it, tila) ?: it.muokkauspaiva,
            isAvoinForCurrentKayttaja = isAvoin
        )
    }

    private fun getPalautettuDateIfExists(
        valmistumispyynto: Valmistumispyynto,
        tila: ValmistumispyynnonTila
    ) = when (tila) {
        ValmistumispyynnonTila.VASTUUHENKILON_TARKASTUS_PALAUTETTU ->
            valmistumispyynto.vastuuhenkiloOsaamisenArvioijaPalautusaika
        ValmistumispyynnonTila.VIRKAILIJAN_TARKASTUS_PALAUTETTU ->
            valmistumispyynto.virkailijanPalautusaika
        ValmistumispyynnonTila.VASTUUHENKILON_HYVAKSYNTA_PALAUTETTU ->
            valmistumispyynto.vastuuhenkiloHyvaksyjaPalautusaika
        else -> null
    }

    private fun getValmistumispyynnonTilaForHyvaksyja(
        valmistumispyynto: Valmistumispyynto,
        hyvaksyjaRole: ValmistumispyynnonHyvaksyjaRole,
        isAvoin: Boolean
    ) = when (hyvaksyjaRole) {
        ValmistumispyynnonHyvaksyjaRole.VASTUUHENKILO_OSAAMISEN_ARVIOIJA ->
            fromValmistumispyyntoArvioija(valmistumispyynto, isAvoin)
        ValmistumispyynnonHyvaksyjaRole.VASTUUHENKILO_HYVAKSYJA ->
            fromValmistumispyyntoHyvaksyja(valmistumispyynto, isAvoin)
        ValmistumispyynnonHyvaksyjaRole.VIRKAILIJA -> fromValmistumispyyntoVirkailija(valmistumispyynto, isAvoin)
        ValmistumispyynnonHyvaksyjaRole.VASTUUHENKILO_OSAAMISEN_ARVIOIJA_HYVAKSYJA ->
            fromValmistumispyyntoArvioijaHyvaksyja(valmistumispyynto, isAvoin)
    }

    private fun getArviointienTila(id: Long, kayttaja: Kayttaja): ValmistumispyyntoArviointienTilaDTO {
        val yliopisto = getYliopisto(kayttaja)
        val valmistumispyynto = getValmistumispyyntoByYliopistoIdAndErikoisalaIdsOrThrow(
            id,
            yliopisto.id!!,
            getErikoisalaIds(kayttaja)
        )
        val opintooikeus = valmistumispyynto.opintooikeus
        val erikoistujanArvioivatKokonaisuudet = arvioitavaKokonaisuusRepository.findAllByErikoisalaIdAndValid(
            opintooikeus?.erikoisala?.id, opintooikeus?.osaamisenArvioinninOppaanPvm ?: LocalDate.now()
        )
        val erikoistujanTyoskentelyjaksot = tyoskentelyjaksoRepository.findAllByOpintooikeusId(opintooikeus?.id!!)
        val erikoistujanArvioinnit = erikoistujanTyoskentelyjaksot.map {
            suoritusarviointiRepository.findAllByTyoskentelyjaksoOpintooikeusId(opintooikeus.id!!)
        }.flatten()
        val erikoistujanArviointienArvioitavaKokonaisuusIds =
            erikoistujanArvioinnit.flatMap { it.arvioitavatKokonaisuudet.map { k -> k.arvioitavaKokonaisuus?.id!! } }.distinct()
        val arviointienTilaDTO = ValmistumispyyntoArviointienTilaDTO(
            hasArvioitaviaKokonaisuuksiaWithArviointiLowerThanFour = erikoistujanArvioinnit.flatMap { it.arvioitavatKokonaisuudet }.filter { it.arviointiasteikonTaso != null }
                .any {
                    it.arviointiasteikonTaso!! < ARVIOINTI_VAHINTAAN
                },
            hasArvioitaviaKokonaisuuksiaWithoutArviointi = erikoistujanArvioivatKokonaisuudet.any {
                it.id!! !in erikoistujanArviointienArvioitavaKokonaisuusIds
            }
        )

        return arviointienTilaDTO
    }

    private fun getValmistumispyynnonHyvaksyjaRoleForVastuuhenkilo(
        kayttaja: Kayttaja
    ): ValmistumispyynnonHyvaksyjaRole? {
        val vastuuhenkilonTehtavat =
            kayttaja.yliopistotAndErikoisalat.firstOrNull()?.vastuuhenkilonTehtavat?.map { it.nimi }
                ?: throw EntityNotFoundException(
                    KAYTTAJA_YLIOPISTO_ERIKOISALA_NOT_FOUND_ERROR
                )
        return if (
            vastuuhenkilonTehtavat.contains(VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI) &&
            vastuuhenkilonTehtavat.contains(VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_HYVAKSYNTA)
        )
            ValmistumispyynnonHyvaksyjaRole.VASTUUHENKILO_OSAAMISEN_ARVIOIJA_HYVAKSYJA
        else if (vastuuhenkilonTehtavat.contains(VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI))
            ValmistumispyynnonHyvaksyjaRole.VASTUUHENKILO_OSAAMISEN_ARVIOIJA
        else if (vastuuhenkilonTehtavat.contains(VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_HYVAKSYNTA))
            ValmistumispyynnonHyvaksyjaRole.VASTUUHENKILO_HYVAKSYJA
        else null
    }

    private fun getVastuuhenkiloOsaamisenArvioija(yliopistoId: Long, erikoisalaId: Long) =
        kayttajaRepository.findOneByAuthoritiesYliopistoErikoisalaAndVastuuhenkilonTehtavatyyppi(
            listOf(VASTUUHENKILO),
            yliopistoId,
            erikoisalaId,
            VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI
        ) ?: throw EntityNotFoundException("Vastuuhenkilöä, joka hyväksyisi osaamisen arvioinnin, ei löydy.")

    private fun getVastuuhenkiloHyvaksyja(yliopistoId: Long, erikoisalaId: Long) =
        kayttajaRepository.findOneByAuthoritiesYliopistoErikoisalaAndVastuuhenkilonTehtavatyyppi(
            listOf(VASTUUHENKILO),
            yliopistoId,
            erikoisalaId,
            VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_HYVAKSYNTA
        ) ?: throw EntityNotFoundException("Vastuuhenkilöä, joka hyväksyisi valmistumispyynnon, ei löydy.")

    private fun getVirkailijat(yliopistoId: Long) =
        kayttajaRepository.findAllByAuthoritiesAndRelYliopisto(
            listOf(OPINTOHALLINNON_VIRKAILIJA),
            yliopistoId
        )

    private fun sendMailNotificationUusiValmistumispyynto(
        vastuuhenkiloOsaamisenArvioijaUser: User,
        valmistumispyynto: Valmistumispyynto
    ) {
        mailService.sendEmailFromTemplate(
            vastuuhenkiloOsaamisenArvioijaUser,
            templateName = "uusivalmistumispyynto.html",
            titleKey = "email.uusivalmistumispyynto.title",
            properties = mapOf(
                Pair(
                    MailProperty.NAME,
                    valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.kayttaja?.user?.getName().toString()
                ), Pair(MailProperty.ID, valmistumispyynto.id!!.toString())
            )
        )
    }

    private fun sendMailNotificationOdottaaVirkailijanTarkastusta(
        erikoistujanYliopisto: YliopistoEnum,
        valmistumispyyntoId: Long
    ) {
        mailService.sendEmailFromTemplate(
            to = erikoistujanYliopisto.getOpintohallintoEmailAddress(applicationProperties),
            templateName = "valmistumispyyntoTarkastettavissa.html",
            titleKey = "email.valmistumispyyntoTarkastettavissa.title",
            properties = mapOf(Pair(MailProperty.ID, valmistumispyyntoId.toString()))
        )
    }

    private fun sendMailNotificationOdottaaHyvaksyntaa(
        valmistumispyynto: Valmistumispyynto
    ) {
        val opintooikeus = valmistumispyynto.opintooikeus
        mailService.sendEmailFromTemplate(
            getVastuuhenkiloHyvaksyja(opintooikeus?.yliopisto?.id!!, opintooikeus.erikoisala?.id!!).user!!,
            templateName = "valmistumispyyntoTarkastettavissa.html",
            titleKey = "email.valmistumispyyntoTarkastettavissa.title",
            properties = mapOf(Pair(MailProperty.ID, valmistumispyynto.id.toString()))
        )
    }

    private fun sendMailNotificationOsaamisenArvioijaPalauttanut(
        valmistumispyynto: Valmistumispyynto
    ) {
        mailService.sendEmailFromTemplate(
            valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.kayttaja?.user!!,
            templateName = "valmistumispyyntoPalautettuErikoistuja.html",
            titleKey = "email.valmistumispyyntoPalautettuErikoistuja.title",
            properties = mapOf()
        )
    }

    private fun sendMailNotificationVirkailijaPalauttanut(
        valmistumispyynto: Valmistumispyynto,
        osaamisenArvioija: Kayttaja?
    ) {
        mailService.sendEmailFromTemplate(
            valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.kayttaja?.user!!,
            templateName = "valmistumispyyntoPalautettuErikoistuja.html",
            titleKey = "email.valmistumispyyntoPalautettuErikoistuja.title",
            properties = mapOf()
        )

        val nimi = valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()

        mailService.sendEmailFromTemplate(
            osaamisenArvioija?.user!!,
            templateName = "valmistumispyyntoPalautettuMuut.html",
            titleKey = "email.valmistumispyyntoPalautettuMuut.title",
            titleProperties = arrayOf("$nimi"),
            properties = mapOf(Pair(MailProperty.NAME, nimi.toString()))
        )
    }

    private fun sendMailNotificationHyvaksyjaPalauttanut(
        valmistumispyynto: Valmistumispyynto
    ) {
        mailService.sendEmailFromTemplate(
            valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.kayttaja?.user!!,
            templateName = "valmistumispyyntoPalautettuErikoistuja.html",
            titleKey = "email.valmistumispyyntoPalautettuErikoistuja.title",
            properties = mapOf()
        )

        val nimi = valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()

        mailService.sendEmailFromTemplate(
            valmistumispyynto.virkailija?.user!!,
            templateName = "valmistumispyyntoPalautettuMuut.html",
            titleKey = "email.valmistumispyyntoPalautettuMuut.title",
            titleProperties = arrayOf("$nimi"),
            properties = mapOf(Pair(MailProperty.NAME, nimi.toString()))
        )

        mailService.sendEmailFromTemplate(
            valmistumispyynto.vastuuhenkiloOsaamisenArvioija?.user!!,
            templateName = "valmistumispyyntoPalautettuMuut.html",
            titleKey = "email.valmistumispyyntoPalautettuMuut.title",
            titleProperties = arrayOf("$nimi"),
            properties = mapOf(Pair(MailProperty.NAME, nimi.toString()))
        )
    }

    private fun mapValmistumispyynnonTarkistus(dto: ValmistumispyynnonTarkistusDTO): ValmistumispyynnonTarkistusDTO {
        dto.valmistumispyynto?.opintooikeusId?.let {
            dto.tyoskentelyjaksotTilastot = tyoskentelyjaksoService.getTilastot(it).koulutustyypit
            terveyskeskuskoulutusjaksonHyvaksyntaRepository.findByOpintooikeusId(it)?.let { hyvaksynta ->
                if (hyvaksynta.vastuuhenkiloHyvaksynyt) {
                    dto.terveyskeskustyoHyvaksyttyPvm = hyvaksynta.vastuuhenkilonKuittausaika
                }
                dto.terveyskeskustyoHyvaksyntaId = hyvaksynta.id
            }
            val opintosuoritukset = opintosuoritusRepository.findAllByOpintooikeusId(it)
            opintosuoritukset
                .firstOrNull { suoritus -> suoritus.tyyppi?.nimi == OpintosuoritusTyyppiEnum.TERVEYSKESKUSKOULUTUSJAKSO }
                ?.let { suoritus ->
                    dto.terveyskeskustyoOpintosuoritusId = suoritus.id
                }
            val teoriakoulutukset = teoriakoulutusRepository.findAllByOpintooikeusId(it)
            dto.teoriakoulutusSuoritettu =
                teoriakoulutukset.filter { koulutus -> koulutus.erikoistumiseenHyvaksyttavaTuntimaara != null }
                    .sumOf { koulutus -> koulutus.erikoistumiseenHyvaksyttavaTuntimaara!! }
            val opintooikeus = getOpintooikeus(it)
            dto.teoriakoulutusVaadittu =
                opintooikeus.opintoopas?.erikoisalanVaatimaTeoriakoulutustenVahimmaismaara
            val sateilysuojakoulutukset =
                opintosuoritukset.filter { suoritus -> suoritus.tyyppi?.nimi == OpintosuoritusTyyppiEnum.SATEILYSUOJAKOULUTUS }
            dto.sateilusuojakoulutusSuoritettu =
                sateilysuojakoulutukset.filter { suoritus -> suoritus.opintopisteet != null }
                    .sumOf { koulutus -> koulutus.opintopisteet!! }
            dto.sateilusuojakoulutusVaadittu =
                opintooikeus.opintoopas?.erikoisalanVaatimaSateilysuojakoulutustenVahimmaismaara
            val johtamisopinnot =
                opintosuoritukset.filter { suoritus -> suoritus.tyyppi?.nimi == OpintosuoritusTyyppiEnum.JOHTAMISOPINTO }
            dto.johtamiskoulutusSuoritettu =
                johtamisopinnot.filter { suoritus -> suoritus.opintopisteet != null }
                    .sumOf { suoritus -> suoritus.opintopisteet!! }
            dto.johtamiskoulutusVaadittu =
                opintooikeus.opintoopas?.erikoisalanVaatimaJohtamisopintojenVahimmaismaara
            dto.kuulustelut =
                opintosuoritukset.filter { suoritus -> suoritus.tyyppi?.nimi == OpintosuoritusTyyppiEnum.VALTAKUNNALLINEN_KUULUSTELU }
                    .map(opintosuoritusMapper::toDto)
            opintosuoritukset.firstOrNull { s -> s.tyyppi?.nimi == OpintosuoritusTyyppiEnum.KOEJAKSO && s.hyvaksytty }?.let { o ->
                dto.koejaksoHyvaksyttyPvm = o.suorituspaiva
            }
            koejaksonVastuuhenkilonArvioRepository.findByOpintooikeusId(it).orElse(null)?.let { arvio ->
                if (arvio.vastuuhenkiloHyvaksynyt) {
                    dto.koejaksoHyvaksyttyPvm = arvio.vastuuhenkilonKuittausaika
                }
            }
            val erikoisalaTyyppi = findErikoisalaTyyppiByOpintooikeusId(it)
            val vanhatSuorituksetDTO = findSuoritustenTila(it, erikoisalaTyyppi)
            dto.suoritustenTila = ValmistumispyyntoSuoritustenTilaDTO(
                erikoisalaTyyppi = erikoisalaTyyppi,
                vanhojaTyoskentelyjaksojaOrSuorituksiaExists = vanhatSuorituksetDTO.vanhojaTyoskentelyjaksojaOrSuorituksiaExists,
                kuulusteluVanhentunut = vanhatSuorituksetDTO.kuulusteluVanhentunut
            )
        }

        return dto
    }

    private fun luoYhteenvetoPdf(valmistumispyynnonTarkistusDTO: ValmistumispyynnonTarkistusDTO, valmistumispyynto: Valmistumispyynto) {
        val locale = Locale.forLanguageTag("fi")
        val context = Context(locale).apply {
            setVariable("tarkistus", valmistumispyynnonTarkistusDTO)
            setVariable("teoriakoulutusSuoritettu", valmistumispyynnonTarkistusDTO.teoriakoulutusSuoritettu)
            setVariable("teoriakoulutusVaadittu", valmistumispyynnonTarkistusDTO.teoriakoulutusVaadittu)
            setVariable("sateilusuojakoulutusSuoritettu", valmistumispyynnonTarkistusDTO.sateilusuojakoulutusSuoritettu)
            setVariable("sateilusuojakoulutusVaadittu", valmistumispyynnonTarkistusDTO.sateilusuojakoulutusVaadittu)
            setVariable("johtamiskoulutusSuoritettu", valmistumispyynnonTarkistusDTO.johtamiskoulutusSuoritettu)
            setVariable("johtamiskoulutusVaadittu", valmistumispyynnonTarkistusDTO.johtamiskoulutusVaadittu)

            valmistumispyynto.opintooikeus?.let {
                val tyoskentelyjaksotTilastot = tyoskentelyjaksoService.getTilastot(it)
                setVariable("tyoskentelyaikaYhteensa", daysToPeriod(tyoskentelyjaksotTilastot.koulutustyypit.yhteensaSuoritettu).format())
                setVariable("arvioErikoistumiseenHyvaksyttavista", daysToPeriod(tyoskentelyjaksotTilastot.arvioErikoistumiseenHyvaksyttavista).format())
                setVariable("arvioPuuttuvastaKoulutuksesta", daysToPeriod(tyoskentelyjaksotTilastot.arvioPuuttuvastaKoulutuksesta).format())
                setVariable("terveyskeskusSuoritettu", daysToPeriod(tyoskentelyjaksotTilastot.koulutustyypit.terveyskeskusSuoritettu).format())
                setVariable("terveyskeskusVaadittuVahintaan", daysToPeriod(tyoskentelyjaksotTilastot.koulutustyypit.terveyskeskusVaadittuVahintaan).format())
                setVariable("yliopistosairaalaSuoritettu", daysToPeriod(tyoskentelyjaksotTilastot.koulutustyypit.yliopistosairaalaSuoritettu).format())
                setVariable("yliopistosairaalaVaadittuVahintaan", daysToPeriod(tyoskentelyjaksotTilastot.koulutustyypit.yliopistosairaalaVaadittuVahintaan).format())
                setVariable("yliopistosairaaloidenUlkopuolinenSuoritettu", daysToPeriod(tyoskentelyjaksotTilastot.koulutustyypit.yliopistosairaaloidenUlkopuolinenSuoritettu).format())
                setVariable("yliopistosairaaloidenUlkopuolinenVaadittuVahintaan", daysToPeriod(tyoskentelyjaksotTilastot.koulutustyypit.yliopistosairaaloidenUlkopuolinenVaadittuVahintaan).format())
                setVariable("yhteensaSuoritettu", daysToPeriod(tyoskentelyjaksotTilastot.koulutustyypit.yhteensaSuoritettu).format())
                setVariable("yhteensaVaadittuVahintaan", daysToPeriod(tyoskentelyjaksotTilastot.koulutustyypit.yhteensaVaadittuVahintaan).format())

                val koulutusYhteensa = tyoskentelyjaksotTilastot.kaytannonKoulutus.sumOf { koulutus -> koulutus.suoritettu }
                tyoskentelyjaksotTilastot.kaytannonKoulutus.forEach { koulutus ->
                    when (koulutus.kaytannonKoulutus) {
                        KaytannonKoulutusTyyppi.OMAN_ERIKOISALAN_KOULUTUS -> {
                            setVariable("omaErikoisalaSuoritettu", daysToPeriod(koulutus.suoritettu).format())
                            setVariable("omaErikoisalaOsuus", (koulutus.suoritettu / koulutusYhteensa * 100).toInt())
                        }
                        KaytannonKoulutusTyyppi.OMAA_ERIKOISALAA_TUKEVA_KOULUTUS -> {
                            setVariable("omaaErikoisalaaTukevaSuoritettu", daysToPeriod(koulutus.suoritettu).format())
                            setVariable("omaErikoisalaaTukevaOsuus", (koulutus.suoritettu / koulutusYhteensa * 100).toInt())
                        }
                        KaytannonKoulutusTyyppi.TUTKIMUSTYO -> {
                            setVariable("tutkimustyoSuoritettu", daysToPeriod(koulutus.suoritettu).format())
                            setVariable("tutkimustyoOsuus", (koulutus.suoritettu / koulutusYhteensa * 100).toInt())
                        }
                        KaytannonKoulutusTyyppi.TERVEYSKESKUSTYO -> {
                            setVariable("terveyskeskustyoSuoritettu", daysToPeriod(koulutus.suoritettu).format())
                            setVariable("terveyskeskustyoOsuus", (koulutus.suoritettu / koulutusYhteensa * 100).toInt())
                        }
                    }
                }

                val tyoskentelyjaksot = tyoskentelyjaksoService.findAllByOpintooikeusId(it.id!!)
                setVariable("tyoskentelyjaksot", tyoskentelyjaksot.sortedByDescending { jakso -> jakso.alkamispaiva })
                setVariable("tyoskentelyjaksotSuoritettu", tyoskentelyjaksotTilastot.tyoskentelyjaksot.groupBy { jakso -> jakso.id }.mapValues { jakso -> daysToPeriod(jakso.value.sumOf { value -> value.suoritettu }).format() })

                val teoriakoulutukset = teoriakoulutusService.findAll(it.id!!)
                setVariable("teoriakoulutukset", teoriakoulutukset)
                setVariable("teoriakoulutusSuoritettuYhteensa", teoriakoulutukset.filter { koulutus -> koulutus.erikoistumiseenHyvaksyttavaTuntimaara != null }.sumOf { koulutus -> koulutus.erikoistumiseenHyvaksyttavaTuntimaara!! })
                setVariable("teoriakoulutusVaadittu", it.opintoopas?.erikoisalanVaatimaTeoriakoulutustenVahimmaismaara)

                val kategoriat = getArviointiKategoriat(it.id!!, true)
                setVariable("arvioinninKategoriat", kategoriat)

                val arviointiasteikonTasot = arviointiasteikkoService.findByOpintooikeusId(it.id!!)?.tasot
                setVariable("arviointiasteikonTasot", arviointiasteikonTasot)
            }
        }
        val outputStream = ByteArrayOutputStream()
        pdfService.luoPdf("pdf/valmistumisenyhteenveto.html", context, outputStream)
        val timestamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))

        val asiakirja = asiakirjaRepository.save(
            Asiakirja(
                opintooikeus = valmistumispyynto.opintooikeus,
                nimi = "valmistumisen_yhteenveto_${timestamp}.pdf",
                tyyppi = MediaType.APPLICATION_PDF_VALUE,
                lisattypvm = LocalDateTime.now(),
                asiakirjaData = AsiakirjaData(data = outputStream.toByteArray())
            )
        )

        valmistumispyynto.yhteenvetoAsiakirja = asiakirja
        valmistumispyyntoRepository.save(valmistumispyynto)

        if (!sarakesignService.getApiUrl(valmistumispyynto.opintooikeus?.yliopisto?.nimi!!)
                .isNullOrBlank()
        ) {
            lahetaAllekirjoitettavaksi(valmistumispyynto, asiakirja)
        }

    }

    private fun luoLiitteetPdf(valmistumispyynto: Valmistumispyynto) {
        valmistumispyynto.opintooikeus?.let {
            val tyoskentelyjaksot = tyoskentelyjaksoRepository.findAllByOpintooikeusId(it.id!!)

            val outputStream = ByteArrayOutputStream()
            pdfService.yhdistaAsiakirjat(tyoskentelyjaksot.flatMap { t -> t.asiakirjat }, outputStream)
            val timestamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))

            val asiakirja = asiakirjaRepository.save(
                Asiakirja(
                    opintooikeus = valmistumispyynto.opintooikeus,
                    nimi = "valmistumisen_yhteenvedon_liitteet_${timestamp}.pdf",
                    tyyppi = MediaType.APPLICATION_PDF_VALUE,
                    lisattypvm = LocalDateTime.now(),
                    asiakirjaData = AsiakirjaData(data = outputStream.toByteArray())
                )
            )

            valmistumispyynto.liitteetAsiakirja = asiakirja
            valmistumispyyntoRepository.save(valmistumispyynto)
        }
    }

    private fun luoErikoistujanTiedotPdf(valmistumispyynto: Valmistumispyynto) {
        if (valmistumispyynto.opintooikeus == null) {
            return
        }
        val opintooikeusId = valmistumispyynto.opintooikeus!!.id!!

        val outputStream = ByteArrayOutputStream()
        lisaaKoulutussuunnitelma(opintooikeusId, outputStream)
        lisaaArvioinnit(opintooikeusId, valmistumispyynto, outputStream)
        lisaaSuoritemerkinnat(opintooikeusId, valmistumispyynto, outputStream)
        lisaaPaivakirjamerkinnat(opintooikeusId, outputStream)
        lisaaSeurantajaksot(opintooikeusId, valmistumispyynto, outputStream)

        val timestamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))

        val asiakirja = asiakirjaRepository.save(
            Asiakirja(
                opintooikeus = valmistumispyynto.opintooikeus,
                nimi = "koulutussuunnitelma_ja_osaaminen_${timestamp}.pdf",
                tyyppi = MediaType.APPLICATION_PDF_VALUE,
                lisattypvm = LocalDateTime.now(),
                asiakirjaData = AsiakirjaData(data = outputStream.toByteArray())
            )
        )
        valmistumispyynto.erikoistujanTiedotAsiakirja = asiakirja
        valmistumispyyntoRepository.save(valmistumispyynto)
    }

    private fun lisaaKoulutussuunnitelma(opintooikeusId: Long, outputStream: ByteArrayOutputStream) {
        val koulutussuunitelma = koulutussuunnitelmaRepository.findOneByOpintooikeusId(opintooikeusId)
        val locale = Locale.forLanguageTag("fi")
        val context = Context(locale).apply {
            setVariable("koulutussuunnitelma", koulutussuunitelma)
        }
        pdfService.luoPdf("pdf/erikoistujantiedot/koulutussuunnitelma.html", context, outputStream)

        if (koulutussuunitelma?.motivaatiokirjeAsiakirja != null) {
            val inputStream = ByteArrayInputStream(outputStream.toByteArray())
            outputStream.reset()
            pdfService.yhdistaPdf(inputStream, ByteArrayInputStream(koulutussuunitelma.motivaatiokirjeAsiakirja?.asiakirjaData?.data), outputStream)
        }
    }

    private fun lisaaArvioinnit(opintooikeusId: Long, valmistumispyynto: Valmistumispyynto, outputStream: ByteArrayOutputStream) {
        val kategoriat = getArviointiKategoriat(opintooikeusId, false)
        val arviointiasteikko = valmistumispyynto.opintooikeus?.opintoopas?.arviointiasteikko
        val arviointiasteikonTasot = arviointiasteikko?.tasot?.associateBy { it.taso }
        val locale = Locale.forLanguageTag("fi")
        val context = Context(locale).apply {
            setVariable("arvioinninKategoriat", kategoriat)
            setVariable("arviointiasteikko", arviointiasteikko)
            setVariable("arviointiasteikonTasot", arviointiasteikonTasot)
        }
        val arvioinnitStream = ByteArrayOutputStream()
        pdfService.luoPdf("pdf/erikoistujantiedot/arvioinnit.html", context, arvioinnitStream)

        var result = ByteArrayInputStream(outputStream.toByteArray())
        var newPdf = ByteArrayInputStream(arvioinnitStream.toByteArray())
        outputStream.reset()
        pdfService.yhdistaPdf(result, newPdf, outputStream)

        suoritusarviointiRepository.findAllByTyoskentelyjaksoOpintooikeusId(opintooikeusId)
            .sortedWith(
            compareBy<Suoritusarviointi>(
                { a -> a.arvioitavatKokonaisuudet.minOf { k -> k.arvioitavaKokonaisuus?.kategoria?.nimi!! } },
                { a -> a.arvioitavatKokonaisuudet.minOf { k -> k.arvioitavaKokonaisuus?.nimi!! } }).thenByDescending { a -> a.tapahtumanAjankohta })
            .forEach { a ->
                val arviointiStream = ByteArrayOutputStream()
                pdfService.luoPdf(
                    "pdf/erikoistujantiedot/arviointi.html",
                    Context(locale).apply {
                        setVariable("arviointi", suoritusarviointiMapper.toDto(a))
                        setVariable("arviointiasteikonTasot", arviointiasteikonTasot)
                        setVariable("vaativuusTasot", getVaativuustasot())
                    },
                    arviointiStream
                )

                result = ByteArrayInputStream(outputStream.toByteArray())
                newPdf = ByteArrayInputStream(arviointiStream.toByteArray())
                outputStream.reset()
                pdfService.yhdistaPdf(result, newPdf, outputStream)

                a.arviointiAsiakirjat.forEach {
                    val inputStream = ByteArrayInputStream(outputStream.toByteArray())
                    outputStream.reset()
                    pdfService.yhdistaPdf(inputStream, ByteArrayInputStream(it.asiakirjaData?.data), outputStream)
                }

                a.itsearviointiAsiakirjat.forEach {
                    val inputStream = ByteArrayInputStream(outputStream.toByteArray())
                    outputStream.reset()
                    pdfService.yhdistaPdf(inputStream, ByteArrayInputStream(it.asiakirjaData?.data), outputStream)
                }
        }
    }

    private fun lisaaSuoritemerkinnat(opintooikeusId: Long, valmistumispyynto: Valmistumispyynto, outputStream: ByteArrayOutputStream) {
        val arviointiasteikko = valmistumispyynto.opintooikeus?.opintoopas?.arviointiasteikko
        val arviointiasteikonTasot = arviointiasteikko?.tasot?.associateBy { it.taso }
        val suoritemerkinnat = suoritemerkintaRepository.findAllByTyoskentelyjaksoOpintooikeusId(opintooikeusId).groupBy { it.suorite?.id }
        val suoritteenKategoriat = suoritteenKategoriaRepository.findAllByErikoisalaId(valmistumispyynto.opintooikeus?.erikoisala?.id!!).sortedBy { it.nimi }.map {
            SuoritteenKategoriaWithSuoritemerkinnatDTO(
                id = it.id,
                nimi = it.nimi,
                nimiSv = it.nimiSv,
                arviointiasteikko = valmistumispyynto.opintooikeus?.opintoopas?.arviointiasteikko?.nimi,
                suoritteet = it.suoritteet.sortedBy { s -> s.nimi }.map { s ->
                    SuoriteWithSuoritemerkinnatDTO(
                        id = s.id,
                        nimi = s.nimi,
                        nimiSv = s.nimiSv,
                        voimassaolonAlkamispaiva = s.voimassaolonAlkamispaiva,
                        voimassaolonPaattymispaiva = s.voimassaolonPaattymispaiva,
                        vaadittulkm = s.vaadittulkm,
                        suoritemerkinnat = suoritemerkinnat[s.id]?.sortedByDescending { m -> m.suorituspaiva }?.map(suoritemerkintaMapper::toDto) ?: listOf()
                    )
                },
                jarjestysnumero = it.jarjestysnumero
            )
        }
        val locale = Locale.forLanguageTag("fi")
        val context = Context(locale).apply {
            setVariable("suoritteenKategoriat", suoritteenKategoriat)
            setVariable("arviointiasteikko", arviointiasteikko)
            setVariable("arviointiasteikonTasot", arviointiasteikonTasot)
        }
        val suoritemerkinnatStream = ByteArrayOutputStream()
        pdfService.luoPdf("pdf/erikoistujantiedot/suoritemerkinnat.html", context, suoritemerkinnatStream)

        var result = ByteArrayInputStream(outputStream.toByteArray())
        var newPdf = ByteArrayInputStream(suoritemerkinnatStream.toByteArray())
        outputStream.reset()
        pdfService.yhdistaPdf(result, newPdf, outputStream)

        suoritteenKategoriat.forEach { k -> k.suoritteet?.forEach { ak -> ak.suoritemerkinnat?.forEach { m ->
            val suoritemerkintaStream = ByteArrayOutputStream()
            pdfService.luoPdf(
                "pdf/erikoistujantiedot/suoritemerkinta.html",
                Context(locale).apply {
                    setVariable("suoritemerkinta", m)
                    setVariable("arviointiasteikonTasot", arviointiasteikonTasot)
                    setVariable("vaativuusTasot", getVaativuustasot())
                },
                suoritemerkintaStream
            )

            result = ByteArrayInputStream(outputStream.toByteArray())
            newPdf = ByteArrayInputStream(suoritemerkintaStream.toByteArray())
            outputStream.reset()
            pdfService.yhdistaPdf(result, newPdf, outputStream)
        } } }
    }

    private fun lisaaPaivakirjamerkinnat(opintooikeusId: Long, outputStream: ByteArrayOutputStream) {
        val paivakirjamerkinnat = paivakirjamerkintaRepository.findAllByOpintooikeusId(opintooikeusId)
        val locale = Locale.forLanguageTag("fi")
        val context = Context(locale).apply {
            setVariable("paivakirjamerkinnat", paivakirjamerkinnat)
        }
        val paivakirjamerkinnatStream = ByteArrayOutputStream()
        pdfService.luoPdf("pdf/erikoistujantiedot/paivittaisetmerkinnat.html", context, paivakirjamerkinnatStream)

        val result = ByteArrayInputStream(outputStream.toByteArray())
        val newPdf = ByteArrayInputStream(paivakirjamerkinnatStream.toByteArray())
        outputStream.reset()
        pdfService.yhdistaPdf(result, newPdf, outputStream)
    }

    private fun lisaaSeurantajaksot(opintooikeusId: Long, valmistumispyynto: Valmistumispyynto, outputStream: ByteArrayOutputStream) {
        val seurantajaksot = seurantajaksoService.findByOpintooikeusId(opintooikeusId)
        val arviointiasteikko = valmistumispyynto.opintooikeus?.opintoopas?.arviointiasteikko
        val arviointiasteikonTasot = arviointiasteikko?.tasot?.associateBy { it.taso }
        val locale = Locale.forLanguageTag("fi")

        seurantajaksot.forEach {
            val jaksonTiedot = seurantajaksoService.findSeurantajaksonTiedot(it.opintooikeusId!!,
                it.alkamispaiva!!,
                it.paattymispaiva!!,
                it.koulutusjaksot?.map { k -> k.id!! }.orEmpty())
            val seurantajaksoStream = ByteArrayOutputStream()
            pdfService.luoPdf(
                "pdf/erikoistujantiedot/seurantajakso.html",
                Context(locale).apply {
                    setVariable("seurantajakso", it)
                    setVariable("seurantajaksonTiedot", jaksonTiedot)
                    setVariable("arviointiasteikko", arviointiasteikko)
                    setVariable("arviointiasteikonTasot", arviointiasteikonTasot)
                },
                seurantajaksoStream
            )

            val result = ByteArrayInputStream(outputStream.toByteArray())
            val newPdf = ByteArrayInputStream(seurantajaksoStream.toByteArray())
            outputStream.reset()
            pdfService.yhdistaPdf(result, newPdf, outputStream)
        }
    }

    private fun getVaativuustasot(): Map<Int, String> {
        return mapOf((1 to "ERITTAIN_HELPPO"), (2 to "MELKO_HELPPO"), (3 to "TAVANOMAINEN"), (4 to "MELKO_VAATIVA"), (5 to "ERITTAIN_VAATIVA"))
    }

    private fun getArviointiKategoriat(opintooikeusId: Long, korkeinArviointi: Boolean): List<ArvioitavanKokonaisuudenKategoriaWithArvioinnitDTO> {
        var suoritusarvioinnit = suoritusarviointiRepository.findAllByTyoskentelyjaksoOpintooikeusId(opintooikeusId).flatMap { it.arvioitavatKokonaisuudet }
        if (korkeinArviointi) {
            suoritusarvioinnit = suoritusarvioinnit.filter { arviointi -> arviointi.arviointiasteikonTaso != null }
        }

        val kokonaisuudetMap = suoritusarvioinnit.groupBy { arviointi -> arviointi.arvioitavaKokonaisuus }
        val kategoriatMap = kokonaisuudetMap.keys.sortedBy { k -> k?.kategoria?.nimi }.groupBy { k -> k?.kategoria }
        return kategoriatMap.entries.map { m ->
            ArvioitavanKokonaisuudenKategoriaWithArvioinnitDTO(
                id = m.key?.id,
                nimi = m.key?.nimi,
                nimiSv = m.key?.nimiSv,
                jarjestysnumero = m.key?.jarjestysnumero,
                arviointejaYhteensa = if (korkeinArviointi) m.value.size else m.value.sumOf { k -> kokonaisuudetMap[k]?.size ?: 0 },
                arvioitavatKokonaisuudet = m.value.sortedBy { k -> k?.nimi }.map { k ->
                    ArvioitavaKokonaisuusWithArvioinnitDTO(
                        id = k?.id,
                        nimi = k?.nimi,
                        nimiSv = k?.nimiSv,
                        kuvaus = k?.kuvaus,
                        kuvausSv = k?.kuvausSv,
                        voimassaoloAlkaa = k?.voimassaoloAlkaa,
                        voimassaoloLoppuu = k?.voimassaoloLoppuu,
                        suoritusarvioinnit = if (korkeinArviointi) listOf(kokonaisuudetMap[k]?.sortedByDescending { a -> a.suoritusarviointi?.tapahtumanAjankohta }
                            ?.maxByOrNull { a -> a.arviointiasteikonTaso!! }).map { a ->
                            SuoritusarviointiByKokonaisuusDTO(
                                id = a?.suoritusarviointi?.id,
                                tapahtumanAjankohta = a?.suoritusarviointi?.tapahtumanAjankohta,
                                arvioitavaTapahtuma = a?.suoritusarviointi?.arvioitavaTapahtuma,
                                arviointiasteikonTaso = a?.arviointiasteikonTaso,
                            arvioinninAntaja = kayttajaMapper.toDto(a?.suoritusarviointi?.arvioinninAntaja!!),
                            tyoskentelyjakso = tyoskentelyjaksoMapper.toDto(a.suoritusarviointi?.tyoskentelyjakso!!))
                        } else kokonaisuudetMap[k]?.sortedByDescending { a -> a.suoritusarviointi?.tapahtumanAjankohta }
                            ?.map { a ->
                                val result = SuoritusarviointiByKokonaisuusDTO(
                                    id = a.suoritusarviointi?.id,
                                    tapahtumanAjankohta = a.suoritusarviointi?.tapahtumanAjankohta,
                                    arvioitavaTapahtuma = a.suoritusarviointi?.arvioitavaTapahtuma,
                                    arviointiasteikonTaso = a.arviointiasteikonTaso,
                                    itsearviointiArviointiasteikonTaso = a.itsearviointiArviointiasteikonTaso,
                                    arvioinninAntaja = kayttajaMapper.toDto(a.suoritusarviointi?.arvioinninAntaja!!),
                                    arvioinninSaaja = kayttajaMapper.toDto(a.suoritusarviointi?.tyoskentelyjakso?.opintooikeus?.erikoistuvaLaakari?.kayttaja!!),
                                    tyoskentelyjakso = tyoskentelyjaksoMapper.toDto(a.suoritusarviointi?.tyoskentelyjakso!!),
                                    arviointiAsiakirjat = a.suoritusarviointi?.arviointiAsiakirjat?.map { asiakirja -> asiakirjaMapper.toDto(asiakirja) },
                                    itsearviointiAsiakirjat = a.suoritusarviointi?.itsearviointiAsiakirjat?.map { asiakirja -> asiakirjaMapper.toDto(asiakirja) })
                                result
                            }
                    )
                }
            )
        }
    }

    private fun lahetaAllekirjoitettavaksi(
        valmistumispyynto: Valmistumispyynto,
        asiakirja: Asiakirja
    ) {
        val recipients: MutableList<SarakeSignRecipientDTO> = mutableListOf()
        valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.kayttaja?.user?.let {
            recipients.add(lisaaVastaanottaja(it, true))
        }
        recipients.add(
            SarakeSignRecipientDTO(
                phaseNumber = 0,
                recipient = valmistumispyynto.opintooikeus?.yliopisto?.nimi?.getOpintohallintoEmailAddress(
                    applicationProperties
                ),
                readonly = true
            )
        )

        valmistumispyynto.vastuuhenkiloHyvaksyja?.user?.let { recipients.add(lisaaVastaanottaja(it, false)) }

        valmistumispyynto.sarakeSignRequestId = sarakesignService.lahetaAllekirjoitettavaksi(
            "Valmistumisen yhteenveto - " + valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi(),
            recipients,
            asiakirja.id!!,
            valmistumispyynto.opintooikeus?.yliopisto?.nimi!!
        )
        valmistumispyyntoRepository.save(valmistumispyynto)
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

    private fun daysToPeriod(days: Double): Period {
        return Period.of(days.toYears(), days.toMonths(), days.toDays())
    }
}
