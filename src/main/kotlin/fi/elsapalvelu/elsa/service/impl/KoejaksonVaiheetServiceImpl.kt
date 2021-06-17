package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.dto.*
import fi.elsapalvelu.elsa.service.dto.enumeration.KoejaksoTila
import fi.elsapalvelu.elsa.service.dto.enumeration.KoejaksoTyyppi
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class KoejaksonVaiheetServiceImpl(
    private val koejaksonKoulutussopimusService: KoejaksonKoulutussopimusService,
    private val koejaksonAloituskeskusteluService: KoejaksonAloituskeskusteluService,
    private val koejaksonValiarviointiService: KoejaksonValiarviointiService,
    private val koejaksonKehittamistoimenpiteetService: KoejaksonKehittamistoimenpiteetService,
    private val koejaksonLoppukeskusteluService: KoejaksonLoppukeskusteluService
) : KoejaksonVaiheetService {

    override fun findAllByKouluttajaKayttajaUserId(userId: String): List<KoejaksonVaiheDTO> {
        val resultMap = HashMap<String, MutableList<KoejaksonVaiheDTO>>()

        // Haetaan ja lisätään listaan kukin koejakson vaihe sekä siihen liittyvät aiemmat vaiheet vain kerran.
        // Esim. jos erikoistuvalle A uusin vaihe on loppukeskustelu, haetaan ja lisätään aiemmat vaiheet listaan
        // samalla kertaa. Kunkin vaiheen kohdalla lisätään myös siihen liittyvät aiemmat hyväksytyt vaiheet.
        // Seuraavia vaiheita haettaessa erikoistuvan A vaiheet ohitetaan, koska ne on jo lisätty.
        // Aloituskeskustelulle ei kuitenkaan lisätä aiempaa vaihetta (koulutussopimus) hyväksytyksi vaiheeksi.
        applyKoejaksonVaiheetStartingFromLoppukeskustelut(userId, resultMap)
        applyKoejaksonVaiheetStartingFromKehittamistoimenpiteet(userId, resultMap)
        applyKoejaksonVaiheetStartingFromValiarvioinnit(userId, resultMap)
        applyKoejaksonVaiheetStartingFromAloituskeskustelut(userId, resultMap)
        applyKoulutussopimukset(userId, resultMap)

        val flattenList = resultMap.values.flatten()
        val avoimetVaiheet = flattenList.filter {
            it.tila == KoejaksoTila.ODOTTAA_HYVAKSYNTAA
        }
        val muutVaiheet = flattenList.filter {
            it.tila != KoejaksoTila.ODOTTAA_HYVAKSYNTAA
        }
        return avoimetVaiheet.sortedBy { it.pvm } + muutVaiheet.sortedByDescending { it.pvm }
    }

    private fun applyKoejaksonVaiheetStartingFromLoppukeskustelut(
        userId: String,
        resultList: HashMap<String, MutableList<KoejaksonVaiheDTO>>
    ) {
        koejaksonLoppukeskusteluService.findAllByKouluttajaUserId(userId).forEach { (erikoistuva, loppukeskustelu) ->
            val erikoistuvaUserId = erikoistuva.userId!!
            if (resultList[erikoistuvaUserId] != null) {
                return@forEach
            }
            resultList[erikoistuvaUserId] = mutableListOf()

            val mappedLoppukeskustelu = mapLoppukeskustelu(loppukeskustelu)
            applyKoulutussopimus(erikoistuvaUserId, resultList)

            val mappedAloituskeskusteluHyvaksytty =
                applyAloituskeskustelu(erikoistuvaUserId, resultList)
            val mappedValiarviointiHyvaksytty = applyValiarviointi(
                erikoistuvaUserId,
                mappedAloituskeskusteluHyvaksytty,
                resultList
            )
            val mappedKehittamistoimenpiteetHyvaksytty = applyKehittamistoimenpiteetSingle(
                erikoistuvaUserId,
                mappedAloituskeskusteluHyvaksytty,
                mappedValiarviointiHyvaksytty,
                resultList
            )

            mappedLoppukeskustelu.hyvaksytytVaiheet.add(mappedKehittamistoimenpiteetHyvaksytty)
            mappedLoppukeskustelu.hyvaksytytVaiheet.add(mappedValiarviointiHyvaksytty)
            mappedLoppukeskustelu.hyvaksytytVaiheet.add(mappedAloituskeskusteluHyvaksytty)

            resultList[erikoistuvaUserId]!!.add(mappedLoppukeskustelu)
        }
    }

    private fun applyKoejaksonVaiheetStartingFromKehittamistoimenpiteet(
        userId: String,
        resultList: HashMap<String, MutableList<KoejaksonVaiheDTO>>
    ) {
        koejaksonKehittamistoimenpiteetService.findAllByKouluttajaUserId(userId)
            .forEach { (erikoistuva, kehittamistoimenpiteet) ->
                val erikoistuvaUserId = erikoistuva.userId!!
                if (resultList[erikoistuvaUserId] != null) {
                    return@forEach
                }
                resultList[erikoistuvaUserId] = mutableListOf()

                val mappedKehittamistoimenpiteet = mapKehittamistoimenpiteet(kehittamistoimenpiteet)
                applyKoulutussopimus(erikoistuvaUserId, resultList)

                val mappedAloituskeskusteluHyvaksytty =
                    applyAloituskeskustelu(erikoistuvaUserId, resultList)
                val mappedValiarviointiHyvaksytty = applyValiarviointi(
                    erikoistuvaUserId,
                    mappedAloituskeskusteluHyvaksytty,
                    resultList
                )

                mappedKehittamistoimenpiteet.hyvaksytytVaiheet.add(mappedValiarviointiHyvaksytty)
                mappedKehittamistoimenpiteet.hyvaksytytVaiheet.add(mappedAloituskeskusteluHyvaksytty)

                resultList[erikoistuvaUserId]!!.add(mappedKehittamistoimenpiteet)
            }
    }

    private fun applyKoejaksonVaiheetStartingFromValiarvioinnit(
        userId: String,
        resultList: HashMap<String, MutableList<KoejaksonVaiheDTO>>
    ) {
        koejaksonValiarviointiService.findAllByKouluttajaUserId(userId).forEach { (erikoistuva, valiarviointi) ->
            val erikoistuvaUserId = erikoistuva.userId!!
            if (resultList[erikoistuvaUserId] != null) {
                return@forEach
            }
            resultList[erikoistuvaUserId] = mutableListOf()

            val mappedValiarviointi = mapValiarviointi(valiarviointi)
            applyKoulutussopimus(erikoistuvaUserId, resultList)

            val mappedAloituskeskusteluHyvaksytty =
                applyAloituskeskustelu(erikoistuvaUserId, resultList)

            mappedValiarviointi.hyvaksytytVaiheet.add(mappedAloituskeskusteluHyvaksytty)
            resultList[erikoistuvaUserId]!!.add(mappedValiarviointi)
        }
    }

    private fun applyKoejaksonVaiheetStartingFromAloituskeskustelut(
        userId: String,
        resultList: HashMap<String, MutableList<KoejaksonVaiheDTO>>
    ) {
        koejaksonAloituskeskusteluService.findAllByKouluttajaUserId(userId)
            .forEach { (erikoistuva, aloituskeskustelu) ->
                val erikoistuvaUserId = erikoistuva.userId!!
                if (resultList[erikoistuvaUserId] != null) {
                    return@forEach
                }
                resultList[erikoistuvaUserId] = mutableListOf()
                applyKoulutussopimus(erikoistuvaUserId, resultList)

                resultList[erikoistuvaUserId]!!.add(mapAloituskeskustelu(aloituskeskustelu))
            }
    }

    private fun applyKoulutussopimukset(
        userId: String,
        resultList: HashMap<String, MutableList<KoejaksonVaiheDTO>>
    ) {
        koejaksonKoulutussopimusService.findAllByKouluttajaKayttajaUserId(userId)
            .forEach { (erikoistuva, koulutussopimus) ->
                val erikoistuvaUserId = erikoistuva.userId!!
                if (resultList[erikoistuvaUserId] != null) {
                    return@forEach
                }
                resultList[erikoistuvaUserId] = mutableListOf()
                resultList[erikoistuvaUserId]!!.add(mapKoulutussopimus(koulutussopimus))
            }
    }

    private fun applyKoulutussopimus(
        userId: String,
        resultList: HashMap<String, MutableList<KoejaksonVaiheDTO>>
    ) {
        val koulutussopimus =
            koejaksonKoulutussopimusService.findByErikoistuvaLaakariKayttajaUserId(userId).get()
        resultList[userId]!!.add(mapKoulutussopimus(koulutussopimus))
    }

    private fun applyAloituskeskustelu(
        userId: String,
        resultList: HashMap<String, MutableList<KoejaksonVaiheDTO>>
    ): HyvaksyttyKoejaksonVaiheDTO {
        val aloituskeskustelu =
            koejaksonAloituskeskusteluService.findByErikoistuvaLaakariKayttajaUserId(userId).get()
        val mappedAloituskeskustelu = mapAloituskeskustelu(aloituskeskustelu)
        val mappedAloituskeskusteluHyvaksytty = mapAloituskeskusteluHyvaksytty(aloituskeskustelu)
        resultList[userId]!!.add(mappedAloituskeskustelu)
        return mappedAloituskeskusteluHyvaksytty
    }

    private fun applyValiarviointi(
        userId: String,
        mappedAloituskeskusteluHyvaksytty: HyvaksyttyKoejaksonVaiheDTO,
        resultList: HashMap<String, MutableList<KoejaksonVaiheDTO>>
    ): HyvaksyttyKoejaksonVaiheDTO {
        val valiarviointi =
            koejaksonValiarviointiService.findByErikoistuvaLaakariKayttajaUserId(userId).get()
        val mappedValiarviointi = mapValiarviointi(valiarviointi)
        val mappedValiarviointiHyvaksytty = mapValiarviointiHyvaksytty(valiarviointi)
        mappedValiarviointi.apply {
            hyvaksytytVaiheet.add(mappedAloituskeskusteluHyvaksytty)
        }
        resultList[userId]!!.add(mappedValiarviointi)
        return mappedValiarviointiHyvaksytty
    }

    private fun applyKehittamistoimenpiteetSingle(
        userId: String,
        mappedAloituskeskusteluHyvaksytty: HyvaksyttyKoejaksonVaiheDTO,
        mappedValiarviointiHyvaksytty: HyvaksyttyKoejaksonVaiheDTO,
        resultList: HashMap<String, MutableList<KoejaksonVaiheDTO>>
    ): HyvaksyttyKoejaksonVaiheDTO {
        val kehittamistoimenpiteet =
            koejaksonKehittamistoimenpiteetService.findByErikoistuvaLaakariKayttajaUserId(userId).get()
        val mappedKehittamistoimenpiteet = mapKehittamistoimenpiteet(kehittamistoimenpiteet)
        val mappedKehittamistoimenpiteetHyvaksytty = mapKehittamistoimenpiteetHyvaksytty(kehittamistoimenpiteet)
        mappedKehittamistoimenpiteet.apply {
            hyvaksytytVaiheet.add(mappedValiarviointiHyvaksytty)
            hyvaksytytVaiheet.add(mappedAloituskeskusteluHyvaksytty)
        }
        resultList[userId]!!.add(mappedKehittamistoimenpiteet)
        return mappedKehittamistoimenpiteetHyvaksytty
    }

    private fun mapKoulutussopimus(koejaksonKoulutussopimusDTO: KoejaksonKoulutussopimusDTO): KoejaksonVaiheDTO {
        return KoejaksonVaiheDTO(
            koejaksonKoulutussopimusDTO.id,
            KoejaksoTyyppi.KOULUTUSSOPIMUS,
            KoejaksoTila.fromSopimus(koejaksonKoulutussopimusDTO),
            koejaksonKoulutussopimusDTO.erikoistuvanNimi,
            koejaksonKoulutussopimusDTO.muokkauspaiva
        )
    }

    private fun mapAloituskeskustelu(koejaksonAloituskeskusteluDTO: KoejaksonAloituskeskusteluDTO): KoejaksonVaiheDTO {
        return KoejaksonVaiheDTO(
            koejaksonAloituskeskusteluDTO.id,
            KoejaksoTyyppi.ALOITUSKESKUSTELU,
            KoejaksoTila.fromAloituskeskustelu(koejaksonAloituskeskusteluDTO),
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

    private fun mapValiarviointi(koejaksonValiarviointiDTO: KoejaksonValiarviointiDTO): KoejaksonVaiheDTO {
        return KoejaksonVaiheDTO(
            koejaksonValiarviointiDTO.id,
            KoejaksoTyyppi.VALIARVIOINTI,
            KoejaksoTila.fromValiarvointi(true, koejaksonValiarviointiDTO),
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

    private fun mapKehittamistoimenpiteet(koejaksonKehittamistoimenpiteetDTO: KoejaksonKehittamistoimenpiteetDTO): KoejaksonVaiheDTO {
        return KoejaksonVaiheDTO(
            koejaksonKehittamistoimenpiteetDTO.id,
            KoejaksoTyyppi.KEHITTAMISTOIMENPITEET,
            KoejaksoTila.fromKehittamistoimenpiteet(true, koejaksonKehittamistoimenpiteetDTO),
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

    private fun mapLoppukeskustelu(koejaksonLoppukeskusteluDTO: KoejaksonLoppukeskusteluDTO): KoejaksonVaiheDTO {
        return KoejaksonVaiheDTO(
            koejaksonLoppukeskusteluDTO.id,
            KoejaksoTyyppi.LOPPUKESKUSTELU,
            KoejaksoTila.fromLoppukeskustelu(true, koejaksonLoppukeskusteluDTO),
            koejaksonLoppukeskusteluDTO.erikoistuvanNimi,
            koejaksonLoppukeskusteluDTO.muokkauspaiva
        )
    }
}
