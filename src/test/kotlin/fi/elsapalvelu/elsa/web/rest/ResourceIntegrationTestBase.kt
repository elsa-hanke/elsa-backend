package fi.elsapalvelu.elsa.web.rest

import fi.elsapalvelu.elsa.domain.Authority
import fi.elsapalvelu.elsa.domain.Erikoisala
import fi.elsapalvelu.elsa.domain.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.Kayttaja
import fi.elsapalvelu.elsa.domain.Yliopisto
import fi.elsapalvelu.elsa.domain.Yliopisto_
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoisalaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper
import fi.elsapalvelu.elsa.web.rest.helpers.KayttajahallintaResourceHelper
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc

@AutoConfigureMockMvc
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

    protected fun persistAndFlush(entity: Any) {
        em.persist(entity)
        em.flush()
    }

    protected fun persistYliopisto(yliopistoNimi: YliopistoEnum): Yliopisto {
        val yliopisto = Yliopisto(nimi = yliopistoNimi)
        em.persist(yliopisto)
        return yliopisto
    }

    protected fun createPersistedErikoisala(): Erikoisala {
        val erikoisala = ErikoisalaHelper.createEntity()
        em.persist(erikoisala)
        return erikoisala
    }

    protected fun createPersistedErikoistuvaLaakari(): ErikoistuvaLaakari {
        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em)
        em.persist(erikoistuvaLaakari)
        return erikoistuvaLaakari
    }

    protected fun createPersistedVastuuhenkilo(yliopisto: Yliopisto, erikoisala: Erikoisala): Kayttaja {
        val vastuuhenkilo = KayttajahallintaResourceHelper.createVastuuhenkilo(em, yliopisto, erikoisala)
        em.persist(vastuuhenkilo)
        return vastuuhenkilo
    }

    protected fun createPersistedVirkailija(yliopisto: Yliopisto): Kayttaja {
        val virkailija = KayttajahallintaResourceHelper.createVirkailija(em, yliopisto)
        em.persist(virkailija)
        return virkailija
    }

    protected fun createPersistedPaakayttaja(): Kayttaja {
        val paakayttaja = KayttajahallintaResourceHelper.createPaakayttaja(em)
        em.persist(paakayttaja)
        return paakayttaja
    }
}
