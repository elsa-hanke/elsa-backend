package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.criteria.ErikoistujanEteneminenCriteria
import fi.elsapalvelu.elsa.service.dto.ErikoistujanEteneminenVirkailijaDTO
import fi.elsapalvelu.elsa.service.dto.ErikoistujienSeurantaDTO
import fi.elsapalvelu.elsa.service.dto.ErikoistumisenEdistyminenDTO
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface EtusivuService {

    fun getErikoistujienSeurantaForVastuuhenkilo(userId: String): ErikoistujienSeurantaDTO

    fun getErikoistujienSeurantaForKouluttaja(userId: String): ErikoistujienSeurantaDTO

    fun getErikoistujienSeurantaForVirkailija(
        userId: String,
        criteria: ErikoistujanEteneminenCriteria,
        pageable: Pageable
    ): Page<ErikoistujanEteneminenVirkailijaDTO>?

    fun getErikoistumisenSeurantaForErikoistuja(userId: String): ErikoistumisenEdistyminenDTO?
}
