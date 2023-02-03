package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.SuoritusarvioinninArvioitavaKokonaisuus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SuoritusarvioinninArvioitavaKokonaisuusRepository :
    JpaRepository<SuoritusarvioinninArvioitavaKokonaisuus, Long>
