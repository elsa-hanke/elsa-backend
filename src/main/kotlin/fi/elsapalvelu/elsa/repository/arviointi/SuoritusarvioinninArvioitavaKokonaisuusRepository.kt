package fi.elsapalvelu.elsa.repository.arviointi

import fi.elsapalvelu.elsa.domain.arviointi.SuoritusarvioinninArvioitavaKokonaisuus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SuoritusarvioinninArvioitavaKokonaisuusRepository :
    JpaRepository<SuoritusarvioinninArvioitavaKokonaisuus, Long>
