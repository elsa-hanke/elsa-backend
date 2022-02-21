package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.ErikoistujienSeurantaDTO

interface EtusivuService {

    fun getErikoistujienSeurantaForVastuuhenkilo(userId: String): ErikoistujienSeurantaDTO

    fun getErikoistujienSeurantaForKouluttaja(userId: String): ErikoistujienSeurantaDTO

}
