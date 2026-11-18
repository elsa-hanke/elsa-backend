package fi.elsapalvelu.elsa.service.tyoskentely

import fi.elsapalvelu.elsa.required
import fi.elsapalvelu.elsa.service.PoissaolonSyyService
import fi.elsapalvelu.elsa.service.dto.tyoskentely.KeskeytysaikaFormDTO
import fi.elsapalvelu.elsa.service.dto.tyoskentely.TyoskentelyjaksoFormDTO
import fi.elsapalvelu.elsa.service.kayttaja.AsiakirjaService
import fi.elsapalvelu.elsa.service.perustiedot.ErikoisalaService
import fi.elsapalvelu.elsa.service.perustiedot.KuntaService
import org.springframework.stereotype.Service

@Service
class TyoskentelyjaksojenLomakepalvelu(
    private val kunnat: KuntaService,
    private val erikoisalat: ErikoisalaService,
    private val asiakirjat: AsiakirjaService,
    private val poissaolonSyyt: PoissaolonSyyService,
    private val tyoskentelyjaksot: TyoskentelyjaksoService
) {
    fun haeTyoskentelyjaksonLomake(opintooikeusId: Long): TyoskentelyjaksoFormDTO {
        val lomake = TyoskentelyjaksoFormDTO()
        lomake.kunnat = kunnat.findAll().toMutableSet()
        lomake.erikoisalat = erikoisalat.findAll().toMutableSet()
        lomake.reservedAsiakirjaNimet = asiakirjat.findAllByOpintooikeusId(opintooikeusId)
            .map { it.nimi.required() }.toMutableSet()
        return lomake
    }

    fun haeKeskeytysajanLomake(opintooikeusId: Long): KeskeytysaikaFormDTO {
        val lomake = KeskeytysaikaFormDTO()
        lomake.poissaolonSyyt = poissaolonSyyt.findAllByOpintooikeusId(opintooikeusId).toMutableSet()
        lomake.tyoskentelyjaksot = tyoskentelyjaksot.findAllByOpintooikeusId(opintooikeusId).toMutableSet()
        return lomake
    }
}
