package fi.elsapalvelu.elsa.cleanup

import fi.elsapalvelu.elsa.domain.koulutus.Koulutussuunnitelma
import fi.elsapalvelu.elsa.domain.seuranta.Paivakirjamerkinta
import fi.elsapalvelu.elsa.domain.seuranta.Seurantajakso
import org.springframework.stereotype.Component

@Component
class CleanupOpintoOikeus {

    fun clearKoulutussuunnitelmaContent(koulutussuunnitelma: Koulutussuunnitelma) {
        koulutussuunnitelma.motivaatiokirje = null
        koulutussuunnitelma.opiskeluJaTyohistoria = null
        koulutussuunnitelma.vahvuudet = null
        koulutussuunnitelma.tulevaisuudenVisiointi = null
        koulutussuunnitelma.osaamisenKartuttaminen = null
        koulutussuunnitelma.elamankentta = null
    }

    fun clearPaivakirjamerkintaContent(paivakirjamerkinta: Paivakirjamerkinta) {
        paivakirjamerkinta.oppimistapahtumanNimi = ""
        paivakirjamerkinta.muunAiheenNimi = ""
        paivakirjamerkinta.reflektio = null
    }

    fun clearSeurantajaksoContent(seurantajakso: Seurantajakso) {

        seurantajakso.omaArviointi = ""
        seurantajakso.lisahuomioita = null
        seurantajakso.seuraavanJaksonTavoitteet = null
        seurantajakso.huolenaiheet = null
        seurantajakso.kouluttajanArvio = null
        seurantajakso.erikoisalanTyoskentelyvalmiudet = null
        seurantajakso.jatkotoimetJaRaportointi = null
        seurantajakso.seurantakeskustelunYhteisetMerkinnat = null
        seurantajakso.korjausehdotus = null
    }
}
