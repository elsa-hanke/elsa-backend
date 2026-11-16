package fi.elsapalvelu.elsa.service.kayttaja.etusivu

import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.koulutus.Opintosuoritus
import fi.elsapalvelu.elsa.domain.koulutus.OpintosuoritusTyyppiEnum
import fi.elsapalvelu.elsa.repository.koejakso.KoejaksonAloituskeskusteluRepository
import fi.elsapalvelu.elsa.repository.koejakso.KoejaksonKoulutussopimusRepository
import fi.elsapalvelu.elsa.repository.koejakso.KoejaksonVastuuhenkilonArvioRepository
import fi.elsapalvelu.elsa.required
import fi.elsapalvelu.elsa.service.dto.enumeration.KoejaksoTila
import fi.elsapalvelu.elsa.service.koejakso.KoejaksonTilasaannot
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class EtusivunKoejaksopalvelu(
    private val koejaksonKoulutussopimusRepository: KoejaksonKoulutussopimusRepository,
    private val koejaksonVastuuhenkilonArvioRepository: KoejaksonVastuuhenkilonArvioRepository,
    private val koejaksonAloituskeskusteluRepository: KoejaksonAloituskeskusteluRepository
) {

    fun haeKoejaksonTila(opintooikeus: Opintooikeus, opintosuoritukset: Sequence<Opintosuoritus>): KoejaksoTila {
        if (onkoKoejaksonSuoritusmerkinta(opintosuoritukset)) {
            return KoejaksonTilasaannot.maaritaYhteenvetotila(hyvaksyttySuoritus = true)
        }

        val koulutussopimus = koejaksonKoulutussopimusRepository.findByOpintooikeusId(opintooikeus.id.required())
        val vastuuhenkilonArvio = koejaksonVastuuhenkilonArvioRepository.findByOpintooikeusId(opintooikeus.id.required())
        val aloituskeskustelu = koejaksonAloituskeskusteluRepository.findByOpintooikeusId(opintooikeus.id.required())

        return KoejaksonTilasaannot.maaritaYhteenvetotila(
            vastuuhenkilonArvio = vastuuhenkilonArvio.orElse(null),
            koulutussopimus = koulutussopimus.orElse(null),
            aloituskeskustelu = aloituskeskustelu.orElse(null)
        )
    }

    fun onkoKoejaksonSuoritusmerkinta(opintosuoritukset: Sequence<Opintosuoritus>) =
        opintosuoritukset.any { it.tyyppi?.nimi == OpintosuoritusTyyppiEnum.KOEJAKSO && it.hyvaksytty }
}
