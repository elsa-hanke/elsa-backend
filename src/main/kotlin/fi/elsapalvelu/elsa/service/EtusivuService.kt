package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.criteria.ErikoistujanEteneminenCriteria
import fi.elsapalvelu.elsa.service.dto.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface EtusivuService {

    fun getErikoistujienSeurantaVastuuhenkiloRajaimet(userId: String): ErikoistujienSeurantaDTO

    fun getErikoistujienSeurantaForVastuuhenkilo(
        userId: String, criteria: ErikoistujanEteneminenCriteria, pageable: Pageable
    ): Page<ErikoistujanEteneminenDTO>?

    fun getErikoistujienSeurantaKouluttajaRajaimet(userId: String): ErikoistujienSeurantaDTO

    fun getErikoistujienSeurantaForKouluttaja(
        userId: String,
        criteria: ErikoistujanEteneminenCriteria,
        pageable: Pageable
    ): Page<ErikoistujanEteneminenDTO>?

    fun getErikoistujienSeurantaForVirkailija(
        userId: String,
        criteria: ErikoistujanEteneminenCriteria,
        pageable: Pageable
    ): Page<ErikoistujanEteneminenVirkailijaDTO>?

    fun getKoulutettavienSeurantaForVirkailija(
        userId: String,
        criteria: ErikoistujanEteneminenCriteria,
        pageable: Pageable
    ): Page<ErikoistujanEteneminenVirkailijaDTO>?

    fun getErikoistumisenSeurantaForErikoistuja(userId: String): ErikoistumisenEdistyminenDTO?

    fun getAvoimetAsiatForErikoistuja(userId: String): List<AvoinAsiaDTO>?

    fun getVanhenevatKatseluoikeudetForKouluttaja(userId: String): List<KatseluoikeusDTO>?
}
