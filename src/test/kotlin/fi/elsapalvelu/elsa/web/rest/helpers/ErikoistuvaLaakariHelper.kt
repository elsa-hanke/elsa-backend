package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.domain.koejakso.*
import fi.elsapalvelu.elsa.domain.tyoskentely.*
import fi.elsapalvelu.elsa.domain.arviointi.*
import fi.elsapalvelu.elsa.domain.suoritteet.*
import fi.elsapalvelu.elsa.domain.koulutus.*
import fi.elsapalvelu.elsa.domain.seuranta.*
import fi.elsapalvelu.elsa.domain.valmistuminen.*
import fi.elsapalvelu.elsa.domain.kayttaja.*
import fi.elsapalvelu.elsa.domain.perustiedot.*
import fi.elsapalvelu.elsa.domain.kayttaja.OpintooikeudenTila
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import java.time.LocalDate
import jakarta.persistence.EntityManager

object ErikoistuvaLaakariHelper {
    private const val DEFAULT_OPISKELIJATUNNUS = "AAAAAAAAAA"
    val DEFAULT_YLIOPISTO = YliopistoEnum.TAMPEREEN_YLIOPISTO
    const val DEFAULT_ASETUS = "55/2020"
    val DEFAULT_ERIKOISTUMISEN_ALOITUSPAIVA: LocalDate = LocalDate.ofEpochDay(10L)
    val DEFAULT_OPINTOOIKEUDEN_ALKAMISPAIVA: LocalDate = LocalDate.ofEpochDay(0L)
    val DEFAULT_OPINTOOIKEUDEN_PAATTYMISPAIVA: LocalDate = LocalDate.now().plusYears(2)
    val DEFAULT_LAILLISTAMISPAIVA: LocalDate = LocalDate.ofEpochDay(15L)
    val DEFAULT_LAILLISTAMISTODISTUS_DATA = byteArrayOf(0x2E, 0x38)
    const val DEFAULT_LAILLISTAMISTODISTUS_DATA_AS_STRING = "Ljg="
    const val DEFAULT_LAILLISTAMISTODISTUS_NIMI = "laillistamistodistus.pdf"
    const val DEFAULT_LAILLISTAMISTODISTUS_TYYPPI = "application/pdf"

    @Suppress("LongParameterList")
    fun createEntity(em: EntityManager, user: User? = null, opintooikeudenAlkamispaiva: LocalDate? = DEFAULT_OPINTOOIKEUDEN_ALKAMISPAIVA,
        opintooikeudenPaattymispaiva: LocalDate? = DEFAULT_OPINTOOIKEUDEN_PAATTYMISPAIVA, erikoisala: Erikoisala? = null, opintoopas: Opintoopas? = null,
        yliopisto: Yliopisto? = null, asetus: Asetus? = null, yliopistoOpintooikeusId: String? = (1..8).map { ('a'..'z').random() }.joinToString(""),
        laillistamispaiva: LocalDate? = DEFAULT_LAILLISTAMISPAIVA, laillistamistodistusData: ByteArray? = DEFAULT_LAILLISTAMISTODISTUS_DATA,
        laillistamistodistusNimi: String? = DEFAULT_LAILLISTAMISTODISTUS_NIMI, laillistamistodistusTyyppi: String? = DEFAULT_LAILLISTAMISTODISTUS_TYYPPI): ErikoistuvaLaakari {
        val erikoistuvaLaakari = ErikoistuvaLaakari()

        val kayttaja = Testisuhteet.kayttaja(em, user)

        erikoistuvaLaakari.syntymaaika = LocalDate.ofEpochDay(5L)
        erikoistuvaLaakari.kayttaja = kayttaja
        erikoistuvaLaakari.laillistamispaiva = laillistamispaiva
        erikoistuvaLaakari.laillistamistodistus = AsiakirjaData(data = laillistamistodistusData)
        erikoistuvaLaakari.laillistamispaivanLiitetiedostonNimi = laillistamistodistusNimi
        erikoistuvaLaakari.laillistamispaivanLiitetiedostonTyyppi = laillistamistodistusTyyppi

        val erikoistuvanYliopisto = yliopisto ?: Testisuhteet.yliopisto(em, DEFAULT_YLIOPISTO)
        val erikoistuvanErikoisala = erikoisala ?: opintoopas?.erikoisala ?: Testisuhteet.akuuttilaaketiede(em)
        require(opintoopas == null || opintoopas.erikoisala == erikoistuvanErikoisala) {
            "Opintooppaan ja opinto-oikeuden erikoisalojen on vastattava toisiaan"
        }
        val opintoopasKaytossa = opintoopas ?: OpintoopasHelper.createEntity(em, erikoisala = erikoistuvanErikoisala).also(em::persist)
        val asetusKaytossa = asetus ?: Testisuhteet.asetus(em, DEFAULT_ASETUS)

        val opintooikeus = Opintooikeus(yliopistoOpintooikeusId = yliopistoOpintooikeusId, opintooikeudenMyontamispaiva = opintooikeudenAlkamispaiva,
            opintooikeudenPaattymispaiva = opintooikeudenPaattymispaiva, viimeinenKatselupaiva = opintooikeudenPaattymispaiva?.plusMonths(6),
            opiskelijatunnus = DEFAULT_OPISKELIJATUNNUS, osaamisenArvioinninOppaanPvm = DEFAULT_ERIKOISTUMISEN_ALOITUSPAIVA, erikoistuvaLaakari = erikoistuvaLaakari,
            yliopisto = erikoistuvanYliopisto, erikoisala = erikoistuvanErikoisala, opintoopas = opintoopasKaytossa, asetus = asetusKaytossa, kaytossa = true,
            tila = OpintooikeudenTila.AKTIIVINEN)
        erikoistuvaLaakari.opintooikeudet.add(opintooikeus)
        em.persist(erikoistuvaLaakari)
        em.persist(opintooikeus)
        em.flush()  // Hibernate inserts ErikoistuvaLaakari first (FK dependency), then Opintooikeus

        erikoistuvaLaakari.aktiivinenOpintooikeus = opintooikeus.id

        return erikoistuvaLaakari
    }

    fun createUpdatedEntity(em: EntityManager): ErikoistuvaLaakari {
        val erikoistuvaLaakari = ErikoistuvaLaakari()

        val kayttaja = KayttajaHelper.createUpdatedEntity(em).also(em::persist)
        erikoistuvaLaakari.kayttaja = kayttaja

        return erikoistuvaLaakari
    }
}
