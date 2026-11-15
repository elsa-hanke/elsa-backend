package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.koulutus.OpintosuoritusKurssikoodi
import fi.elsapalvelu.elsa.domain.koulutus.OpintosuoritusTyyppi
import fi.elsapalvelu.elsa.domain.perustiedot.Yliopisto
import fi.elsapalvelu.elsa.domain.koulutus.OpintosuoritusTyyppiEnum
import jakarta.persistence.EntityManager

object KurssikoodiHelper {

    private const val DEFAULT_TUNNISTE = "AAAAAAAAAA"
    private val DEFAULT_TYYPPI = OpintosuoritusTyyppiEnum.JOHTAMISOPINTO

    fun createEntity(
        em: EntityManager,
        tunniste: String? = DEFAULT_TUNNISTE,
        tyyppi: OpintosuoritusTyyppiEnum? = DEFAULT_TYYPPI,
        yliopisto: Yliopisto,
        isOsakokonaisuus: Boolean = false
    ): OpintosuoritusKurssikoodi {

        val opintosuoritusTyyppi =
            em.createQuery("select t from OpintosuoritusTyyppi t where t.nimi = :nimi", OpintosuoritusTyyppi::class.java)
                .setParameter("nimi", tyyppi).singleResult

        val kurssikoodi = OpintosuoritusKurssikoodi(
            tunniste = tunniste,
            tyyppi = opintosuoritusTyyppi,
            isOsakokonaisuus = isOsakokonaisuus,
            yliopisto = yliopisto
        )

        em.persist(kurssikoodi)
        em.flush()

        return kurssikoodi
    }
}
