package fi.elsapalvelu.elsa.web.rest

import fi.elsapalvelu.elsa.domain.Authority
import fi.elsapalvelu.elsa.domain.Erikoisala
import fi.elsapalvelu.elsa.domain.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.Kayttaja
import fi.elsapalvelu.elsa.domain.Yliopisto
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.MockMvc

open class ResourceIntegrationTestBase {
    @Autowired
    protected lateinit var em: EntityManager
    @Autowired
    protected lateinit var testMockMvc: MockMvc

    protected lateinit var vastuuhenkilo: Kayttaja
    protected lateinit var virkailija: Kayttaja

    protected fun initErikoistuvaLaakari(yliopisto: Yliopisto? = null, erikoisala: Erikoisala? = null): ErikoistuvaLaakari {
        val erikoistuvaLaakariUser = KayttajaResourceWithMockUserIT.createEntity(authority = Authority(ERIKOISTUVA_LAAKARI))
        em.persist(erikoistuvaLaakariUser)
        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, erikoistuvaLaakariUser, yliopisto = yliopisto, erikoisala = erikoisala)
        em.persist(erikoistuvaLaakari)

        return erikoistuvaLaakari
    }
}
