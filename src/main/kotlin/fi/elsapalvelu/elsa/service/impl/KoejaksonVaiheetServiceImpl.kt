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

    override fun findAllByKouluttajaKayttajaId(kayttajaId: String): List<KoejaksonVaiheDTO> {
        val resultMap = HashMap<String, MutableList<KoejaksonVaiheDTO>>()

        // Haetaan ja lisätään listaan kukin koejakson vaihe sekä siihen liittyvät aiemmat vaiheet vain kerran.
        // Esim. jos erikoistuvalle A uusin vaihe on loppukeskustelu, haetaan ja lisätään aiemmat vaiheet listaan
        // samalla kertaa. Kunkin vaiheen kohdalla lisätään myös siihen liittyvät aiemmat hyväksytyt vaiheet.
        // Seuraavia vaiheita haettaessa erikoistuvan A vaiheet ohitetaan, koska ne on jo lisätty.
        // Aloituskeskustelulle ei kuitenkaan lisätä aiempaa vaihetta (koulutussopimus) hyväksytyksi vaiheeksi.

        applyKoejaksonVaiheetStartingFromLoppukeskustelut(kayttajaId, resultMap)
        applyKoejaksonVaiheetStartingFromKehittamistoimenpiteet(kayttajaId, resultMap)
        applyKoejaksonVaiheetStartingFromValiarvioinnit(kayttajaId, resultMap)
        applyKoejaksonVaiheetStartingFromAloituskeskustelut(kayttajaId, resultMap)
        applyKoulutussopimuksetForKouluttaja(kayttajaId, resultMap)

        return sortKoejaksonVaiheet(resultMap)
    }

    override fun findAllByVastuuhenkiloKayttajaId(kayttajaId: String): List<KoejaksonVaiheDTO> {
        val resultMap = HashMap<String, MutableList<KoejaksonVaiheDTO>>()

        applyKoejaksonVaiheetStartingFromVastuuhenkilonArvio(kayttajaId, resultMap)
        applyKoulutussopimuksetForVastuuhenkilo(kayttajaId, resultMap)

        return sortKoejaksonVaiheet(resultMap)
    }

    private fun applyKoejaksonVaiheetStartingFromVastuuhenkilonArvio(
        kayttajaId: String,
        resultList: HashMap<String, MutableList<KoejaksonVaiheDTO>>
    ) {
        vastuuhenkilonArvioService.findAllByVastuuhenkiloId(kayttajaId)
            .forEach { (erikoistuvaKayttaja, vastuuhenkilonArvio) ->
                val erikoistuvaKayttajaId = erikoistuvaKayttaja.id!!
                if (resultList[erikoistuvaKayttajaId] != null) {
                    return@forEach
                }
                resultList[erikoistuvaKayttajaId] = mutableListOf()

                mapVastuuhenkilonArvio(vastuuhenkilonArvio).apply {
                    hyvaksytytVaiheet.add(getLoppukeskusteluHyvaksytty(erikoistuvaKayttajaId))
                    getKehittamistoimenpiteetHyvaksytty(erikoistuvaKayttajaId).ifPresent {
                        hyvaksytytVaiheet.add(it)
                    }
                    hyvaksytytVaiheet.add(getValiarviointiHyvaksytty(erikoistuvaKayttajaId))
                    hyvaksytytVaiheet.add(getAloituskeskusteluHyvaksytty(erikoistuvaKayttajaId))
                }.let {
                    resultList[erikoistuvaKayttajaId]!!.add(it)
                }
            }
    }

    private fun applyKoejaksonVaiheetStartingFromLoppukeskustelut(
        kayttajaId: String,
        resultList: HashMap<String, MutableList<KoejaksonVaiheDTO>>,
    ) {
        koejaksonLoppukeskusteluService.findAllByKouluttajaId(kayttajaId)
            .forEach { (erikoistuvaKayttaja, loppukeskustelu) ->
                val erikoistuvaKayttajaId = erikoistuvaKayttaja.id!!
                if (resultList[erikoistuvaKayttajaId] != null) {
                    return@forEach
                }
                resultList[erikoistuvaKayttajaId] = mutableListOf()

                val mappedAloituskeskusteluHyvaksytty =
                    applyAloituskeskustelu(erikoistuvaKayttajaId)
                val mappedValiarviointiHyvaksytty = applyValiarviointi(
                    erikoistuvaKayttajaId,
                    mappedAloituskeskusteluHyvaksytty
                )
                val mappedKehittamistoimenpiteetHyvaksytty = applyKehittamistoimenpiteetSingle(
                    erikoistuvaKayttajaId,
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
                    resultList[erikoistuvaKayttajaId]!!.add(it)
                }
            }
    }

    private fun applyKoejaksonVaiheetStartingFromKehittamistoimenpiteet(
        kayttajaId: String,
        resultList: HashMap<String, MutableList<KoejaksonVaiheDTO>>,
    ) {
        koejaksonKehittamistoimenpiteetService.findAllByKouluttajaId(kayttajaId)
            .forEach { (erikoistuvaKayttaja, kehittamistoimenpiteet) ->
                val erikoistuvaKayttajaId = erikoistuvaKayttaja.id!!
                if (resultList[erikoistuvaKayttajaId] != null) {
                    return@forEach
                }
                resultList[erikoistuvaKayttajaId] = mutableListOf()

                val mappedAloituskeskusteluHyvaksytty =
                    applyAloituskeskustelu(erikoistuvaKayttajaId)
                val mappedValiarviointiHyvaksytty = applyValiarviointi(
                    erikoistuvaKayttajaId,
                    mappedAloituskeskusteluHyvaksytty
                )

                mapKehittamistoimenpiteet(kehittamistoimenpiteet, kayttajaId).apply {
                    hyvaksytytVaiheet.add(mappedValiarviointiHyvaksytty)
                    hyvaksytytVaiheet.add(mappedAloituskeskusteluHyvaksytty)
                }.let {
                    resultList[erikoistuvaKayttajaId]!!.add(it)
                }
            }
    }

    private fun applyKoejaksonVaiheetStartingFromValiarvioinnit(
        kayttajaId: String,
        resultList: HashMap<String, MutableList<KoejaksonVaiheDTO>>,
    ) {
        koejaksonValiarviointiService.findAllByKouluttajaId(kayttajaId)
            .forEach { (erikoistuvaKayttaja, valiarviointi) ->
                val erikoistuvaKayttajaId = erikoistuvaKayttaja.id!!
                if (resultList[erikoistuvaKayttajaId] != null) {
                    return@forEach
                }
                resultList[erikoistuvaKayttajaId] = mutableListOf()

                val mappedAloituskeskusteluHyvaksytty =
                    applyAloituskeskustelu(erikoistuvaKayttajaId)

                mapValiarviointi(valiarviointi, kayttajaId).apply {
                    hyvaksytytVaiheet.add(mappedAloituskeskusteluHyvaksytty)
                }.let {
                    resultList[erikoistuvaKayttajaId]!!.add(it)
                }
            }
    }

    private fun applyKoejaksonVaiheetStartingFromAloituskeskustelut(
        kayttajaId: String,
        resultList: HashMap<String, MutableList<KoejaksonVaiheDTO>>,
    ) {
        koejaksonAloituskeskusteluService.findAllByKouluttajaId(kayttajaId)
            .forEach { (erikoistuvaKayttaja, aloituskeskustelu) ->
                val erikoistuvaKayttajaId = erikoistuvaKayttaja.id!!
                if (resultList[erikoistuvaKayttajaId] != null) {
                    return@forEach
                }
                resultList[erikoistuvaKayttajaId] = mutableListOf()
                resultList[erikoistuvaKayttajaId]!!.add(
                    mapAloituskeskustelu(
                        aloituskeskustelu,
                        kayttajaId
                    )
                )
            }
    }

    private fun applyKoulutussopimuksetForKouluttaja(
        kayttajaId: String,
        resultList: HashMap<String, MutableList<KoejaksonVaiheDTO>>
    ) {
        koejaksonKoulutussopimusService.findAllByKouluttajaKayttajaId(kayttajaId)
            .forEach { (erikoistuvaKayttaja, koulutussopimus) ->
                val erikoistuvaKayttajaId = erikoistuvaKayttaja.id!!
                resultList.putIfAbsent(erikoistuvaKayttajaId, mutableListOf())
                resultList[erikoistuvaKayttajaId]!!.add(mapKoulutussopimus(koulutussopimus, kayttajaId))
            }
    }

    private fun applyKoulutussopimuksetForVastuuhenkilo(
        kayttajaId: String,
        resultList: HashMap<String, MutableList<KoejaksonVaiheDTO>>
    ) {
        koejaksonKoulutussopimusService.findAllByVastuuhenkiloKayttajaId(kayttajaId)
            .forEach { (erikoistuvaKayttaja, koulutussopimus) ->
                val erikoistuvaKayttajaId = erikoistuvaKayttaja.id!!
                resultList.putIfAbsent(erikoistuvaKayttajaId, mutableListOf())
                resultList[erikoistuvaKayttajaId]!!.add(mapKoulutussopimus(koulutussopimus))
            }
    }

    private fun applyAloituskeskustelu(
        kayttajaId: String
    ): HyvaksyttyKoejaksonVaiheDTO {
        val aloituskeskustelu =
            koejaksonAloituskeskusteluService.findByErikoistuvaLaakariKayttajaId(kayttajaId).get()
        return mapAloituskeskusteluHyvaksytty(aloituskeskustelu)
    }

    private fun getAloituskeskusteluHyvaksytty(kayttajaId: String): HyvaksyttyKoejaksonVaiheDTO {
        val aloituskeskustelu =
            koejaksonAloituskeskusteluService.findByErikoistuvaLaakariKayttajaId(kayttajaId).get()
        return mapAloituskeskusteluHyvaksytty(aloituskeskustelu)
    }

    private fun applyValiarviointi(
        kayttajaId: String,
        mappedAloituskeskusteluHyvaksytty: HyvaksyttyKoejaksonVaiheDTO,
    ): HyvaksyttyKoejaksonVaiheDTO {
        val valiarviointi =
            koejaksonValiarviointiService.findByErikoistuvaLaakariKayttajaId(kayttajaId).get()
        val mappedValiarviointi = mapValiarviointi(valiarviointi, kayttajaId)
        val mappedValiarviointiHyvaksytty = mapValiarviointiHyvaksytty(valiarviointi)
        mappedValiarviointi.apply {
            hyvaksytytVaiheet.add(mappedAloituskeskusteluHyvaksytty)
        }
        return mappedValiarviointiHyvaksytty
    }

    private fun getValiarviointiHyvaksytty(kayttajaId: String): HyvaksyttyKoejaksonVaiheDTO {
        val valiarviointi =
            koejaksonValiarviointiService.findByErikoistuvaLaakariKayttajaId(kayttajaId).get()
        return mapValiarviointiHyvaksytty(valiarviointi)
    }

    private fun applyKehittamistoimenpiteetSingle(
        kayttajaId: String,
        mappedAloituskeskusteluHyvaksytty: HyvaksyttyKoejaksonVaiheDTO,
        mappedValiarviointiHyvaksytty: HyvaksyttyKoejaksonVaiheDTO,
    ): Optional<HyvaksyttyKoejaksonVaiheDTO> {
        var mappedKehittamistoimenpiteetHyvaksytty: HyvaksyttyKoejaksonVaiheDTO? = null
        koejaksonKehittamistoimenpiteetService.findByErikoistuvaLaakariKayttajaId(kayttajaId)
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

    private fun getKehittamistoimenpiteetHyvaksytty(kayttajaId: String): Optional<HyvaksyttyKoejaksonVaiheDTO> {
        var mappedKehittamistoimenpiteetHyvaksytty: HyvaksyttyKoejaksonVaiheDTO? = null
        koejaksonKehittamistoimenpiteetService.findByErikoistuvaLaakariKayttajaId(kayttajaId)
            .ifPresent {
                mappedKehittamistoimenpiteetHyvaksytty = mapKehittamistoimenpiteetHyvaksytty(it)
            }
        return Optional.ofNullable(mappedKehittamistoimenpiteetHyvaksytty)
    }

    private fun getLoppukeskusteluHyvaksytty(kayttajaId: String): HyvaksyttyKoejaksonVaiheDTO {
        val loppukeskustelu =
            koejaksonLoppukeskusteluService.findByErikoistuvaLaakariKayttajaId(kayttajaId).get()
        return mapLoppukeskusteluHyvaksytty(loppukeskustelu)
    }

    private fun mapKoulutussopimus(
        koejaksonKoulutussopimusDTO: KoejaksonKoulutussopimusDTO,
        kayttajaId: String? = null
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
        kayttajaId: String? = null
    ): KoejaksonVaiheDTO {
        return KoejaksonVaiheDTO(
            koejaksonAloituskeskusteluDTO.id,
            KoejaksoTyyppi.ALOITUSKESKUSTELU,
            KoejaksoTila.fromAloituskeskustelu(koejaksonAloituskeskusteluDTO, kayttajaId),
            koejaksonAloituskeskusteluDTO.erikoistuvanNimi,
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
        kayttajaId: String? = null
    ): KoejaksonVaiheDTO {
        return KoejaksonVaiheDTO(
            koejaksonValiarviointiDTO.id,
            KoejaksoTyyppi.VALIARVIOINTI,
            KoejaksoTila.fromValiarvointi(true, koejaksonValiarviointiDTO, kayttajaId),
            koejaksonValiarviointiDTO.erikoistuvanNimi,
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
        kayttajaId: String? = null
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
        kayttajaId: String? = null
    ): KoejaksonVaiheDTO {
        return KoejaksonVaiheDTO(
            koejaksonLoppukeskusteluDTO.id,
            KoejaksoTyyppi.LOPPUKESKUSTELU,
            KoejaksoTila.fromLoppukeskustelu(true, koejaksonLoppukeskusteluDTO, kayttajaId),
            koejaksonLoppukeskusteluDTO.erikoistuvanNimi,
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
            vastuuhenkilonArvioDTO.muokkauspaiva
        )
    }

    private fun sortKoejaksonVaiheet(
        resultMap: HashMap<String, MutableList<KoejaksonVaiheDTO>>
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
}
