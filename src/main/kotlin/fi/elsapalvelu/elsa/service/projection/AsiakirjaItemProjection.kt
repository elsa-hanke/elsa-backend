package fi.elsapalvelu.elsa.service.projection

interface AsiakirjaItemProjection {
    val nimi: String
    val tyyppi: String
    val data: ByteArray
}
