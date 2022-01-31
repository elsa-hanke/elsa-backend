package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.Opintooikeus
import fi.elsapalvelu.elsa.repository.*
import fi.elsapalvelu.elsa.service.KoejaksonVaiheetService
import fi.elsapalvelu.elsa.service.dto.*
import fi.elsapalvelu.elsa.service.dto.enumeration.KoejaksoTila
import fi.elsapalvelu.elsa.service.dto.enumeration.KoejaksoTyyppi
import fi.elsapalvelu.elsa.service.mapper.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import javax.persistence.EntityNotFoundException

@Service
@Transactional
class KoejaksonVaiheetServiceImpl(
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
    private val vastuuhenkilonArvioRepository: KoejaksonVastuuhenkilonArvioRepository,
    private val vastuuhenkilonArvioMapper: KoejaksonVastuuhenkilonArvioMapper,
    private val kayttajaRepository: KayttajaRepository
) : KoejaksonVaiheetService {

    override fun findAllByKouluttajaKayttajaUserId(userId: String): List<KoejaksonVaiheDTO> {
        val resultMap = HashMap<Long, MutableList<KoejaksonVaiheDTO>>()

        // Haetaan ja lisätään listaan kukin koejakson vaihe sekä siihen liittyvät aiemmat vaiheet vain kerran.
        // Esim. jos erikoistuvalle A uusin vaihe on loppukeskustelu, haetaan ja lisätään aiemmat vaiheet listaan
        // samalla kertaa. Kunkin vaiheen kohdalla lisätään myös siihen liittyvät aiemmat hyväksytyt vaiheet.
        // Seuraavia vaiheita haettaessa erikoistuvan A vaiheet ohitetaan, koska ne on jo lisätty.
        // Aloituskeskustelulle ei kuitenkaan lisätä aiempaa vaihetta (koulutussopimus) hyväksytyksi vaiheeksi.
        val kayttajaId = kayttajaRepository.findOneByUserId(userId).get().id

        applyKoejaksonVaiheetStartingFromLoppukeskustelut(userId, resultMap, kayttajaId)
        applyKoejaksonVaiheetStartingFromKehittamistoimenpiteet(userId, resultMap, kayttajaId)
        applyKoejaksonVaiheetStartingFromValiarvioinnit(userId, resultMap, kayttajaId)
        applyKoejaksonVaiheetStartingFromAloituskeskustelut(userId, resultMap, kayttajaId)
        applyKoulutussopimuksetForKouluttaja(userId, resultMap, kayttajaId!!)

        return sortKoejaksonVaiheet(resultMap)
    }

    override fun findAllByVastuuhenkiloKayttajaUserId(userId: String): List<KoejaksonVaiheDTO> {
        val resultMap = HashMap<Long, MutableList<KoejaksonVaiheDTO>>()

        applyKoejaksonVaiheetStartingFromVastuuhenkilonArvio(userId, resultMap)
        applyKoulutussopimuksetForVastuuhenkilo(userId, resultMap)

        return sortKoejaksonVaiheet(resultMap)
    }

    private fun applyKoejaksonVaiheetStartingFromVastuuhenkilonArvio(
        userId: String,
        resultMap: HashMap<Long, MutableList<KoejaksonVaiheDTO>>
    ) {
        vastuuhenkilonArvioRepository.findAllByVastuuhenkiloUserId(userId).associate {
            getOpintooikeusIdOrElseThrow(it.opintooikeus) to vastuuhenkilonArvioMapper.toDto(it)
        }.forEach {
            val opintooikeusId = it.key
            if (resultMap[opintooikeusId] != null) {
                return@forEach
            }
            resultMap[opintooikeusId] = mutableListOf()

            mapVastuuhenkilonArvio(it.value).apply {
                hyvaksytytVaiheet.add(getLoppukeskusteluHyvaksytty(opintooikeusId))
                getKehittamistoimenpiteetHyvaksytty(opintooikeusId).ifPresent { hyvaksytty ->
                    hyvaksytytVaiheet.add(hyvaksytty)
                }
                hyvaksytytVaiheet.add(getValiarviointiHyvaksytty(opintooikeusId))
                hyvaksytytVaiheet.add(getAloituskeskusteluHyvaksytty(opintooikeusId))
            }.let { mappedKoejaksonVaihe ->
                resultMap[opintooikeusId]!!.add(mappedKoejaksonVaihe)
            }
        }
    }

    private fun applyKoejaksonVaiheetStartingFromLoppukeskustelut(
        userId: String,
        resultMap: HashMap<Long, MutableList<KoejaksonVaiheDTO>>,
        kayttajaId: Long? = null
    ) {
        koejaksonLoppukeskusteluRepository.findAllByLahikouluttajaUserIdOrLahiesimiesUserId(userId).associate {
            getOpintooikeusIdOrElseThrow(it.opintooikeus) to koejaksonLoppukeskusteluMapper.toDto(it)
        }.forEach {
            val opintooikeusId = it.key
            if (resultMap[opintooikeusId] != null) {
                return@forEach
            }
            resultMap[opintooikeusId] = mutableListOf()

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

            mapLoppukeskustelu(it.value, kayttajaId).apply {
                mappedKehittamistoimenpiteetHyvaksytty.ifPresent { hyvaksytty ->
                    hyvaksytytVaiheet.add(hyvaksytty)
                }
                hyvaksytytVaiheet.add(mappedValiarviointiHyvaksytty)
                hyvaksytytVaiheet.add(mappedAloituskeskusteluHyvaksytty)
            }.let { mappedKoejaksonVaihe ->
                resultMap[opintooikeusId]!!.add(mappedKoejaksonVaihe)
            }
        }
    }

    private fun applyKoejaksonVaiheetStartingFromKehittamistoimenpiteet(
        userId: String,
        resultMap: HashMap<Long, MutableList<KoejaksonVaiheDTO>>,
        kayttajaId: Long? = null
    ) {
        kehittamistoimenpiteetRepository.findAllByLahikouluttajaUserIdOrLahiesimiesUserId(userId).associate {
            getOpintooikeusIdOrElseThrow(it.opintooikeus) to kehittamistoimenpiteetMapper.toDto(it)
        }.forEach {
            val opintooikeusId = it.key
            if (resultMap[opintooikeusId] != null) {
                return@forEach
            }
            resultMap[opintooikeusId] = mutableListOf()

            val mappedAloituskeskusteluHyvaksytty =
                applyAloituskeskustelu(opintooikeusId)
            val mappedValiarviointiHyvaksytty = applyValiarviointi(
                opintooikeusId,
                mappedAloituskeskusteluHyvaksytty
            )

            mapKehittamistoimenpiteet(it.value, kayttajaId).apply {
                hyvaksytytVaiheet.add(mappedValiarviointiHyvaksytty)
                hyvaksytytVaiheet.add(mappedAloituskeskusteluHyvaksytty)
            }.let { mappedKoejaksonVaihe ->
                resultMap[opintooikeusId]!!.add(mappedKoejaksonVaihe)
            }
        }
    }

    private fun applyKoejaksonVaiheetStartingFromValiarvioinnit(
        userId: String,
        resultMap: HashMap<Long, MutableList<KoejaksonVaiheDTO>>,
        kayttajaId: Long? = null
    ) {
        valiarviointiRepository.findAllByLahikouluttajaUserIdOrLahiesimiesUserId(userId).associate {
            getOpintooikeusIdOrElseThrow(it.opintooikeus) to valiarviointiMapper.toDto(it)
        }.forEach {
            val opintooikeusId = it.key
            if (resultMap[opintooikeusId] != null) {
                return@forEach
            }
            resultMap[opintooikeusId] = mutableListOf()

            val mappedAloituskeskusteluHyvaksytty =
                applyAloituskeskustelu(opintooikeusId)

            mapValiarviointi(it.value, kayttajaId).apply {
                hyvaksytytVaiheet.add(mappedAloituskeskusteluHyvaksytty)
            }.let { mappedKoejaksonVaihe ->
                resultMap[opintooikeusId]!!.add(mappedKoejaksonVaihe)
            }
        }
    }

    private fun applyKoejaksonVaiheetStartingFromAloituskeskustelut(
        userId: String,
        resultMap: HashMap<Long, MutableList<KoejaksonVaiheDTO>>,
        kayttajaId: Long? = null
    ) {
        aloituskeskusteluRepository.findAllByLahikouluttajaUserIdOrLahiesimiesUserId(userId).associate {
            getOpintooikeusIdOrElseThrow(it.opintooikeus) to aloituskeskusteluMapper.toDto(it)
        }.forEach {
            val opintooikeusId = it.key
            if (resultMap[opintooikeusId] != null) {
                return@forEach
            }
            resultMap[opintooikeusId] = mutableListOf()
            resultMap[opintooikeusId]!!.add(
                mapAloituskeskustelu(
                    it.value,
                    kayttajaId
                )
            )
        }
    }

    private fun applyKoulutussopimuksetForKouluttaja(
        userId: String,
        resultMap: HashMap<Long, MutableList<KoejaksonVaiheDTO>>,
        kayttajaId: Long
    ) {
        koejaksonKoulutussopimusRepository.findAllByKouluttajatKouluttajaUserId(userId).associate {
            getOpintooikeusIdOrElseThrow(it.opintooikeus) to koejaksonKoulutussopimusMapper.toDto(it)
        }.forEach {
            val opintooikeusId = it.key
            resultMap.putIfAbsent(opintooikeusId, mutableListOf())
            resultMap[opintooikeusId]!!.add(mapKoulutussopimus(it.value, kayttajaId))
        }
    }

    private fun applyKoulutussopimuksetForVastuuhenkilo(
        userId: String,
        resultMap: HashMap<Long, MutableList<KoejaksonVaiheDTO>>
    ) {
        koejaksonKoulutussopimusRepository.findAllByVastuuhenkiloUserId(userId).associate {
            getOpintooikeusIdOrElseThrow(it.opintooikeus) to koejaksonKoulutussopimusMapper.toDto(it)
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
            aloituskeskusteluRepository.findByOpintooikeusId(opintooikeusId).map(aloituskeskusteluMapper::toDto).get()
        return mapAloituskeskusteluHyvaksytty(aloituskeskusteluDTO)
    }

    private fun getAloituskeskusteluHyvaksytty(opintooikeusId: Long): HyvaksyttyKoejaksonVaiheDTO {
        val aloituskeskusteluDTO =
            aloituskeskusteluRepository.findByOpintooikeusId(opintooikeusId).map(aloituskeskusteluMapper::toDto).get()
        return mapAloituskeskusteluHyvaksytty(aloituskeskusteluDTO)
    }

    private fun applyValiarviointi(
        opintooikeusId: Long,
        mappedAloituskeskusteluHyvaksytty: HyvaksyttyKoejaksonVaiheDTO,
        kayttajaId: Long? = null
    ): HyvaksyttyKoejaksonVaiheDTO {
        val valiarviointiDTO =
            valiarviointiRepository.findByOpintooikeusId(opintooikeusId).map(valiarviointiMapper::toDto).get()
        val mappedValiarviointi = mapValiarviointi(valiarviointiDTO, kayttajaId)
        val mappedValiarviointiHyvaksytty = mapValiarviointiHyvaksytty(valiarviointiDTO)
        mappedValiarviointi.apply {
            hyvaksytytVaiheet.add(mappedAloituskeskusteluHyvaksytty)
        }
        return mappedValiarviointiHyvaksytty
    }

    private fun getValiarviointiHyvaksytty(opintooikeusId: Long): HyvaksyttyKoejaksonVaiheDTO {
        val valiarviointiDTO =
            valiarviointiRepository.findByOpintooikeusId(opintooikeusId).map(valiarviointiMapper::toDto).get()
        return mapValiarviointiHyvaksytty(valiarviointiDTO)
    }

    private fun applyKehittamistoimenpiteetSingle(
        opintooikeusId: Long,
        mappedAloituskeskusteluHyvaksytty: HyvaksyttyKoejaksonVaiheDTO,
        mappedValiarviointiHyvaksytty: HyvaksyttyKoejaksonVaiheDTO,
        kayttajaId: Long? = null
    ): Optional<HyvaksyttyKoejaksonVaiheDTO> {
        var mappedKehittamistoimenpiteetHyvaksytty: HyvaksyttyKoejaksonVaiheDTO? = null
        kehittamistoimenpiteetRepository.findByOpintooikeusId(opintooikeusId).map(kehittamistoimenpiteetMapper::toDto)
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
        kehittamistoimenpiteetRepository.findByOpintooikeusId(opintooikeusId).map(kehittamistoimenpiteetMapper::toDto)
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

    private fun mapVastuuhenkilonArvio(vastuuhenkilonArvioDTO: KoejaksonVastuuhenkilonArvioDTO): KoejaksonVaiheDTO {
        return KoejaksonVaiheDTO(
            vastuuhenkilonArvioDTO.id,
            KoejaksoTyyppi.VASTUUHENKILON_ARVIO,
            KoejaksoTila.fromVastuuhenkilonArvio(true, vastuuhenkilonArvioDTO),
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
