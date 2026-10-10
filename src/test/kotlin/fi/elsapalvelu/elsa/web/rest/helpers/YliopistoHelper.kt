package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.perustiedot.Yliopisto
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import jakarta.persistence.EntityManager

object YliopistoHelper {
    /**
     * Seed isolated reference data just as Liquibase seeds the existing universities.
     * The production changelog has no yliopisto_audit table. These fixtures exercise
     * approval/archiving transactions, not ORM updates to university reference data.
     */
    fun createReferenceData(em: EntityManager, nimi: YliopistoEnum): Yliopisto {
        val id = (em.createNativeQuery("INSERT INTO yliopisto (nimi) VALUES (:nimi) RETURNING id")
            .setParameter("nimi", nimi.name)
            .singleResult as Number).toLong()
        return em.find(Yliopisto::class.java, id)
    }
}
