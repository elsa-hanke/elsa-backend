package fi.elsapalvelu.elsa.service.impl.kayttaja

import fi.elsapalvelu.elsa.service.kayttaja.EtusivuService
import fi.elsapalvelu.elsa.service.kayttaja.etusivu.EtusivunSeurantapalvelu
import fi.elsapalvelu.elsa.service.kayttaja.etusivu.EtusivunEdistymispalvelu
import fi.elsapalvelu.elsa.service.kayttaja.etusivu.EtusivunAvoimienAsioidenPalvelu
import fi.elsapalvelu.elsa.service.criteria.ErikoistujanEteneminenCriteria
import fi.elsapalvelu.elsa.service.dto.kayttaja.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class EtusivuServiceImpl(
    private val seurantapalvelu: EtusivunSeurantapalvelu,
    private val edistymispalvelu: EtusivunEdistymispalvelu,
    private val avoimienAsioidenPalvelu: EtusivunAvoimienAsioidenPalvelu
) : EtusivuService {

    override fun getErikoistujienSeurantaVastuuhenkiloRajaimet(userId: String): ErikoistujienSeurantaDTO =
        seurantapalvelu.haeRajaimet(userId)

    override fun getErikoistujienSeurantaForVastuuhenkilo(
        userId: String, criteria: ErikoistujanEteneminenCriteria, pageable: Pageable
    ): Page<ErikoistujanEteneminenDTO>? =
        seurantapalvelu.haeErikoistujatVastuuhenkilolle(userId, criteria, pageable)

    override fun getKoulutettavienSeurantaForVastuuhenkilo(
        userId: String, criteria: ErikoistujanEteneminenCriteria, pageable: Pageable
    ): Page<KoulutettavanEteneminenDTO>? =
        seurantapalvelu.haeKoulutettavatVastuuhenkilolle(userId, criteria, pageable)

    override fun getErikoistujienSeurantaKouluttajaRajaimet(userId: String): ErikoistujienSeurantaDTO =
        seurantapalvelu.haeRajaimet(userId)

    override fun getErikoistujienSeurantaForKouluttaja(
        userId: String, criteria: ErikoistujanEteneminenCriteria, pageable: Pageable
    ): Page<ErikoistujanEteneminenDTO>? =
        seurantapalvelu.haeErikoistujatKouluttajalle(userId, criteria, pageable)

    override fun getErikoistujienSeurantaForVirkailija(
        userId: String, criteria: ErikoistujanEteneminenCriteria, pageable: Pageable
    ): Page<ErikoistujanEteneminenVirkailijaDTO>? =
        seurantapalvelu.haeErikoistujatVirkailijalle(userId, criteria, pageable)

    override fun getKoulutettavienSeurantaForVirkailija(
        userId: String, criteria: ErikoistujanEteneminenCriteria, pageable: Pageable
    ): Page<KoulutettavanEteneminenDTO>? =
        seurantapalvelu.haeKoulutettavatVirkailijalle(userId, criteria, pageable)

    override fun getErikoistumisenSeurantaForErikoistuja(userId: String): ErikoistumisenEdistyminenDTO? =
        edistymispalvelu.haeErikoistujanEdistyminen(userId)

    override fun getAvoimetAsiatForErikoistuja(userId: String): List<AvoinAsiaDTO>? =
        avoimienAsioidenPalvelu.haeAvoimetAsiat(userId)

    override fun getAvoimetAsiatForYekKoulutettava(userId: String): List<AvoinAsiaDTO>? =
        avoimienAsioidenPalvelu.haeAvoimetAsiat(userId, yek = true)

    override fun getVanhenevatKatseluoikeudetForKouluttaja(userId: String): List<KatseluoikeusDTO>? =
        avoimienAsioidenPalvelu.haeKouluttajanVanhenevatKatseluoikeudet(userId)
}
