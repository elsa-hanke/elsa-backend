package fi.elsapalvelu.elsa.service.impl.valmistuminen

import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordProperties

/** Keep the exact attachment selection used to generate the ordinary PDF until its archive copy is built. */
data class CreatedValmistumisAsiakirjat(
    val records: List<RecordProperties>,
    val attachmentSources: List<Asiakirja>
)
