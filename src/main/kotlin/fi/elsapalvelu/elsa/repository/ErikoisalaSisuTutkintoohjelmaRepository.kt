package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.koulutus.ErikoisalaSisuTutkintoohjelma
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ErikoisalaSisuTutkintoohjelmaRepository : JpaRepository<ErikoisalaSisuTutkintoohjelma, Long> {

    fun findOneByTutkintoohjelmaId(tutkintoohjelmaId: String): ErikoisalaSisuTutkintoohjelma?
}
