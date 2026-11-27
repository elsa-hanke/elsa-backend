package fi.elsapalvelu.elsa.cleanup

import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.repository.kayttaja.OpintooikeusRepository
import org.springframework.stereotype.Component

@Component
class OpintooikeusCleanupService(
    private val opintooikeusRepository: OpintooikeusRepository,
    private val erikoistuvaLaakariCleanupService: ErikoistuvaLaakariCleanupService,
    private val koulutusSuunnitelmaCleanupService: KoulutusSuunnitelmaCleanupService,
    private val opintosuorituksetCleanupService: OpintosuorituksetCleanupService,
    private val teoriakoulutusCleanupService: TeoriakoulutusCleanupService,
    private val tyoskentelyjaksoCleanupService: TyoskentelyjaksoCleanupService
) {

    fun clearOpintooikeus(opintooikeus: Opintooikeus) {

        /*
            TODO Miten käsitetään annetut valtuutukset? Poistetaanko ne vai jätetäänkö ne?
            Jos poistetaan, niin miten raportoidaan, että kuinka monta erikoistuvaa vastuuhenkilöllä on
            opintooikeus.annetutValtuutukset
        */
        // opintooikeus.asetus?

        opintooikeus.koulutussuunnitelma?.let { koulutusSuunnitelmaCleanupService.clearKoulutusSuunnitelma(it) }

        // opintooikeus.opintoopas?

        opintooikeus.opintosuoritukset?.let { opintosuorituksetCleanupService.cleanOpintosuoritukset(it) }

        opintooikeus.opiskelijatunnus = ""
        opintooikeus.teoriakoulutukset?.let { teoriakoulutusCleanupService.cleanTeoriakoulutukset(it) }
        opintooikeus.tyoskentelyjaksot?.let { tyoskentelyjaksoCleanupService.cleanTyoskentelyjaksot(it) }

        opintooikeusRepository.save(opintooikeus)


        /*
        keskeytysaika
koejakson_aloituskeskustelu
koejakson_kehittamistoimenpiteet
koejakson_koulutussopimus
koejakson_loppukeskustelu
koejakson_valiarviointi
koejakson_vastuuhenkilon_arvio
koulutusjakso
koulutussopimuksen_kouluttaja
koulutussuunnitelma
opintooikeus
opintooikeus_herate
paivakirjamerkinta
rel_paivakirjamerkinta__aihekategoria
rel_paivakirjamerkinta__aihekategoria_audit
seurantajakso
suoritemerkinta
suoritusarvioinnin_arviointityokalun_vastaus
suoritusarvioinnin_kommentti
suoritusarvioinnin_kommentti_audit
suoritusarviointi
teoriakoulutus
terveyskeskuskoulutusjakson_hyvaksynta
valmistumispyynto
         */
    }

}
