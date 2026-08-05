package fi.elsapalvelu.elsa.service.kayttaja

import fi.elsapalvelu.elsa.service.criteria.ErikoistujanEteneminenCriteria
import fi.elsapalvelu.elsa.service.dto.*
import fi.elsapalvelu.elsa.service.dto.koejakso.*
import fi.elsapalvelu.elsa.service.dto.tyoskentely.*
import fi.elsapalvelu.elsa.service.dto.arviointi.*
import fi.elsapalvelu.elsa.service.dto.suoritteet.*
import fi.elsapalvelu.elsa.service.dto.koulutus.*
import fi.elsapalvelu.elsa.service.dto.seuranta.*
import fi.elsapalvelu.elsa.service.dto.valmistuminen.*
import fi.elsapalvelu.elsa.service.dto.kayttaja.*
import fi.elsapalvelu.elsa.service.dto.perustiedot.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface EtusivuService {

    fun getErikoistujienSeurantaVastuuhenkiloRajaimet(userId: String): ErikoistujienSeurantaDTO

    fun getErikoistujienSeurantaForVastuuhenkilo(
        userId: String, criteria: ErikoistujanEteneminenCriteria, pageable: Pageable
    ): Page<ErikoistujanEteneminenDTO>?

    fun getKoulutettavienSeurantaForVastuuhenkilo(
        userId: String, criteria: ErikoistujanEteneminenCriteria, pageable: Pageable
    ): Page<KoulutettavanEteneminenDTO>?

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
    ): Page<KoulutettavanEteneminenDTO>?

    fun getErikoistumisenSeurantaForErikoistuja(userId: String): ErikoistumisenEdistyminenDTO?

    fun getAvoimetAsiatForErikoistuja(userId: String): List<AvoinAsiaDTO>?

    fun getAvoimetAsiatForYekKoulutettava(userId: String): List<AvoinAsiaDTO>?

    fun getVanhenevatKatseluoikeudetForKouluttaja(userId: String): List<KatseluoikeusDTO>?
}
