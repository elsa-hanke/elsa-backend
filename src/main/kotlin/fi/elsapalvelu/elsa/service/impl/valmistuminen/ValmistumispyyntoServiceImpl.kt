package fi.elsapalvelu.elsa.service.impl.valmistuminen

import fi.elsapalvelu.elsa.config.YEK_ERIKOISALA_ID
import fi.elsapalvelu.elsa.domain.perustiedot.ErikoisalaTyyppi
import fi.elsapalvelu.elsa.domain.perustiedot.VastuuhenkilonTehtavatyyppiEnum
import fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto
import fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto.Companion.fromValmistumispyyntoErikoistuja
import fi.elsapalvelu.elsa.repository.valmistuminen.ValmistumispyynnonTarkistusRepository
import fi.elsapalvelu.elsa.repository.valmistuminen.ValmistumispyyntoRepository
import fi.elsapalvelu.elsa.required
import fi.elsapalvelu.elsa.service.constants.ERIKOISALA_NOT_FOUND_ERROR
import fi.elsapalvelu.elsa.service.criteria.NimiErikoisalaAndAvoinCriteria
import fi.elsapalvelu.elsa.service.dto.enumeration.ValmistumispyynnonTila
import fi.elsapalvelu.elsa.service.dto.kayttaja.AsiakirjaDTO
import fi.elsapalvelu.elsa.service.dto.suoritteet.VanhentuneetSuorituksetDTO
import fi.elsapalvelu.elsa.service.dto.valmistuminen.UusiValmistumispyyntoDTO
import fi.elsapalvelu.elsa.service.dto.valmistuminen.ValmistumispyynnonTarkistusDTO
import fi.elsapalvelu.elsa.service.dto.valmistuminen.ValmistumispyynnonTarkistusUpdateDTO
import fi.elsapalvelu.elsa.service.dto.valmistuminen.ValmistumispyyntoArviointienTilaDTO
import fi.elsapalvelu.elsa.service.dto.valmistuminen.ValmistumispyyntoDTO
import fi.elsapalvelu.elsa.service.dto.valmistuminen.ValmistumispyyntoHyvaksyntaFormDTO
import fi.elsapalvelu.elsa.service.dto.valmistuminen.ValmistumispyyntoListItemDTO
import fi.elsapalvelu.elsa.service.dto.valmistuminen.ValmistumispyyntoOsaamisenArviointiDTO
import fi.elsapalvelu.elsa.service.dto.valmistuminen.ValmistumispyyntoOsaamisenArviointiFormDTO
import fi.elsapalvelu.elsa.service.kayttaja.ErikoistuvaLaakariService
import fi.elsapalvelu.elsa.service.mapper.valmistuminen.ValmistumispyynnonTarkistusMapper
import fi.elsapalvelu.elsa.service.mapper.valmistuminen.ValmistumispyynnonTarkistusUpdateMapper
import fi.elsapalvelu.elsa.service.mapper.valmistuminen.ValmistumispyyntoMapper
import fi.elsapalvelu.elsa.service.mapper.valmistuminen.ValmistumispyyntoOsaamisenArviointiMapper
import fi.elsapalvelu.elsa.service.valmistuminen.ValmistumispyyntoService
import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.Clock
import java.time.LocalDate

@Suppress("TooManyFunctions") // ValmistumispyyntoService declares 22 operations.
@Service
@Transactional
class ValmistumispyyntoServiceImpl(
    private val valmistumispyyntoRepository: ValmistumispyyntoRepository,
    private val valmistumispyyntoQueryService: ValmistumispyyntoQueryService,
    private val valmistumispyyntoMapper: ValmistumispyyntoMapper,
    private val valmistumispyyntoOsaamisenArviointiMapper: ValmistumispyyntoOsaamisenArviointiMapper,
    private val clock: Clock,
    private val valmistumispyynnonTarkistusRepository: ValmistumispyynnonTarkistusRepository,
    private val valmistumispyynnonTarkistusMapper: ValmistumispyynnonTarkistusMapper,
    private val valmistumispyynnonTarkistusUpdateMapper: ValmistumispyynnonTarkistusUpdateMapper,
    private val erikoistuvaLaakariService: ErikoistuvaLaakariService,
    private val vanhentumisService: ValmistumispyynnonVanhentumisService,
    private val tilaService: ValmistumispyynnonTilaService,
    private val osapuoliService: ValmistumispyynnonOsapuoliService,
    private val arviointienTilaService: ValmistumispyynnonArviointienTilaService,
    private val asiakirjaService: ValmistumispyynnonAsiakirjaService,
    private val viimeistelyService: ValmistumispyynnonViimeistelyService,
    private val ilmoitusService: ValmistumispyynnonIlmoitusService,
    private val tarkistusService: ValmistumispyynnonTarkistusService
) : ValmistumispyyntoService {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(readOnly = true)
    override fun findErikoisalaTyyppiByOpintooikeusId(opintooikeusId: Long): ErikoisalaTyyppi =
        osapuoliService.haeOpintooikeus(opintooikeusId).erikoisala?.tyyppi
            ?: throw EntityNotFoundException(ERIKOISALA_NOT_FOUND_ERROR)

    @Transactional(readOnly = true)
    override fun findOneByOpintooikeusId(opintooikeusId: Long): ValmistumispyyntoDTO? {
        val valmistumispyynto = valmistumispyyntoRepository.findByOpintooikeusId(opintooikeusId)
        val tila = fromValmistumispyyntoErikoistuja(valmistumispyynto)
        val opintooikeus = osapuoliService.haeOpintooikeus(opintooikeusId)
        val yliopistoId = opintooikeus.yliopisto?.id.required()
        val erikoisalaId = opintooikeus.erikoisala?.id.required()
        val vastuuhenkiloOsaamisenarvioija =
            if (erikoisalaId != YEK_ERIKOISALA_ID) osapuoliService.haeOsaamisenArvioija(
                yliopistoId,
                erikoisalaId
            ) else null
        val vastuuhenkiloHyvaksyja = osapuoliService.haeHyvaksyja(
            yliopistoId,
            erikoisalaId
        )
        val valmistumispyyntoDTO =
            valmistumispyynto?.let { valmistumispyyntoMapper.toDto(it) } ?: ValmistumispyyntoDTO().apply {
                erikoistujanLaillistamispaiva = opintooikeus.erikoistuvaLaakari?.laillistamispaiva
                erikoistujanLaillistamistodistus = opintooikeus.erikoistuvaLaakari?.laillistamistodistus?.data
                erikoistujanLaillistamistodistusNimi =
                    opintooikeus.erikoistuvaLaakari?.laillistamispaivanLiitetiedostonNimi
                erikoistujanLaillistamistodistusTyyppi =
                    opintooikeus.erikoistuvaLaakari?.laillistamispaivanLiitetiedostonTyyppi
            }

        return valmistumispyyntoDTO.apply {
            this.tila = tila
            vastuuhenkiloOsaamisenArvioijaNimi = vastuuhenkiloOsaamisenarvioija?.getNimi()
            vastuuhenkiloOsaamisenArvioijaNimike = vastuuhenkiloOsaamisenarvioija?.nimike
            vastuuhenkiloHyvaksyjaNimi = vastuuhenkiloHyvaksyja.getNimi()
            vastuuhenkiloHyvaksyjaNimike = vastuuhenkiloHyvaksyja.nimike
            arkistoitava = viimeistelyService.onkoArkistointiKaytossa(opintooikeus.yliopisto?.nimi)
        }
    }

    @Transactional(readOnly = true)
    override fun findSuoritustenTila(
        opintooikeusId: Long,
        erikoisalaTyyppi: ErikoisalaTyyppi
    ): VanhentuneetSuorituksetDTO =
        vanhentumisService.haeVanhentuneetSuoritukset(opintooikeusId, erikoisalaTyyppi)

    override fun create(
        opintooikeusId: Long,
        uusiValmistumispyyntoDTO: UusiValmistumispyyntoDTO
    ): ValmistumispyyntoDTO {
        tarkistusService.validateValmistumispyyntoPdfText(uusiValmistumispyyntoDTO)
        osapuoliService.haeOpintooikeus(opintooikeusId).let { opintooikeus ->
            osapuoliService.paivitaYhteystiedot(
                opintooikeus.erikoistuvaLaakari?.kayttaja?.user,
                uusiValmistumispyyntoDTO.erikoistujanSahkoposti,
                uusiValmistumispyyntoDTO.erikoistujanPuhelinnumero
            )
            val valmistumispyynto = Valmistumispyynto(
                opintooikeus = opintooikeus,
                selvitysVanhentuneistaSuorituksista = uusiValmistumispyyntoDTO.selvitysVanhentuneistaSuorituksista,
                erikoistujanKuittausaika = LocalDate.now()
            )
            valmistumispyyntoRepository.save(valmistumispyynto).let { saved ->
                if (saved.opintooikeus?.erikoisala?.id != YEK_ERIKOISALA_ID) {
                    val vastuuhenkiloOsaamisenArvioijaUser =
                        osapuoliService.haeOsaamisenArvioija(
                        opintooikeus.yliopisto?.id.required(),
                        opintooikeus.erikoisala?.id.required()
                    ).user.required()
                    ilmoitusService.lahetaIlmoitusUudestaValmistumispyynnosta(
                        vastuuhenkiloOsaamisenArvioijaUser,
                        saved
                    )
                } else {
                    ilmoitusService.lahetaIlmoitusVirkailijanTarkastuksesta(saved)
                }
                return valmistumispyyntoMapper.toDto(saved)
                    .apply { tila = ValmistumispyynnonTila.ODOTTAA_VASTUUHENKILON_TARKASTUSTA }
            }
        }
    }

    override fun update(
        opintooikeusId: Long,
        uusiValmistumispyyntoDTO: UusiValmistumispyyntoDTO
    ): ValmistumispyyntoDTO {
        tarkistusService.validateValmistumispyyntoPdfText(uusiValmistumispyyntoDTO)
        osapuoliService.haeOpintooikeus(opintooikeusId).let { opintooikeus ->
            osapuoliService.paivitaYhteystiedot(
                opintooikeus.erikoistuvaLaakari?.kayttaja?.user,
                uusiValmistumispyyntoDTO.erikoistujanSahkoposti,
                uusiValmistumispyyntoDTO.erikoistujanPuhelinnumero
            )
            osapuoliService.haeValmistumispyyntoOpintooikeudella(opintooikeusId).apply {
                vastuuhenkiloOsaamisenArvioijaKorjausehdotus = null
                vastuuhenkiloOsaamisenArvioijaPalautusaika = null
                erikoistujanKuittausaika = LocalDate.now()
                this.selvitysVanhentuneistaSuorituksista = uusiValmistumispyyntoDTO.selvitysVanhentuneistaSuorituksista

                if (opintooikeus.erikoisala?.id == YEK_ERIKOISALA_ID) {
                    virkailijanPalautusaika = null
                } else if (vastuuhenkiloOsaamisenArvioijaKuittausaika != null) {
                    virkailijanPalautusaika = null
                    ilmoitusService.lahetaIlmoitusVirkailijanTarkastuksesta(this)
                }
            }.let {
                valmistumispyyntoRepository.save(it).let { saved ->
                    if (it.opintooikeus?.erikoisala?.id == YEK_ERIKOISALA_ID) {
                        ilmoitusService.lahetaIlmoitusVirkailijanTarkastuksesta(saved)
                    } else if (saved.vastuuhenkiloOsaamisenArvioijaKuittausaika == null) {
                        val vastuuhenkiloOsaamisenArvioijaUser =
                            osapuoliService.haeOsaamisenArvioija(
                                opintooikeus.yliopisto?.id.required(),
                                opintooikeus.erikoisala?.id.required()
                            ).user.required()
                        ilmoitusService.lahetaIlmoitusUudestaValmistumispyynnosta(
                            vastuuhenkiloOsaamisenArvioijaUser,
                            saved
                        )
                    }
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
        val kayttaja = osapuoliService.haeKayttaja(userId)
        val yliopisto = osapuoliService.haeYliopisto(kayttaja)

        val valmistumispyynto = osapuoliService.haeValmistumispyynto(
            id,
            kayttaja,
            yliopisto.id.required(),
            VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI
        )

        valmistumispyynto.vastuuhenkiloOsaamisenArvioija = kayttaja

        if (osaamisenArviointiDTO.osaaminenRiittavaValmistumiseen == true) {
            valmistumispyynto.vastuuhenkiloOsaamisenArvioijaKuittausaika = LocalDate.now()
            valmistumispyynto.virkailijanPalautusaika = null
            ilmoitusService.lahetaIlmoitusVirkailijanTarkastuksesta(valmistumispyynto)
        } else {
            valmistumispyynto.vastuuhenkiloOsaamisenArvioijaPalautusaika = LocalDate.now()
            valmistumispyynto.vastuuhenkiloOsaamisenArvioijaKorjausehdotus = osaamisenArviointiDTO.korjausehdotus
            valmistumispyynto.erikoistujanKuittausaika = null
            ilmoitusService.lahetaIlmoitusPalautuksesta(valmistumispyynto)
        }

        return valmistumispyyntoMapper.toDto(valmistumispyynto).apply {
            tila = tilaService.haeTilaOsaamisenArvioijalle(valmistumispyynto)
        }
    }

    override fun updateValmistumispyyntoByHyvaksyjaUserId(id: Long, userId: String, hyvaksyntaFormDTO: ValmistumispyyntoHyvaksyntaFormDTO): ValmistumispyynnonTarkistusDTO {
        log.info("Hyvaksynta-operaatio aloitettu [valmistumispyyntoId=$id]")

        val kayttaja = osapuoliService.haeKayttaja(userId)
        val yliopisto = osapuoliService.haeYliopisto(kayttaja)
        log.info("Kayttaja ja yliopisto haettu [valmistumispyyntoId=$id, yliopistoId=${yliopisto.id}]")

        val valmistumispyynto = osapuoliService.haeValmistumispyynto(
            id,
            kayttaja,
            yliopisto.id.required(),
            VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_HYVAKSYNTA
        )
        log.info("Valmistumispyynto haettu [valmistumispyyntoId=$id]")

        osapuoliService.paivitaYhteystiedot(
            kayttaja.user,
            hyvaksyntaFormDTO.sahkoposti,
            hyvaksyntaFormDTO.puhelinnumero
        )
        log.info("Kayttajan yhteystiedot paivitetty [valmistumispyyntoId=$id]")

        valmistumispyynto.vastuuhenkiloHyvaksyja = kayttaja

        if (hyvaksyntaFormDTO.korjausehdotus != null) {
            log.info("Valmistumispyynto palautetaan erikoistujalle [valmistumispyyntoId=$id]")
            valmistumispyynto.vastuuhenkiloHyvaksyjaPalautusaika = LocalDate.now()
            valmistumispyynto.vastuuhenkiloHyvaksyjaKorjausehdotus = hyvaksyntaFormDTO.korjausehdotus
            valmistumispyynto.erikoistujanKuittausaika = null
            valmistumispyynto.virkailijanKuittausaika = null
            ilmoitusService.lahetaIlmoitusPalautuksesta(
                valmistumispyynto,
                ilmoitaYliopistolle = true
            )
            log.info("Palautussahkoposti lahetetty [valmistumispyyntoId=$id]")
        } else {
            log.info("Valmistumispyynto hyvaksytaan: tallennetaan kuittausaika [valmistumispyyntoId=$id]")
            valmistumispyynto.vastuuhenkiloHyvaksyjaKuittausaika = LocalDate.now()
            val result = valmistumispyyntoRepository.save(valmistumispyynto)
            log.info("Kuittausaika tallennettu [valmistumispyyntoId=$id]")

            result.valmistumispyynnonTarkistus?.let {
                viimeistelyService.viimeistele(
                    id,
                    it,
                    valmistumispyynto,
                    yliopisto.nimi
                )
            }
        }

        val valmistumispyynnonTarkistus = valmistumispyynnonTarkistusRepository.findByValmistumispyyntoId(valmistumispyynto.id.required())

        log.info("Hyvaksynta-operaatio valmis [valmistumispyyntoId=$id]")
        return valmistumispyynnonTarkistusMapper.toDto(valmistumispyynnonTarkistus.required()).apply {
            this.kommentitVirkailijoille = null
            this.valmistumispyynto?.tila = tilaService.haeTilaHyvaksyjalle(valmistumispyynto)
        }
    }


    override fun updateTarkistusByVirkailijaUserId(id: Long, userId: String, valmistumispyynnonTarkistusDTO: ValmistumispyynnonTarkistusUpdateDTO,
        laillistamistodistus: MultipartFile?): ValmistumispyynnonTarkistusDTO? {
        tarkistusService.validateVirkailijanPdfText(id, valmistumispyynnonTarkistusDTO)
        val kayttaja = osapuoliService.haeKayttaja(userId)
        val yliopisto = kayttaja.yliopistot.first()
        var tarkistus = valmistumispyynnonTarkistusRepository.findByValmistumispyyntoIdAndValmistumispyyntoOpintooikeusYliopistoId(id, yliopisto.id.required())

        if (tarkistus != null) {
            tarkistus.yekSuoritettu = valmistumispyynnonTarkistusDTO.yekSuoritettu
            tarkistus.yekSuorituspaiva = valmistumispyynnonTarkistusDTO.yekSuorituspaiva
            tarkistus.ptlSuoritettu = valmistumispyynnonTarkistusDTO.ptlSuoritettu
            tarkistus.ptlSuorituspaiva = valmistumispyynnonTarkistusDTO.ptlSuorituspaiva
            tarkistus.aiempiElKoulutusSuoritettu = valmistumispyynnonTarkistusDTO.aiempiElKoulutusSuoritettu
            tarkistus.aiempiElKoulutusSuorituspaiva = valmistumispyynnonTarkistusDTO.aiempiElKoulutusSuorituspaiva
            tarkistus.ltTutkintoSuoritettu = valmistumispyynnonTarkistusDTO.ltTutkintoSuoritettu
            tarkistus.ltTutkintoSuorituspaiva = valmistumispyynnonTarkistusDTO.ltTutkintoSuorituspaiva
            tarkistus.terveyskeskustyoTarkistettu = valmistumispyynnonTarkistusDTO.terveyskeskustyoTarkistettu
            tarkistus.yliopistosairaalanUlkopuolinenTyoTarkistettu = valmistumispyynnonTarkistusDTO.yliopistosairaalanUlkopuolinenTyoTarkistettu
            tarkistus.yliopistosairaalatyoTarkistettu = valmistumispyynnonTarkistusDTO.yliopistosairaalatyoTarkistettu
            tarkistus.kokonaistyoaikaTarkistettu = valmistumispyynnonTarkistusDTO.kokonaistyoaikaTarkistettu
            tarkistus.teoriakoulutusTarkistettu = valmistumispyynnonTarkistusDTO.teoriakoulutusTarkistettu
            tarkistus.kommentitVirkailijoille = valmistumispyynnonTarkistusDTO.kommentitVirkailijoille
            tarkistus.virkailijanYhteenveto = valmistumispyynnonTarkistusDTO.virkailijanYhteenveto
            tarkistus.koejaksoEiVaadittu = valmistumispyynnonTarkistusDTO.koejaksoEiVaadittu
        } else {
            valmistumispyyntoRepository.findByIdAndOpintooikeusYliopistoId(id, yliopisto.id.required())?.let {
                    tarkistus = valmistumispyynnonTarkistusUpdateMapper.toEntity(valmistumispyynnonTarkistusDTO)
                    tarkistus.id = null
                    tarkistus.valmistumispyynto = it
                }
        }

        tarkistus?.let {
            valmistumispyynnonTarkistusRepository.save(it)

            if (laillistamistodistus != null || valmistumispyynnonTarkistusDTO.laillistamispaiva != null) {
                erikoistuvaLaakariService.updateLaillistamispaiva(it.valmistumispyynto?.opintooikeus?.erikoistuvaLaakari?.kayttaja?.user?.id.required(),
                    valmistumispyynnonTarkistusDTO.laillistamispaiva, laillistamistodistus?.bytes, laillistamistodistus?.originalFilename, laillistamistodistus?.contentType)
            }

            if (valmistumispyynnonTarkistusDTO.keskenerainen != true) {
                it.valmistumispyynto?.virkailija = kayttaja

                if (valmistumispyynnonTarkistusDTO.korjausehdotus != null) {
                    it.valmistumispyynto?.virkailijanKorjausehdotus = valmistumispyynnonTarkistusDTO.korjausehdotus
                    it.valmistumispyynto?.virkailijanPalautusaika = LocalDate.now(clock)
                    it.valmistumispyynto?.erikoistujanKuittausaika = null
                    ilmoitusService.lahetaIlmoitusPalautuksesta(
                        it.valmistumispyynto.required()
                    )
                } else {
                    it.valmistumispyynto?.virkailijanSaate = valmistumispyynnonTarkistusDTO.lisatiedotVastuuhenkilolle
                    it.valmistumispyynto?.virkailijanKuittausaika = LocalDate.now(clock)
                    it.valmistumispyynto?.vastuuhenkiloHyvaksyjaKorjausehdotus = null
                    it.valmistumispyynto?.vastuuhenkiloHyvaksyjaPalautusaika = null
                    it.valmistumispyynto?.virkailijanKorjausehdotus = null
                    ilmoitusService.lahetaIlmoitusHyvaksyjalle(
                        it.valmistumispyynto.required()
                    )
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
        val kayttaja = osapuoliService.haeKayttaja(userId)
        val yliopisto = osapuoliService.haeYliopisto(kayttaja)
        val yek = valmistumispyyntoCriteria.erikoisalaId?.equals == YEK_ERIKOISALA_ID
        val hyvaksyjaRole = tilaService.haeVastuuhenkilonRoolit(kayttaja, yek)
        viimeistelyService.onkoArkistointiKaytossa(yliopisto.nimi)
        return valmistumispyyntoQueryService.findValmistumispyynnotByCriteria(
            valmistumispyyntoCriteria,
            hyvaksyjaRole,
            pageable,
            yliopisto.id.required(),
            if (yek) listOf(YEK_ERIKOISALA_ID) else osapuoliService.haeErikoisalaIds(kayttaja),
            kayttaja.user?.langKey
        ).map {
            val isAvoin = valmistumispyyntoCriteria.avoin == true
            tilaService.mapVastuuhenkilonListItem(it, hyvaksyjaRole, isAvoin)
        }
    }

    override fun findAllForVirkailijaByCriteria(
        userId: String,
        valmistumispyyntoCriteria: NimiErikoisalaAndAvoinCriteria,
        erikoisalaIds: List<Long>,
        excludedErikoisalaIds: List<Long>,
        pageable: Pageable
    ): Page<ValmistumispyyntoListItemDTO> {
        val kayttaja = osapuoliService.haeKayttaja(userId)
        return valmistumispyyntoQueryService.findValmistumispyynnotByCriteriaForVirkailija(
            valmistumispyyntoCriteria,
            pageable,
            kayttaja.yliopistot.first().id.required(),
            erikoisalaIds,
            excludedErikoisalaIds,
            kayttaja.user?.langKey
        ).map {
            val isAvoin = valmistumispyyntoCriteria.avoin == true
            tilaService.mapVirkailijanListItem(it, isAvoin)
        }
    }

    @Transactional(readOnly = true)
    override fun findOneByIdAndVastuuhenkiloOsaamisenArvioijaUserId(
        id: Long,
        userId: String
    ): ValmistumispyyntoOsaamisenArviointiDTO? {
        val kayttaja = osapuoliService.haeKayttaja(userId)
        val yliopisto = osapuoliService.haeYliopisto(kayttaja)
        val valmistumispyynto = osapuoliService.haeValmistumispyynto(
            id,
            kayttaja,
            yliopisto.id.required(),
            VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI
        )

        return valmistumispyyntoOsaamisenArviointiMapper.toDto(valmistumispyynto).apply {
            tila = tilaService.haeTilaOsaamisenArvioijalle(valmistumispyynto)
        }
    }

    override fun findOneByIdAndVirkailijaUserId(
        id: Long,
        userId: String
    ): ValmistumispyynnonTarkistusDTO? {
        val kayttaja = osapuoliService.haeKayttaja(userId)
        val yliopisto = kayttaja.yliopistot.first()
        valmistumispyynnonTarkistusRepository.findByValmistumispyyntoIdAndValmistumispyyntoOpintooikeusYliopistoId(
            id,
            yliopisto.id.required()
        )?.let {
            val result = tarkistusService.taydenna(valmistumispyynnonTarkistusMapper.toDto(it))
            it.valmistumispyynto?.let { pyynto ->
                result.valmistumispyynto?.tila = tilaService.haeTilaVirkailijalle(pyynto)
            }
            return result
        }

        val valmistumispyynto =
            valmistumispyyntoRepository.findByIdAndOpintooikeusYliopistoId(id, yliopisto.id.required())
                ?: throw osapuoliService.valmistumispyyntoaEiLoydy()

        return tarkistusService.taydenna(ValmistumispyynnonTarkistusDTO(
            valmistumispyynto = valmistumispyyntoMapper.toDto(
                valmistumispyynto
            ).apply { tila = tilaService.haeTilaVirkailijalle(valmistumispyynto) }
        ))
    }

    override fun findOneByIdAndVastuuhenkiloHyvaksyjaUserId(
        id: Long,
        userId: String
    ): ValmistumispyynnonTarkistusDTO? {
        val kayttaja = osapuoliService.haeKayttaja(userId)
        val yliopisto = osapuoliService.haeYliopisto(kayttaja)
        val tarkistus = valmistumispyynnonTarkistusRepository.findByValmistumispyyntoIdForHyvaksyja(id, yliopisto.id.required())
            ?: throw osapuoliService.valmistumispyyntoaEiLoydy()
        val yek = tarkistus.valmistumispyynto?.opintooikeus?.erikoisala?.id == YEK_ERIKOISALA_ID
        if (tilaService.haeVastuuhenkilonRoolit(kayttaja, yek).isEmpty()) {
            throw osapuoliService.valmistumispyyntoaEiLoydy()
        }

        if (!yek && !osapuoliService.haeErikoisalaIds(kayttaja).contains(
                tarkistus.valmistumispyynto?.opintooikeus
                    ?.erikoisala?.id
            )
        ) {
            throw osapuoliService.valmistumispyyntoaEiLoydy()
        }

        val result = tarkistusService.taydenna(valmistumispyynnonTarkistusMapper.toDto(tarkistus))
        result.kommentitVirkailijoille = null
        tarkistus.valmistumispyynto?.let { pyynto ->
            result.valmistumispyynto?.tila = tilaService.haeTilaHyvaksyjalle(pyynto)
            result.valmistumispyynto?.arkistoitava =
                viimeistelyService.onkoArkistointiKaytossa(yliopisto.nimi)
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
        val kayttaja = osapuoliService.haeKayttaja(userId)
        val yliopisto = osapuoliService.haeYliopisto(kayttaja)
        val valmistumispyynto = osapuoliService.haeValmistumispyynto(
            id,
            kayttaja,
            yliopisto.id.required(),
            VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI
        )
        return arviointienTilaService.haeArviointienTila(valmistumispyynto)
    }

    override fun getValmistumispyynnonAsiakirja(
        userId: String,
        valmistumispyyntoId: Long,
        asiakirjaId: Long
    ): AsiakirjaDTO? = asiakirjaService.haeValmistumispyynnonAsiakirja(
        userId,
        valmistumispyyntoId,
        asiakirjaId
    )

    override fun getValmistumispyynnonAsiakirjaVirkailija(
        valmistumispyyntoId: Long,
        yliopistoId: Long?,
        asiakirjaId: Long
    ): AsiakirjaDTO? = asiakirjaService.haeValmistumispyynnonAsiakirjaVirkailijalle(
        valmistumispyyntoId,
        yliopistoId,
        asiakirjaId
    )

    override fun getValmistumispyynnonTyoskentelyjaksoAsiakirja(
        userId: String,
        valmistumispyyntoId: Long,
        asiakirjaId: Long
    ): AsiakirjaDTO? = asiakirjaService.haeTyoskentelyjaksonAsiakirja(
        userId,
        valmistumispyyntoId,
        asiakirjaId
    )

    override fun onkoLahetetty(opintooikeusId: Long): Boolean {
        val valmistumispyynto = valmistumispyyntoRepository.findByOpintooikeusId(opintooikeusId)
        return valmistumispyynto?.erikoistujanKuittausaika != null
    }

    override fun onkoAvoinOsaamisenTarkistaminen(userId: String, id: Long): Boolean =
        osapuoliService.onkoOsaamisenArviointiAvoin(userId, id)

    override fun onkoAvoinVirkailija(userId: String, id: Long): Boolean =
        osapuoliService.onkoVirkailijanTarkistusAvoin(userId, id)

    override fun onkoAvoinHyvaksyja(userId: String, id: Long): Boolean =
        osapuoliService.onkoLopullinenHyvaksyntaAvoin(userId, id)

}
