package fi.elsapalvelu.elsa.extensions

import fi.elsapalvelu.elsa.config.YEK_ERIKOISALA_ID
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto

/** True when the study right belongs to the YEK (general practice) programme. */
fun Opintooikeus?.isYek(): Boolean = this?.erikoisala?.id == YEK_ERIKOISALA_ID

/** True when the graduation request belongs to the YEK (general practice) programme. */
fun Valmistumispyynto.isYek(): Boolean = opintooikeus.isYek()
