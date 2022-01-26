package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.Koulutussuunnitelma
import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.web.rest.createByteArray
import fi.elsapalvelu.elsa.web.rest.findAll
import javax.persistence.EntityManager

class KoulutussuunnitelmaHelper {

    companion object {

        const val DEFAULT_MOTIVAATIOKIRJE = "AAAAAAAAAA"
        const val UPDATED_MOTIVAATIOKIRJE = "BBBBBBBBBB"

        const val DEFAULT_MOTIVAATIOKIRJE_YKSITYINEN: Boolean = false
        const val UPDATED_MOTIVAATIOKIRJE_YKSITYINEN: Boolean = true

        const val DEFAULT_OPISKELU_JA_TYOHISTORIA = "AAAAAAAAAA"
        const val UPDATED_OPISKELU_JA_TYOHISTORIA = "BBBBBBBBBB"

        const val DEFAULT_OPISKELU_JA_TYOHISTORIA_YKSITYINEN: Boolean = false
        const val UPDATED_OPISKELU_JA_TYOHISTORIA_YKSITYINEN: Boolean = true

        const val DEFAULT_VAHVUUDET = "AAAAAAAAAA"
        const val UPDATED_VAHVUUDET = "BBBBBBBBBB"

        const val DEFAULT_VAHVUUDET_YKSITYINEN: Boolean = false
        const val UPDATED_VAHVUUDET_YKSITYINEN: Boolean = true

        const val DEFAULT_TULEVAISUUDEN_VISIOINTI = "AAAAAAAAAA"
        const val UPDATED_TULEVAISUUDEN_VISIOINTI = "BBBBBBBBBB"

        const val DEFAULT_TULEVAISUUDEN_VISIOINTI_YKSITYINEN: Boolean = false
        const val UPDATED_TULEVAISUUDEN_VISIOINTI_YKSITYINEN: Boolean = true

        const val DEFAULT_OSAAMISEN_KARTUTTAMINEN = "AAAAAAAAAA"
        const val UPDATED_OSAAMISEN_KARTUTTAMINEN = "BBBBBBBBBB"

        const val DEFAULT_OSAAMISEN_KARTUTTAMINEN_YKSITYINEN: Boolean = false
        const val UPDATED_OSAAMISEN_KARTUTTAMINEN_YKSITYINEN: Boolean = true

        const val DEFAULT_ELAMANKENTTA = "AAAAAAAAAA"
        const val UPDATED_ELAMANKENTTA = "BBBBBBBBBB"

        const val DEFAULT_ELAMANKENTTA_YKSITYINEN: Boolean = false
        const val UPDATED_ELAMANKENTTA_YKSITYINEN: Boolean = true

        val DEFAULT_FILE: ByteArray = createByteArray(1, "0")
        val UPDATED_FILE: ByteArray = createByteArray(1, "1")

        @JvmStatic
        fun createEntity(em: EntityManager, user: User? = null): Koulutussuunnitelma {
            val koulutussuunnitelma = Koulutussuunnitelma(
                motivaatiokirje = DEFAULT_MOTIVAATIOKIRJE,
                motivaatiokirjeYksityinen = DEFAULT_MOTIVAATIOKIRJE_YKSITYINEN,
                opiskeluJaTyohistoria = DEFAULT_OPISKELU_JA_TYOHISTORIA,
                opiskeluJaTyohistoriaYksityinen = DEFAULT_OPISKELU_JA_TYOHISTORIA_YKSITYINEN,
                vahvuudet = DEFAULT_VAHVUUDET,
                vahvuudetYksityinen = DEFAULT_VAHVUUDET_YKSITYINEN,
                tulevaisuudenVisiointi = DEFAULT_TULEVAISUUDEN_VISIOINTI,
                tulevaisuudenVisiointiYksityinen = DEFAULT_TULEVAISUUDEN_VISIOINTI_YKSITYINEN,
                osaamisenKartuttaminen = DEFAULT_OSAAMISEN_KARTUTTAMINEN,
                osaamisenKartuttaminenYksityinen = DEFAULT_OSAAMISEN_KARTUTTAMINEN_YKSITYINEN,
                elamankentta = DEFAULT_ELAMANKENTTA,
                elamankenttaYksityinen = DEFAULT_ELAMANKENTTA_YKSITYINEN
            )

            // Lisätään pakollinen tieto
            var erikoistuvaLaakari =
                em.findAll(ErikoistuvaLaakari::class).firstOrNull { it.kayttaja?.user == user }
            if (erikoistuvaLaakari == null) {
                erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, user)
                em.persist(erikoistuvaLaakari)
                em.flush()
            }
            koulutussuunnitelma.opintooikeus = erikoistuvaLaakari.getOpintooikeusKaytossa()

            return koulutussuunnitelma
        }

        @JvmStatic
        fun createUpdatedEntity(em: EntityManager): Koulutussuunnitelma {
            val koulutussuunnitelma = Koulutussuunnitelma(
                motivaatiokirje = UPDATED_MOTIVAATIOKIRJE,
                motivaatiokirjeYksityinen = UPDATED_MOTIVAATIOKIRJE_YKSITYINEN,
                opiskeluJaTyohistoria = UPDATED_OPISKELU_JA_TYOHISTORIA,
                opiskeluJaTyohistoriaYksityinen = UPDATED_OPISKELU_JA_TYOHISTORIA_YKSITYINEN,
                vahvuudet = UPDATED_VAHVUUDET,
                vahvuudetYksityinen = UPDATED_VAHVUUDET_YKSITYINEN,
                tulevaisuudenVisiointi = UPDATED_TULEVAISUUDEN_VISIOINTI,
                tulevaisuudenVisiointiYksityinen = UPDATED_TULEVAISUUDEN_VISIOINTI_YKSITYINEN,
                osaamisenKartuttaminen = UPDATED_OSAAMISEN_KARTUTTAMINEN,
                osaamisenKartuttaminenYksityinen = UPDATED_OSAAMISEN_KARTUTTAMINEN_YKSITYINEN,
                elamankentta = UPDATED_ELAMANKENTTA,
                elamankenttaYksityinen = UPDATED_ELAMANKENTTA_YKSITYINEN
            )

            // Lisätään pakollinen tieto
            val erikoistuvaLaakari: ErikoistuvaLaakari
            if (em.findAll(ErikoistuvaLaakari::class).isEmpty()) {
                erikoistuvaLaakari = ErikoistuvaLaakariHelper.createUpdatedEntity(em)
                em.persist(erikoistuvaLaakari)
                em.flush()
            } else {
                erikoistuvaLaakari = em.findAll(ErikoistuvaLaakari::class).get(0)
            }
            koulutussuunnitelma.opintooikeus = erikoistuvaLaakari.getOpintooikeusKaytossa()

            return koulutussuunnitelma
        }
    }
}
