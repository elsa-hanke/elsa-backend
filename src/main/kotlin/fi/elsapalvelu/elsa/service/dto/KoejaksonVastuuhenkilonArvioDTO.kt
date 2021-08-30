package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDate
import javax.validation.constraints.NotNull

data class KoejaksonVastuuhenkilonArvioDTO(

    var id: Long? = null,

    @get: NotNull
    var erikoistuvanNimi: String? = null,

    @get: NotNull
    var erikoistuvanErikoisala: String? = null,

    @get: NotNull
    var erikoistuvanOpiskelijatunnus: String? = null,

    @get: NotNull
    var erikoistuvanYliopisto: String? = null,

    @get: NotNull
    var vastuuhenkilo: KoejaksonKouluttajaDTO? = null,

    var koejaksoHyvaksytty: Boolean? = null,

    var vastuuhenkiloHyvaksynyt: Boolean? = null,

    var vastuuhenkilonKuittausaika: LocalDate? = null,

    var perusteluHylkaamiselle: String? = null,

    var hylattyArviointiKaytyLapiKeskustellen: Boolean? = null,

    var muokkauspaiva: LocalDate? = null,

    var erikoistuvaAllekirjoittanut: Boolean? = null,

    var erikoistuvanAllekirjoitusaika: LocalDate? = null

) : Serializable
