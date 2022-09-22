package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.domain.Kayttaja
import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.domain.Valmistumispyynto
import fi.elsapalvelu.elsa.domain.Valmistumispyynto.Companion.fromValmistumispyyntoArvioija
import fi.elsapalvelu.elsa.domain.Valmistumispyynto.Companion.fromValmistumispyyntoArvioijaHyvaksyja
import fi.elsapalvelu.elsa.domain.Valmistumispyynto.Companion.fromValmistumispyyntoErikoistuja
import fi.elsapalvelu.elsa.domain.Valmistumispyynto.Companion.fromValmistumispyyntoHyvaksyja
import fi.elsapalvelu.elsa.domain.Valmistumispyynto.Companion.fromValmistumispyyntoVirkailija
import fi.elsapalvelu.elsa.domain.enumeration.*
import fi.elsapalvelu.elsa.repository.*
import fi.elsapalvelu.elsa.security.OPINTOHALLINNON_VIRKAILIJA
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.service.MailProperty
import fi.elsapalvelu.elsa.service.MailService
import fi.elsapalvelu.elsa.service.ValmistumispyyntoService
import fi.elsapalvelu.elsa.service.constants.*
import fi.elsapalvelu.elsa.service.criteria.NimiErikoisalaAndAvoinCriteria
import fi.elsapalvelu.elsa.service.dto.*
import fi.elsapalvelu.elsa.service.dto.enumeration.ValmistumispyynnonHyvaksyjaRole
import fi.elsapalvelu.elsa.service.dto.enumeration.ValmistumispyynnonTila
import fi.elsapalvelu.elsa.service.mapper.ValmistumispyyntoMapper
import fi.elsapalvelu.elsa.service.mapper.ValmistumispyyntoOsaamisenArviointiMapper
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate
import javax.persistence.EntityNotFoundException

private const val VANHENTUNUT_KUULUSTELU_YEARS = 4L
private const val VANHENTUNUT_SUORITUS_YEARS_EL = 10L
private const val VANHENTUNUT_SUORITUS_YEARS_EHL = 6L
private const val ARVIOINTI_VAHINTAAN = 4

@Service
class ValmistumispyyntoServiceImpl(
    private val valmistumispyyntoRepository: ValmistumispyyntoRepository,
    private val valmistumispyyntoQueryService: ValmistumispyyntoQueryService,
    private val arvioitavaKokonaisuusRepository: ArvioitavaKokonaisuusRepository,
    private val suoritusarviointiRepository: SuoritusarviointiRepository,
    private val kayttajaRepository: KayttajaRepository,
    private val valmistumispyyntoMapper: ValmistumispyyntoMapper,
    private val valmistumispyyntoOsaamisenArviointiMapper: ValmistumispyyntoOsaamisenArviointiMapper,
    private val opintooikeusRepository: OpintooikeusRepository,
    private val tyoskentelyjaksoRepository: TyoskentelyjaksoRepository,
    private val opintosuoritusRepository: OpintosuoritusRepository,
    private val mailService: MailService,
    private val applicationProperties: ApplicationProperties,
    private val clock: Clock
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
                sendMailNotificationVastuuhenkiloPalauttanut(valmistumispyynto)
            }
            valmistumispyynto
        } ?: throw getValmistumispyyntoNotFoundException()

        return valmistumispyyntoMapper.toDto(valmistumispyynto).apply {
            tila = getValmistumispyynnonTilaForArvioija(valmistumispyynto)
        }
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

        return valmistumispyyntoOsaamisenArviointiMapper.toDto(valmistumispyynto).apply {
            tila = getValmistumispyynnonTilaForArvioija(valmistumispyynto)
        }
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
            erikoistujanArvioinnit.map { it.arvioitavaKokonaisuus?.id!! }.distinct()
        val arviointienTilaDTO = ValmistumispyyntoArviointienTilaDTO(
            hasArvioitaviaKokonaisuuksiaWithArviointiLowerThanFour = erikoistujanArvioinnit.filter { it.arviointiasteikonTaso != null }
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

    private fun sendMailNotificationVastuuhenkiloPalauttanut(
        valmistumispyynto: Valmistumispyynto
    ) {
        mailService.sendEmailFromTemplate(
            valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.kayttaja?.user!!,
            templateName = "valmistumispyyntoVastuuhenkiloPalauttanut.html",
            titleKey = "email.valmistumispyyntoVastuuhenkiloPalauttanut.title",
            properties = mapOf()
        )
    }
}
