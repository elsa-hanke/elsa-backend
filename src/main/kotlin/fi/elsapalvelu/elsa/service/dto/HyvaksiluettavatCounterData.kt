package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.domain.PoissaolonSyy

data class HyvaksiluettavatCounterData(
    // Poissaolon syy tyyppi: VAHENNETAAN_YLIMENEVA_AIKA.
    // Voidaan hyväksilukea vain kerran 30 päivää riippumatta siitä
    // kuinka monta ja kuinka monelle vuodelle jakautuvaa työskentelyjaksoa
    // erikoistuva on kirjannut.
    var hyvaksiluettavatDays: MutableMap<PoissaolonSyy, Double> = mutableMapOf(),
    // Poissaolon syy tyyppi: VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI.
    // Voidaan hyväksilukea 30 päivää per kalenterivuosi.
    var hyvaksiluettavatPerYearMap: MutableMap<Int, Double> = mutableMapOf()
)
