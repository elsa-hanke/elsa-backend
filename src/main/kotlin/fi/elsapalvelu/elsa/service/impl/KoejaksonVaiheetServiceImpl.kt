package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.Opintooikeus
import fi.elsapalvelu.elsa.repository.*
import fi.elsapalvelu.elsa.service.KoejaksonKoulutussopimusService
import fi.elsapalvelu.elsa.service.KoejaksonVaiheetService
import fi.elsapalvelu.elsa.service.KoejaksonVastuuhenkilonArvioQueryService
import fi.elsapalvelu.elsa.service.KoejaksonVastuuhenkilonArvioService
import fi.elsapalvelu.elsa.service.criteria.NimiErikoisalaAndAvoinCriteria
import fi.elsapalvelu.elsa.service.dto.*
import fi.elsapalvelu.elsa.service.dto.enumeration.KoejaksoTila
import fi.elsapalvelu.elsa.service.dto.enumeration.KoejaksoTyyppi
import fi.elsapalvelu.elsa.service.mapper.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import jakarta.persistence.EntityNotFoundException

@Service
@Transactional
class KoejaksonVaiheetServiceImpl(
    private val koejaksonKoulutussopimusService: KoejaksonKoulutussopimusService,
    private val koejaksonKoulutussopimusRepository: KoejaksonKoulutussopimusRepository,
    private val koejaksonKoulutussopimusMapper: KoejaksonKoulutussopimusMapper,
    private val aloituskeskusteluRepository: KoejaksonAloituskeskusteluRepository,
    private val aloituskeskusteluMapper: KoejaksonAloituskeskusteluMapper,
    private val valiarviointiRepository: KoejaksonValiarviointiRepository,
    private val valiarviointiMapper: KoejaksonValiarviointiMapper,
    private val kehittamistoimenpiteetRepository: KoejaksonKehittamistoimenpiteetRepository,
    private val kehittamistoimenpiteetMapper: KoejaksonKehittamistoimenpiteetMapper,
    private val koejaksonLoppukeskusteluRepository: KoejaksonLoppukeskusteluRepository,
    private val koejaksonLoppukeskusteluMapper: KoejaksonLoppukeskusteluMapper,
    private val koejaksonVastuuhenkilonArvioService: KoejaksonVastuuhenkilonArvioService,
    private val vastuuhenkilonArvioRepository: KoejaksonVastuuhenkilonArvioRepository,
    private val vastuuhenkilonArvioMapper: KoejaksonVastuuhenkilonArvioMapper,
    private val kayttajaRepository: KayttajaRepository,
    private val koejaksonVastuuhenkilonArvioQueryService: KoejaksonVastuuhenkilonArvioQueryService,
    private val koejaksonVastuuhenkilonArvioRepository: KoejaksonVastuuhenkilonArvioRepository
) : KoejaksonVaiheetService {

    override fun findAllByKouluttajaKayttajaUserId(
        userId: String,
        vainAvoimet: Boolean
    ): List<KoejaksonVaiheDTO> {
        val resultMap = HashMap<Long, MutableList<KoejaksonVaiheDTO>>()

        // Haetaan ja lisätään listaan kukin koejakson vaihe sekä siihen liittyvät aiemmat vaiheet vain kerran.
        // Esim. jos erikoistuvalle A uusin vaihe on loppukeskustelu, haetaan ja lisätään aiemmat vaiheet listaan
        // samalla kertaa. Kunkin vaiheen kohdalla lisätään myös siihen liittyvät aiemmat hyväksytyt vaiheet.
        // Seuraavia vaiheita haettaessa erikoistuvan A vaiheet ohitetaan, koska ne on jo lisätty.
        // Aloituskeskustelulle ei kuitenkaan lisätä aiempaa vaihetta (koulutussopimus) hyväksytyksi vaiheeksi.
        val kayttajaId = kayttajaRepository.findOneByUserId(userId).get().id

        applyKoejaksonVaiheetStartingFromLoppukeskustelut(
            userId,
            resultMap,
            kayttajaId,
            vainAvoimet
        )
        applyKoejaksonVaiheetStartingFromKehittamistoimenpiteet(
            userId,
            resultMap,
            kayttajaId,
            vainAvoimet
        )
        applyKoejaksonVaiheetStartingFromValiarvioinnit(userId, resultMap, kayttajaId, vainAvoimet)
        applyKoejaksonVaiheetStartingFromAloituskeskustelut(
            userId,
            resultMap,
            kayttajaId,
            vainAvoimet
        )
        applyKoulutussopimuksetForKouluttaja(userId, resultMap, kayttajaId!!, vainAvoimet)

        return sortKoejaksonVaiheet(resultMap)
    }

    override fun findAllByVastuuhenkiloKayttajaUserId(
        userId: String,
        vainAvoimet: Boolean
    ): List<KoejaksonVaiheDTO> {
        val resultMap = HashMap<Long, MutableList<KoejaksonVaiheDTO>>()
        val kayttajaId = kayttajaRepository.findOneByUserId(userId).get().id

        applyKoejaksonVaiheetStartingFromVastuuhenkilonArvio(userId, resultMap, vainAvoimet)
        applyKoejaksonVaiheetStartingFromLoppukeskustelut(
            userId,
            resultMap,
            kayttajaId,
            vainAvoimet
        )
        applyKoejaksonVaiheetStartingFromKehittamistoimenpiteet(
            userId,
            resultMap,
            kayttajaId,
            vainAvoimet
        )
        applyKoejaksonVaiheetStartingFromValiarvioinnit(userId, resultMap, kayttajaId, vainAvoimet)
        applyKoejaksonVaiheetStartingFromAloituskeskustelut(
            userId,
            resultMap,
            kayttajaId,
            vainAvoimet
        )
        applyKoulutussopimuksetForVastuuhenkilo(userId, resultMap, vainAvoimet)

        return sortKoejaksonVaiheet(resultMap)
    }

    override fun findAllByVirkailijaKayttajaUserId(
        userId: String,
        criteria: NimiErikoisalaAndAvoinCriteria,
        pageable: Pageable
    ): Page<KoejaksonVaiheDTO>? {
        kayttajaRepository.findOneByUserId(userId).orElse(null)?.let { k ->
            // Opintohallinnon virkailija toimii vain yhden yliopiston alla.
            k.yliopistot.firstOrNull()?.let {
                return koejaksonVastuuhenkilonArvioQueryService.findByCriteriaAndYliopistoId(
                    criteria,
                    pageable,
                    it.id!!,
                    k.user?.langKey
                ).map { arvio ->
                    koejaksonVastuuhenkilonArvioService.tarkistaAllekirjoitus(arvio)
                    KoejaksonVaiheDTO(
                        arvio.id,
                        KoejaksoTyyppi.VASTUUHENKILON_ARVIO,
                        KoejaksoTila.fromVastuuhenkilonArvio(arvio, virkailija = true),
                        arvio.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi(),
                        arvio.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getAvatar(),
                        arvio.muokkauspaiva
                    )
                }
            }
        }
        return null
    }

    override fun findAllAvoinByVirkailijaKayttajaUserId(userId: String): List<KoejaksonVaiheDTO>? {
        kayttajaRepository.findOneByUserId(userId).orElse(null)?.let { k ->
            k.yliopistot.firstOrNull()?.let {
                return koejaksonVastuuhenkilonArvioRepository.findAllAvoinByVirkailija(it.id!!)
                    .map { arvio ->
                        KoejaksonVaiheDTO(
                            arvio.id,
                            KoejaksoTyyppi.VASTUUHENKILON_ARVIO,
                            KoejaksoTila.ODOTTAA_HYVAKSYNTAA,
                            arvio.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi(),
                            arvio.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getAvatar(),
                            arvio.muokkauspaiva
                        )
                    }
            }
        }
        return null
    }

    private fun applyKoejaksonVaiheetStartingFromVastuuhenkilonArvio(
        userId: String,
        resultMap: HashMap<Long, MutableList<KoejaksonVaiheDTO>>,
        vainAvoimet: Boolean
    ) {
        val vastuuhenkilonArviot =
            if (vainAvoimet) vastuuhenkilonArvioRepository.findAllAvoinByVastuuhenkilo(
                userId
            ) else vastuuhenkilonArvioRepository.findAllByVastuuhenkiloUserIdAndVirkailijaHyvaksynytTrue(
                userId
            )
        vastuuhenkilonArviot.associate {
            koejaksonVastuuhenkilonArvioService.tarkistaAllekirjoitus(it)
            getOpintooikeusIdOrElseThrow(it.opintooikeus) to vastuuhenkilonArvioMapper.toDto(it)
        }.forEach {
            val opintooikeusId = it.key
            if (resultMap[opintooikeusId] != null) {
                return@forEach
            }
            resultMap[opintooikeusId] = mutableListOf()
            val result = mapVastuuhenkilonArvio(it.value, userId)

            if (!vainAvoimet) {
                result.apply {
                    hyvaksytytVaiheet.add(getLoppukeskusteluHyvaksytty(opintooikeusId))
                    getKehittamistoimenpiteetHyvaksytty(opintooikeusId).ifPresent { hyvaksytty ->
                        hyvaksytytVaiheet.add(hyvaksytty)
                    }
                    hyvaksytytVaiheet.add(getValiarviointiHyvaksytty(opintooikeusId))
                    hyvaksytytVaiheet.add(getAloituskeskusteluHyvaksytty(opintooikeusId))
                }
            }

            resultMap[opintooikeusId]!!.add(result)
        }
    }

    private fun applyKoejaksonVaiheetStartingFromLoppukeskustelut(
        userId: String,
        resultMap: HashMap<Long, MutableList<KoejaksonVaiheDTO>>,
        kayttajaId: Long? = null,
        vainAvoimet: Boolean
    ) {
        val loppukeskustelut =
            if (vainAvoimet) koejaksonLoppukeskusteluRepository.findAllAvoinByLahikouluttajaUserIdOrLahiesimiesUserId(
                userId
            ) else koejaksonLoppukeskusteluRepository.findAllByLahikouluttajaUserIdOrLahiesimiesUserId(
                userId
            )
        loppukeskustelut.associate {
            getOpintooikeusIdOrElseThrow(it.opintooikeus) to koejaksonLoppukeskusteluMapper.toDto(
                it
            )
        }.forEach {
            val opintooikeusId = it.key
            if (resultMap[opintooikeusId] != null) {
                return@forEach
            }
            resultMap[opintooikeusId] = mutableListOf()
            val result = mapLoppukeskustelu(it.value, kayttajaId)

            if (!vainAvoimet) {
                val mappedAloituskeskusteluHyvaksytty =
                    applyAloituskeskustelu(opintooikeusId)
                val mappedValiarviointiHyvaksytty = applyValiarviointi(
                    opintooikeusId,
                    mappedAloituskeskusteluHyvaksytty
                )
                val mappedKehittamistoimenpiteetHyvaksytty = applyKehittamistoimenpiteetSingle(
                    opintooikeusId,
                    mappedAloituskeskusteluHyvaksytty,
                    mappedValiarviointiHyvaksytty
                )

                result.apply {
                    mappedKehittamistoimenpiteetHyvaksytty.ifPresent { hyvaksytty ->
                        hyvaksytytVaiheet.add(hyvaksytty)
                    }
                    hyvaksytytVaiheet.add(mappedValiarviointiHyvaksytty)
                    hyvaksytytVaiheet.add(mappedAloituskeskusteluHyvaksytty)
                }
            }

            resultMap[opintooikeusId]!!.add(result)

        }
    }

    private fun applyKoejaksonVaiheetStartingFromKehittamistoimenpiteet(
        userId: String,
        resultMap: HashMap<Long, MutableList<KoejaksonVaiheDTO>>,
        kayttajaId: Long? = null,
        vainAvoimet: Boolean
    ) {
        val kehittamistoimenpiteet =
            if (vainAvoimet) kehittamistoimenpiteetRepository.findAllAvoinByLahikouluttajaUserIdOrLahiesimiesUserId(
                userId
            ) else kehittamistoimenpiteetRepository.findAllByLahikouluttajaUserIdOrLahiesimiesUserId(
                userId
            )
        kehittamistoimenpiteet.associate {
            getOpintooikeusIdOrElseThrow(it.opintooikeus) to kehittamistoimenpiteetMapper.toDto(
                it
            )
        }.forEach {
            val opintooikeusId = it.key
            if (resultMap[opintooikeusId] != null) {
                return@forEach
            }
            resultMap[opintooikeusId] = mutableListOf()
            val result = mapKehittamistoimenpiteet(it.value, kayttajaId)

            if (!vainAvoimet) {
                val mappedAloituskeskusteluHyvaksytty =
                    applyAloituskeskustelu(opintooikeusId)
                val mappedValiarviointiHyvaksytty = applyValiarviointi(
                    opintooikeusId,
                    mappedAloituskeskusteluHyvaksytty
                )

                result.apply {
                    hyvaksytytVaiheet.add(mappedValiarviointiHyvaksytty)
                    hyvaksytytVaiheet.add(mappedAloituskeskusteluHyvaksytty)
                }
            }

            resultMap[opintooikeusId]!!.add(result)
        }
    }

    private fun applyKoejaksonVaiheetStartingFromValiarvioinnit(
        userId: String,
        resultMap: HashMap<Long, MutableList<KoejaksonVaiheDTO>>,
        kayttajaId: Long? = null,
        vainAvoimet: Boolean
    ) {
        val valiarvioinnit =
            if (vainAvoimet) valiarviointiRepository.findAllAvoinByLahikouluttajaUserIdOrLahiesimiesUserId(
                userId
            ) else valiarviointiRepository.findAllByLahikouluttajaUserIdOrLahiesimiesUserId(
                userId
            )
        valiarvioinnit.associate {
            getOpintooikeusIdOrElseThrow(it.opintooikeus) to valiarviointiMapper.toDto(it)
        }.forEach {
            val opintooikeusId = it.key
            if (resultMap[opintooikeusId] != null) {
                return@forEach
            }
            resultMap[opintooikeusId] = mutableListOf()
            val result = mapValiarviointi(it.value, kayttajaId)

            if (!vainAvoimet) {
                val mappedAloituskeskusteluHyvaksytty =
                    applyAloituskeskustelu(opintooikeusId)

                result.apply {
                    hyvaksytytVaiheet.add(mappedAloituskeskusteluHyvaksytty)
                }
            }

            resultMap[opintooikeusId]!!.add(result)
        }
    }

    private fun applyKoejaksonVaiheetStartingFromAloituskeskustelut(
        userId: String,
        resultMap: HashMap<Long, MutableList<KoejaksonVaiheDTO>>,
        kayttajaId: Long? = null,
        vainAvoimet: Boolean
    ) {
        val aloituskeskustelu =
            if (vainAvoimet) aloituskeskusteluRepository.findAllAvoinByLahikouluttajaUserIdOrLahiesimiesUserId(
                userId
            ) else aloituskeskusteluRepository.findAllByLahikouluttajaUserIdOrLahiesimiesUserId(
                userId
            )
        aloituskeskustelu.associate {
            getOpintooikeusIdOrElseThrow(it.opintooikeus) to aloituskeskusteluMapper.toDto(
                it
            )
        }.forEach {
            val opintooikeusId = it.key
            if (resultMap[opintooikeusId] != null) {
                return@forEach
            }
            resultMap[opintooikeusId] = mutableListOf()
            resultMap[opintooikeusId]!!.add(
                mapAloituskeskustelu(it.value, kayttajaId)
            )
        }
    }

    private fun applyKoulutussopimuksetForKouluttaja(
        userId: String,
        resultMap: HashMap<Long, MutableList<KoejaksonVaiheDTO>>,
        kayttajaId: Long,
        vainAvoimet: Boolean
    ) {
        val koulutussopimukset =
            if (vainAvoimet) koejaksonKoulutussopimusRepository.findAllAvoinByKouluttajatKouluttajaUserId(
                userId
            ) else koejaksonKoulutussopimusRepository.findAllByKouluttajatKouluttajaUserId(
                userId
            )
        koulutussopimukset.associate {
            getOpintooikeusIdOrElseThrow(it.opintooikeus) to koejaksonKoulutussopimusMapper.toDto(
                it
            )
        }.forEach {
            val opintooikeusId = it.key
            resultMap.putIfAbsent(opintooikeusId, mutableListOf())
            resultMap[opintooikeusId]!!.add(mapKoulutussopimus(it.value, kayttajaId))
        }
    }

    private fun applyKoulutussopimuksetForVastuuhenkilo(
        userId: String,
        resultMap: HashMap<Long, MutableList<KoejaksonVaiheDTO>>,
        vainAvoimet: Boolean
    ) {
        val koulutussopimukset =
            if (vainAvoimet) koejaksonKoulutussopimusRepository.findAllAvoinByVastuuhenkiloUserId(
                userId
            ) else koejaksonKoulutussopimusRepository.findAllByVastuuhenkiloUserId(userId)
        koulutussopimukset.associate {
            getOpintooikeusIdOrElseThrow(it.opintooikeus) to koejaksonKoulutussopimusMapper.toDto(
                it
            )
        }.forEach {
            val opintooikeusId = it.key
            resultMap.putIfAbsent(opintooikeusId, mutableListOf())
            resultMap[opintooikeusId]!!.add(mapKoulutussopimus(it.value))
        }
    }

    private fun applyAloituskeskustelu(
        opintooikeusId: Long
    ): HyvaksyttyKoejaksonVaiheDTO {
        val aloituskeskusteluDTO =
            aloituskeskusteluRepository.findByOpintooikeusId(opintooikeusId)
                .map(aloituskeskusteluMapper::toDto).get()
        return mapAloituskeskusteluHyvaksytty(aloituskeskusteluDTO)
    }

    private fun getAloituskeskusteluHyvaksytty(opintooikeusId: Long): HyvaksyttyKoejaksonVaiheDTO {
        val aloituskeskusteluDTO =
            aloituskeskusteluRepository.findByOpintooikeusId(opintooikeusId)
                .map(aloituskeskusteluMapper::toDto).get()
        return mapAloituskeskusteluHyvaksytty(aloituskeskusteluDTO)
    }

    private fun applyValiarviointi(
        opintooikeusId: Long,
        mappedAloituskeskusteluHyvaksytty: HyvaksyttyKoejaksonVaiheDTO,
        kayttajaId: Long? = null
    ): HyvaksyttyKoejaksonVaiheDTO {
        val valiarviointiDTO =
            valiarviointiRepository.findByOpintooikeusId(opintooikeusId)
                .map(valiarviointiMapper::toDto).get()
        val mappedValiarviointi = mapValiarviointi(valiarviointiDTO, kayttajaId)
        val mappedValiarviointiHyvaksytty = mapValiarviointiHyvaksytty(valiarviointiDTO)
        mappedValiarviointi.apply {
            hyvaksytytVaiheet.add(mappedAloituskeskusteluHyvaksytty)
        }
        return mappedValiarviointiHyvaksytty
    }

    private fun getValiarviointiHyvaksytty(opintooikeusId: Long): HyvaksyttyKoejaksonVaiheDTO {
        val valiarviointiDTO =
            valiarviointiRepository.findByOpintooikeusId(opintooikeusId)
                .map(valiarviointiMapper::toDto).get()
        return mapValiarviointiHyvaksytty(valiarviointiDTO)
    }

    private fun applyKehittamistoimenpiteetSingle(
        opintooikeusId: Long,
        mappedAloituskeskusteluHyvaksytty: HyvaksyttyKoejaksonVaiheDTO,
        mappedValiarviointiHyvaksytty: HyvaksyttyKoejaksonVaiheDTO,
        kayttajaId: Long? = null
    ): Optional<HyvaksyttyKoejaksonVaiheDTO> {
        var mappedKehittamistoimenpiteetHyvaksytty: HyvaksyttyKoejaksonVaiheDTO? = null
        kehittamistoimenpiteetRepository.findByOpintooikeusId(opintooikeusId)
            .map(kehittamistoimenpiteetMapper::toDto)
            .ifPresent {
                val mappedKehittamistoimenpiteet = mapKehittamistoimenpiteet(it, kayttajaId)
                mappedKehittamistoimenpiteetHyvaksytty = mapKehittamistoimenpiteetHyvaksytty(it)
                mappedKehittamistoimenpiteet.apply {
                    hyvaksytytVaiheet.add(mappedValiarviointiHyvaksytty)
                    hyvaksytytVaiheet.add(mappedAloituskeskusteluHyvaksytty)
                }
            }
        return Optional.ofNullable(mappedKehittamistoimenpiteetHyvaksytty)
    }

    private fun getKehittamistoimenpiteetHyvaksytty(opintooikeusId: Long): Optional<HyvaksyttyKoejaksonVaiheDTO> {
        var mappedKehittamistoimenpiteetHyvaksytty: HyvaksyttyKoejaksonVaiheDTO? = null
        kehittamistoimenpiteetRepository.findByOpintooikeusId(opintooikeusId)
            .map(kehittamistoimenpiteetMapper::toDto)
            .ifPresent {
                mappedKehittamistoimenpiteetHyvaksytty = mapKehittamistoimenpiteetHyvaksytty(it)
            }
        return Optional.ofNullable(mappedKehittamistoimenpiteetHyvaksytty)
    }

    private fun getLoppukeskusteluHyvaksytty(opintooikeusId: Long): HyvaksyttyKoejaksonVaiheDTO {
        val loppukeskustelu =
            koejaksonLoppukeskusteluRepository.findByOpintooikeusId(opintooikeusId)
                .map(koejaksonLoppukeskusteluMapper::toDto).get()
        return mapLoppukeskusteluHyvaksytty(loppukeskustelu)
    }

    private fun mapKoulutussopimus(
        koejaksonKoulutussopimusDTO: KoejaksonKoulutussopimusDTO,
        kayttajaId: Long? = null
    ): KoejaksonVaiheDTO {
        return KoejaksonVaiheDTO(
            koejaksonKoulutussopimusDTO.id,
            KoejaksoTyyppi.KOULUTUSSOPIMUS,
            KoejaksoTila.fromSopimus(koejaksonKoulutussopimusDTO, kayttajaId),
            koejaksonKoulutussopimusDTO.erikoistuvanNimi,
            koejaksonKoulutussopimusDTO.erikoistuvanAvatar,
            koejaksonKoulutussopimusDTO.muokkauspaiva
        )
    }

    private fun mapAloituskeskustelu(
        koejaksonAloituskeskusteluDTO: KoejaksonAloituskeskusteluDTO,
        kayttajaId: Long? = null
    ): KoejaksonVaiheDTO {
        return KoejaksonVaiheDTO(
            koejaksonAloituskeskusteluDTO.id,
            KoejaksoTyyppi.ALOITUSKESKUSTELU,
            KoejaksoTila.fromAloituskeskustelu(koejaksonAloituskeskusteluDTO, kayttajaId),
            koejaksonAloituskeskusteluDTO.erikoistuvanNimi,
            koejaksonAloituskeskusteluDTO.erikoistuvanAvatar,
            koejaksonAloituskeskusteluDTO.muokkauspaiva
        )
    }

    private fun mapAloituskeskusteluHyvaksytty(
        koejaksonAloituskeskusteluDTO: KoejaksonAloituskeskusteluDTO
    ): HyvaksyttyKoejaksonVaiheDTO {
        return HyvaksyttyKoejaksonVaiheDTO(
            koejaksonAloituskeskusteluDTO.id,
            KoejaksoTyyppi.ALOITUSKESKUSTELU,
            koejaksonAloituskeskusteluDTO.muokkauspaiva
        )
    }

    private fun mapValiarviointi(
        koejaksonValiarviointiDTO: KoejaksonValiarviointiDTO,
        kayttajaId: Long? = null
    ): KoejaksonVaiheDTO {
        return KoejaksonVaiheDTO(
            koejaksonValiarviointiDTO.id,
            KoejaksoTyyppi.VALIARVIOINTI,
            KoejaksoTila.fromValiarvointi(true, koejaksonValiarviointiDTO, kayttajaId),
            koejaksonValiarviointiDTO.erikoistuvanNimi,
            koejaksonValiarviointiDTO.erikoistuvanAvatar,
            koejaksonValiarviointiDTO.muokkauspaiva
        )
    }

    private fun mapValiarviointiHyvaksytty(
        koejaksonValiarviointiDTO: KoejaksonValiarviointiDTO
    ): HyvaksyttyKoejaksonVaiheDTO {
        return HyvaksyttyKoejaksonVaiheDTO(
            koejaksonValiarviointiDTO.id,
            KoejaksoTyyppi.VALIARVIOINTI,
            koejaksonValiarviointiDTO.muokkauspaiva
        )
    }

    private fun mapKehittamistoimenpiteet(
        koejaksonKehittamistoimenpiteetDTO: KoejaksonKehittamistoimenpiteetDTO,
        kayttajaId: Long? = null
    ): KoejaksonVaiheDTO {
        return KoejaksonVaiheDTO(
            koejaksonKehittamistoimenpiteetDTO.id,
            KoejaksoTyyppi.KEHITTAMISTOIMENPITEET,
            KoejaksoTila.fromKehittamistoimenpiteet(
                true,
                koejaksonKehittamistoimenpiteetDTO,
                kayttajaId
            ),
            koejaksonKehittamistoimenpiteetDTO.erikoistuvanNimi,
            koejaksonKehittamistoimenpiteetDTO.erikoistuvanAvatar,
            koejaksonKehittamistoimenpiteetDTO.muokkauspaiva
        )
    }

    private fun mapKehittamistoimenpiteetHyvaksytty(
        koejaksonKehittamistoimenpiteetDTO: KoejaksonKehittamistoimenpiteetDTO
    ): HyvaksyttyKoejaksonVaiheDTO {
        return HyvaksyttyKoejaksonVaiheDTO(
            koejaksonKehittamistoimenpiteetDTO.id,
            KoejaksoTyyppi.KEHITTAMISTOIMENPITEET,
            koejaksonKehittamistoimenpiteetDTO.muokkauspaiva
        )
    }

    private fun mapLoppukeskustelu(
        koejaksonLoppukeskusteluDTO: KoejaksonLoppukeskusteluDTO,
        kayttajaId: Long? = null
    ): KoejaksonVaiheDTO {
        return KoejaksonVaiheDTO(
            koejaksonLoppukeskusteluDTO.id,
            KoejaksoTyyppi.LOPPUKESKUSTELU,
            KoejaksoTila.fromLoppukeskustelu(true, koejaksonLoppukeskusteluDTO, kayttajaId),
            koejaksonLoppukeskusteluDTO.erikoistuvanNimi,
            koejaksonLoppukeskusteluDTO.erikoistuvanAvatar,
            koejaksonLoppukeskusteluDTO.muokkauspaiva
        )
    }

    private fun mapLoppukeskusteluHyvaksytty(
        koejaksonLoppukeskusteluDTO: KoejaksonLoppukeskusteluDTO
    ): HyvaksyttyKoejaksonVaiheDTO {
        return HyvaksyttyKoejaksonVaiheDTO(
            koejaksonLoppukeskusteluDTO.id,
            KoejaksoTyyppi.LOPPUKESKUSTELU,
            koejaksonLoppukeskusteluDTO.muokkauspaiva
        )
    }

    private fun mapVastuuhenkilonArvio(
        vastuuhenkilonArvioDTO: KoejaksonVastuuhenkilonArvioDTO,
        userId: String
    ): KoejaksonVaiheDTO {
        return KoejaksonVaiheDTO(
            vastuuhenkilonArvioDTO.id,
            KoejaksoTyyppi.VASTUUHENKILON_ARVIO,
            KoejaksoTila.fromVastuuhenkilonArvio(true, vastuuhenkilonArvioDTO, userId),
            vastuuhenkilonArvioDTO.erikoistuvanNimi,
            vastuuhenkilonArvioDTO.erikoistuvanAvatar,
            vastuuhenkilonArvioDTO.muokkauspaiva
        )
    }

    private fun sortKoejaksonVaiheet(
        resultMap: HashMap<Long, MutableList<KoejaksonVaiheDTO>>
    ): List<KoejaksonVaiheDTO> {
        val flattenList = resultMap.values.flatten().asSequence()
        val avoimetVaiheet = flattenList.filter {
            it.tila == KoejaksoTila.ODOTTAA_HYVAKSYNTAA
        }.sortedBy { it.pvm }.toList()
        val muutVaiheet = flattenList.filter {
            it.tila != KoejaksoTila.ODOTTAA_HYVAKSYNTAA
        }.sortedByDescending { it.pvm }.toList()

        return avoimetVaiheet + muutVaiheet
    }

    private fun getOpintooikeusIdOrElseThrow(opintooikeus: Opintooikeus?): Long {
        return opintooikeus?.id ?: throw EntityNotFoundException("Opinto-oikeutta ei löydy")
    }
}
