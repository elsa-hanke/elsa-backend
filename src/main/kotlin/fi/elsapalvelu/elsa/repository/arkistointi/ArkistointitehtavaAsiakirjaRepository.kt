package fi.elsapalvelu.elsa.repository.arkistointi

import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointitehtavaAsiakirja
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ArkistointitehtavaAsiakirjaRepository :
    JpaRepository<ArkistointitehtavaAsiakirja, Long> {

    fun findAllByArkistointitehtavaIdOrderByJarjestysAsc(
        arkistointitehtavaId: Long
    ): List<ArkistointitehtavaAsiakirja>
}
