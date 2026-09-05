package fi.elsapalvelu.elsa.repository.arkistointi

import fi.elsapalvelu.elsa.domain.arkistointi.Arkistointitehtava
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointitehtavanTila
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface ArkistointitehtavaRepository : JpaRepository<Arkistointitehtava, Long> {

    fun findByIdempotenssiavain(idempotenssiavain: String): Arkistointitehtava?

    @Query(
        """
        select t from Arkistointitehtava t
        where (t.tila = :odottaa and t.seuraavaKasittelyaika <= :nyt)
           or (t.tila = :kasittelyssa and t.kasittelyvarausPaattyy <= :nyt)
        order by t.seuraavaKasittelyaika asc, t.id asc
        """
    )
    fun findKasittelykelpoiset(
        @Param("nyt") nyt: Instant,
        @Param("odottaa") odottaa: ArkistointitehtavanTila,
        @Param("kasittelyssa") kasittelyssa: ArkistointitehtavanTila,
        pageable: Pageable
    ): List<Arkistointitehtava>
}
