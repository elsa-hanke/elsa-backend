package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.KayttajaRepository
import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.dto.*
import fi.elsapalvelu.elsa.service.dto.enumeration.KoejaksoTila
import fi.elsapalvelu.elsa.service.dto.enumeration.KoejaksoTyyppi
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional
class KoejaksonVaiheetServiceImpl(
    private val koejaksonKoulutussopimusService: KoejaksonKoulutussopimusService,
    private val koejaksonAloituskeskusteluService: KoejaksonAloituskeskusteluService,
    private val koejaksonValiarviointiService: KoejaksonValiarviointiService,
    private val koejaksonKehittamistoimenpiteetService: KoejaksonKehittamistoimenpiteetService,
    private val koejaksonLoppukeskusteluService: KoejaksonLoppukeskusteluService,
    private val vastuuhenkilonArvioService: KoejaksonVastuuhenkilonArvioService,
    private val kayttajaRepository: KayttajaRepository
) : KoejaksonVaiheetService {

    override fun findAllByKouluttajaKayttajaUserId(userId: String): List<KoejaksonVaiheDTO> {
        val resultMap = HashMap<String, MutableList<KoejaksonVaiheDTO>>()

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
        applyKoulutussopimuksetForKouluttaja(userId, kayttajaId!!, resultMap)

        return sortKoejaksonVaiheet(resultMap)
    }

    override fun findAllByVastuuhenkiloKayttajaUserId(userId: String): List<KoejaksonVaiheDTO> {
        val resultMap = HashMap<String, MutableList<KoejaksonVaiheDTO>>()

        applyKoejaksonVaiheetStartingFromVastuuhenkilonArvio(userId, resultMap)
        applyKoulutussopimuksetForVastuuhenkilo(userId, resultMap)

        return sortKoejaksonVaiheet(resultMap)
    }

    private fun applyKoejaksonVaiheetStartingFromVastuuhenkilonArvio(
        userId: String,
        resultList: HashMap<String, MutableList<KoejaksonVaiheDTO>>
    ) {
        vastuuhenkilonArvioService.findAllByVastuuhenkiloUserId(userId)
            .forEach { (erikoistuva, vastuuhenkilonArvio) ->
                val erikoistuvaUserId = erikoistuva.userId!!
                if (resultList[erikoistuvaUserId] != null) {
                    return@forEach
                }
                resultList[erikoistuvaUserId] = mutableListOf()

                mapVastuuhenkilonArvio(vastuuhenkilonArvio).apply {
                    hyvaksytytVaiheet.add(getLoppukeskusteluHyvaksytty(erikoistuvaUserId))
                    getKehittamistoimenpiteetHyvaksytty(erikoistuvaUserId).ifPresent {
                        hyvaksytytVaiheet.add(it)
                    }
                    hyvaksytytVaiheet.add(getValiarviointiHyvaksytty(erikoistuvaUserId))
                    hyvaksytytVaiheet.add(getAloituskeskusteluHyvaksytty(erikoistuvaUserId))
                }.let {
                    resultList[erikoistuvaUserId]!!.add(it)
                }
            }
    }

    private fun applyKoejaksonVaiheetStartingFromLoppukeskustelut(
        userId: String,
        resultList: HashMap<String, MutableList<KoejaksonVaiheDTO>>,
        kayttajaId: Long? = null
    ) {
        koejaksonLoppukeskusteluService.findAllByKouluttajaUserId(userId)
            .forEach { (erikoistuva, loppukeskustelu) ->
                val erikoistuvaUserId = erikoistuva.userId!!
                if (resultList[erikoistuvaUserId] != null) {
                    return@forEach
                }
                resultList[erikoistuvaUserId] = mutableListOf()

                val mappedAloituskeskusteluHyvaksytty =
                    applyAloituskeskustelu(erikoistuvaUserId)
                val mappedValiarviointiHyvaksytty = applyValiarviointi(
                    erikoistuvaUserId,
                    mappedAloituskeskusteluHyvaksytty
                )
                val mappedKehittamistoimenpiteetHyvaksytty = applyKehittamistoimenpiteetSingle(
                    erikoistuvaUserId,
                    mappedAloituskeskusteluHyvaksytty,
                    mappedValiarviointiHyvaksytty
                )

                mapLoppukeskustelu(loppukeskustelu, kayttajaId).apply {
                    mappedKehittamistoimenpiteetHyvaksytty.ifPresent {
                        hyvaksytytVaiheet.add(it)
                    }
                    hyvaksytytVaiheet.add(mappedValiarviointiHyvaksytty)
                    hyvaksytytVaiheet.add(mappedAloituskeskusteluHyvaksytty)
                }.let {
                    resultList[erikoistuvaUserId]!!.add(it)
                }
            }
    }

    private fun applyKoejaksonVaiheetStartingFromKehittamistoimenpiteet(
        userId: String,
        resultList: HashMap<String, MutableList<KoejaksonVaiheDTO>>,
        kayttajaId: Long? = null
    ) {
        koejaksonKehittamistoimenpiteetService.findAllByKouluttajaUserId(userId)
            .forEach { (erikoistuva, kehittamistoimenpiteet) ->
                val erikoistuvaUserId = erikoistuva.userId!!
                if (resultList[erikoistuvaUserId] != null) {
                    return@forEach
                }
                resultList[erikoistuvaUserId] = mutableListOf()

                val mappedAloituskeskusteluHyvaksytty =
                    applyAloituskeskustelu(erikoistuvaUserId)
                val mappedValiarviointiHyvaksytty = applyValiarviointi(
                    erikoistuvaUserId,
                    mappedAloituskeskusteluHyvaksytty
                )

                mapKehittamistoimenpiteet(kehittamistoimenpiteet, kayttajaId).apply {
                    hyvaksytytVaiheet.add(mappedValiarviointiHyvaksytty)
                    hyvaksytytVaiheet.add(mappedAloituskeskusteluHyvaksytty)
                }.let {
                    resultList[erikoistuvaUserId]!!.add(it)
                }
            }
    }

    private fun applyKoejaksonVaiheetStartingFromValiarvioinnit(
        userId: String,
        resultList: HashMap<String, MutableList<KoejaksonVaiheDTO>>,
        kayttajaId: Long? = null
    ) {
        koejaksonValiarviointiService.findAllByKouluttajaUserId(userId)
            .forEach { (erikoistuva, valiarviointi) ->
                val erikoistuvaUserId = erikoistuva.userId!!
                if (resultList[erikoistuvaUserId] != null) {
                    return@forEach
                }
                resultList[erikoistuvaUserId] = mutableListOf()

                val mappedAloituskeskusteluHyvaksytty =
                    applyAloituskeskustelu(erikoistuvaUserId)

                mapValiarviointi(valiarviointi, kayttajaId).apply {
                    hyvaksytytVaiheet.add(mappedAloituskeskusteluHyvaksytty)
                }.let {
                    resultList[erikoistuvaUserId]!!.add(it)
                }
            }
    }

    private fun applyKoejaksonVaiheetStartingFromAloituskeskustelut(
        userId: String,
        resultList: HashMap<String, MutableList<KoejaksonVaiheDTO>>,
        kayttajaId: Long? = null
    ) {
        koejaksonAloituskeskusteluService.findAllByKouluttajaUserId(userId)
            .forEach { (erikoistuva, aloituskeskustelu) ->
                val erikoistuvaUserId = erikoistuva.userId!!
                if (resultList[erikoistuvaUserId] != null) {
                    return@forEach
                }
                resultList[erikoistuvaUserId] = mutableListOf()

                resultList[erikoistuvaUserId]!!.add(
                    mapAloituskeskustelu(
                        aloituskeskustelu,
                        kayttajaId
                    )
                )
            }
    }

    private fun applyKoulutussopimuksetForKouluttaja(
        userId: String,
        kayttajaId: Long,
        resultList: HashMap<String, MutableList<KoejaksonVaiheDTO>>
    ) {
        koejaksonKoulutussopimusService.findAllByKouluttajaKayttajaUserId(userId)
            .forEach { (erikoistuva, koulutussopimus) ->
                val erikoistuvaUserId = erikoistuva.userId!!
                resultList.putIfAbsent(erikoistuvaUserId, mutableListOf())
                resultList[erikoistuvaUserId]!!.add(mapKoulutussopimus(koulutussopimus, kayttajaId))
            }
    }

    private fun applyKoulutussopimuksetForVastuuhenkilo(
        userId: String,
        resultList: HashMap<String, MutableList<KoejaksonVaiheDTO>>
    ) {
        koejaksonKoulutussopimusService.findAllByVastuuhenkiloKayttajaUserId(userId)
            .forEach { (erikoistuva, koulutussopimus) ->
                val erikoistuvaUserId = erikoistuva.userId!!
                resultList.putIfAbsent(erikoistuvaUserId, mutableListOf())
                resultList[erikoistuvaUserId] = mutableListOf()
                resultList[erikoistuvaUserId]!!.add(mapKoulutussopimus(koulutussopimus))
            }
    }

    private fun applyAloituskeskustelu(
        userId: String
    ): HyvaksyttyKoejaksonVaiheDTO {
        val aloituskeskustelu =
            koejaksonAloituskeskusteluService.findByErikoistuvaLaakariKayttajaUserId(userId).get()
        return mapAloituskeskusteluHyvaksytty(aloituskeskustelu)
    }

    private fun getAloituskeskusteluHyvaksytty(userId: String): HyvaksyttyKoejaksonVaiheDTO {
        val aloituskeskustelu =
            koejaksonAloituskeskusteluService.findByErikoistuvaLaakariKayttajaUserId(userId).get()
        return mapAloituskeskusteluHyvaksytty(aloituskeskustelu)
    }

    private fun applyValiarviointi(
        userId: String,
        mappedAloituskeskusteluHyvaksytty: HyvaksyttyKoejaksonVaiheDTO,
        kayttajaId: Long? = null
    ): HyvaksyttyKoejaksonVaiheDTO {
        val valiarviointi =
            koejaksonValiarviointiService.findByErikoistuvaLaakariKayttajaUserId(userId).get()
        val mappedValiarviointi = mapValiarviointi(valiarviointi, kayttajaId)
        val mappedValiarviointiHyvaksytty = mapValiarviointiHyvaksytty(valiarviointi)
        mappedValiarviointi.apply {
            hyvaksytytVaiheet.add(mappedAloituskeskusteluHyvaksytty)
        }
        return mappedValiarviointiHyvaksytty
    }

    private fun getValiarviointiHyvaksytty(userId: String): HyvaksyttyKoejaksonVaiheDTO {
        val valiarviointi =
            koejaksonValiarviointiService.findByErikoistuvaLaakariKayttajaUserId(userId).get()
        return mapValiarviointiHyvaksytty(valiarviointi)
    }

    private fun applyKehittamistoimenpiteetSingle(
        userId: String,
        mappedAloituskeskusteluHyvaksytty: HyvaksyttyKoejaksonVaiheDTO,
        mappedValiarviointiHyvaksytty: HyvaksyttyKoejaksonVaiheDTO,
        kayttajaId: Long? = null
    ): Optional<HyvaksyttyKoejaksonVaiheDTO> {
        var mappedKehittamistoimenpiteetHyvaksytty: HyvaksyttyKoejaksonVaiheDTO? = null
        koejaksonKehittamistoimenpiteetService.findByErikoistuvaLaakariKayttajaUserId(userId)
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

    private fun getKehittamistoimenpiteetHyvaksytty(userId: String): Optional<HyvaksyttyKoejaksonVaiheDTO> {
        var mappedKehittamistoimenpiteetHyvaksytty: HyvaksyttyKoejaksonVaiheDTO? = null
        koejaksonKehittamistoimenpiteetService.findByErikoistuvaLaakariKayttajaUserId(userId)
            .ifPresent {
                mappedKehittamistoimenpiteetHyvaksytty = mapKehittamistoimenpiteetHyvaksytty(it)
            }
        return Optional.ofNullable(mappedKehittamistoimenpiteetHyvaksytty)
    }

    private fun getLoppukeskusteluHyvaksytty(userId: String): HyvaksyttyKoejaksonVaiheDTO {
        val loppukeskustelu =
            koejaksonLoppukeskusteluService.findByErikoistuvaLaakariKayttajaUserId(userId).get()
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
            koejaksonAloituskeskusteluDTO.muokkauspaiva
        )
    }

    private fun mapAloituskeskusteluHyvaksytty(koejaksonAloituskeskusteluDTO: KoejaksonAloituskeskusteluDTO): HyvaksyttyKoejaksonVaiheDTO {
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
            koejaksonValiarviointiDTO.muokkauspaiva
        )
    }

    private fun mapValiarviointiHyvaksytty(koejaksonValiarviointiDTO: KoejaksonValiarviointiDTO): HyvaksyttyKoejaksonVaiheDTO {
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
            koejaksonKehittamistoimenpiteetDTO.muokkauspaiva
        )
    }

    private fun mapKehittamistoimenpiteetHyvaksytty(koejaksonKehittamistoimenpiteetDTO: KoejaksonKehittamistoimenpiteetDTO): HyvaksyttyKoejaksonVaiheDTO {
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
            koejaksonLoppukeskusteluDTO.muokkauspaiva
        )
    }

    private fun mapLoppukeskusteluHyvaksytty(koejaksonLoppukeskusteluDTO: KoejaksonLoppukeskusteluDTO): HyvaksyttyKoejaksonVaiheDTO {
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
            vastuuhenkilonArvioDTO.muokkauspaiva
        )
    }

    private fun sortKoejaksonVaiheet(resultMap: HashMap<String, MutableList<KoejaksonVaiheDTO>>): List<KoejaksonVaiheDTO> {
        val flattenList = resultMap.values.flatten()
        val avoimetVaiheet = flattenList.filter {
            it.tila == KoejaksoTila.ODOTTAA_HYVAKSYNTAA
        }
        val muutVaiheet = flattenList.filter {
            it.tila != KoejaksoTila.ODOTTAA_HYVAKSYNTAA
        }
        return avoimetVaiheet.sortedBy { it.pvm } + muutVaiheet.sortedByDescending { it.pvm }
    }
}
