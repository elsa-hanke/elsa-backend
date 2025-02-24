package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.domain.enumeration.OpintooikeudenTila
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.web.rest.findAll
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils
import java.time.LocalDate
import jakarta.persistence.EntityManager

class ErikoistuvaLaakariHelper {

    companion object {

        private const val DEFAULT_PUHELINNUMERO = "AAAAAAAAAA"
        private const val UPDATED_PUHELINNUMERO = "BBBBBBBBBB"

        private const val DEFAULT_OPISKELIJATUNNUS = "AAAAAAAAAA"
        private const val UPDATED_OPISKELIJATUNNUS = "BBBBBBBBBB"

        val DEFAULT_YLIOPISTO = YliopistoEnum.TAMPEREEN_YLIOPISTO
        val DEFAULT_ASETUS = "55/2020"
        val DEFAULT_ERIKOISTUMISEN_ALOITUSPAIVA: LocalDate = LocalDate.ofEpochDay(10L)
        val DEFAULT_OPINTOOIKEUDEN_ALKAMISPAIVA: LocalDate = LocalDate.ofEpochDay(0L)
        val DEFAULT_OPINTOOIKEUDEN_PAATTYMISPAIVA: LocalDate = LocalDate.now().plusYears(2)
        val DEFAULT_LAILLISTAMISPAIVA: LocalDate = LocalDate.ofEpochDay(15L)
        val DEFAULT_LAILLISTAMISTODISTUS_DATA = byteArrayOf(0x2E, 0x38)
        val DEFAULT_LAILLISTAMISTODISTUS_DATA_AS_STRING = "Ljg="
        val DEFAULT_LAILLISTAMISTODISTUS_NIMI = "laillistamistodistus.pdf"
        val DEFAULT_LAILLISTAMISTODISTUS_TYYPPI = "application/pdf"

        @JvmStatic
        fun createEntity(
            em: EntityManager,
            user: User? = null,
            opintooikeudenAlkamispaiva: LocalDate? = DEFAULT_OPINTOOIKEUDEN_ALKAMISPAIVA,
            opintooikeudenPaattymispaiva: LocalDate? = DEFAULT_OPINTOOIKEUDEN_PAATTYMISPAIVA,
            erikoisala: Erikoisala? = null,
            opintoopas: Opintoopas? = null,
            yliopisto: Yliopisto? = null,
            asetus: Asetus? = null,
            yliopistoOpintooikeusId: String? = RandomStringUtils.randomAlphabetic(8),
            laillistamispaiva: LocalDate? = DEFAULT_LAILLISTAMISPAIVA,
            laillistamistodistusData: ByteArray? = DEFAULT_LAILLISTAMISTODISTUS_DATA,
            laillistamistodistusNimi: String? = DEFAULT_LAILLISTAMISTODISTUS_NIMI,
            laillistamistodistusTyyppi: String? = DEFAULT_LAILLISTAMISTODISTUS_TYYPPI
        ): ErikoistuvaLaakari {
            val erikoistuvaLaakari = ErikoistuvaLaakari()

            var kayttaja = user?.let { em.findAll(Kayttaja::class).firstOrNull { it.user == user } }
            if (kayttaja == null) {
                kayttaja = KayttajaHelper.createEntity(em, user)
                em.persist(kayttaja)
                em.flush()
            }

            erikoistuvaLaakari.syntymaaika = LocalDate.ofEpochDay(5L)
            erikoistuvaLaakari.kayttaja = kayttaja
            erikoistuvaLaakari.laillistamispaiva = laillistamispaiva
            erikoistuvaLaakari.laillistamistodistus = AsiakirjaData(data = laillistamistodistusData)
            erikoistuvaLaakari.laillistamispaivanLiitetiedostonNimi = laillistamistodistusNimi
            erikoistuvaLaakari.laillistamispaivanLiitetiedostonTyyppi = laillistamistodistusTyyppi
            em.persist(erikoistuvaLaakari)

            var erikoistuvanYliopisto = yliopisto
            if (erikoistuvanYliopisto == null) {
                if (em.findAll(Yliopisto::class).isEmpty()) {
                    erikoistuvanYliopisto = Yliopisto(nimi = DEFAULT_YLIOPISTO)
                    em.persist(erikoistuvanYliopisto)
                    em.flush()
                } else {
                    erikoistuvanYliopisto = em.findAll(Yliopisto::class).get(0)
                }
            }

            var erikoistuvanErikoisala = erikoisala
            if (erikoistuvanErikoisala == null) {
                if (em.findAll(Erikoisala::class).isEmpty()) {
                    erikoistuvanErikoisala = ErikoisalaHelper.createEntity()
                    em.persist(erikoistuvanErikoisala)
                    em.flush()
                } else {
                    erikoistuvanErikoisala = em.findAll(Erikoisala::class).get(0)
                }
            }

            var opintoopasKaytossa = opintoopas
            if (opintoopasKaytossa == null) {
                if (em.findAll(Opintoopas::class).isEmpty()) {
                    opintoopasKaytossa =
                        OpintoopasHelper.createEntity(em, erikoisala = erikoistuvanErikoisala)
                    em.persist(opintoopasKaytossa)
                    em.flush()
                } else {
                    opintoopasKaytossa = em.findAll(Opintoopas::class).get(0)
                    opintoopasKaytossa.erikoisala = erikoistuvanErikoisala
                }
            }

            var asetusKaytossa = asetus
            if (asetusKaytossa == null) {
                if (em.findAll(Asetus::class).isEmpty()) {
                    asetusKaytossa = Asetus(nimi = DEFAULT_ASETUS)
                    em.persist(asetusKaytossa)
                    em.flush()
                } else {
                    asetusKaytossa = em.findAll(Asetus::class).get(0)
                }
            }

            val opintooikeus = Opintooikeus(
                yliopistoOpintooikeusId = yliopistoOpintooikeusId,
                opintooikeudenMyontamispaiva = opintooikeudenAlkamispaiva,
                opintooikeudenPaattymispaiva = opintooikeudenPaattymispaiva,
                viimeinenKatselupaiva = opintooikeudenPaattymispaiva?.plusMonths(6),
                opiskelijatunnus = DEFAULT_OPISKELIJATUNNUS,
                osaamisenArvioinninOppaanPvm = DEFAULT_ERIKOISTUMISEN_ALOITUSPAIVA,
                erikoistuvaLaakari = erikoistuvaLaakari,
                yliopisto = erikoistuvanYliopisto,
                erikoisala = erikoistuvanErikoisala,
                opintoopas = opintoopasKaytossa,
                asetus = asetusKaytossa,
                kaytossa = true,
                tila = OpintooikeudenTila.AKTIIVINEN
            )
            em.persist(opintooikeus)
            em.flush()

            erikoistuvaLaakari.opintooikeudet.add(opintooikeus)
            erikoistuvaLaakari.aktiivinenOpintooikeus = opintooikeus.id

            return erikoistuvaLaakari
        }

        @JvmStatic
        fun createUpdatedEntity(em: EntityManager): ErikoistuvaLaakari {
            val erikoistuvaLaakari = ErikoistuvaLaakari()

            // Lisätään pakollinen tieto
            val kayttaja: Kayttaja
            if (em.findAll(Kayttaja::class).isEmpty()) {
                kayttaja = KayttajaHelper.createUpdatedEntity(em)
                em.persist(kayttaja)
                em.flush()
            } else {
                kayttaja = em.findAll(Kayttaja::class).get(0)
            }
            erikoistuvaLaakari.kayttaja = kayttaja

            return erikoistuvaLaakari
        }
    }
}
