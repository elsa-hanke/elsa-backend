package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.perustiedot.Yliopisto
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.domain.koulutus.Opintoopas
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

@SpringBootTest(classes = [ElsaBackendApp::class])
@Transactional
class TestisuhteetIT {
    @Autowired private lateinit var em: EntityManager

    @Test
    fun `annettu yliopisto valitaan vaikka tietokannassa on samannimisia yliopistoja`() {
        val nimi = YliopistoEnum.TAMPEREEN_YLIOPISTO
        em.persist(Yliopisto(nimi = nimi))
        val yliopisto = Yliopisto(nimi = nimi).also(em::persist)

        val opintooikeus = ErikoistuvaLaakariHelper.createEntity(em, yliopisto = yliopisto).getOpintooikeusKaytossa()!!
        em.flush()
        em.clear()

        assertThat(em.find(Opintooikeus::class.java, opintooikeus.id).yliopisto!!.id).isEqualTo(yliopisto.id)
    }

    @Test
    fun `toisen erikoistuvan luonti ei muuta ensimmaisen opintoopasta tai erikoisalaa`() {
        val ensimmainen = ErikoistuvaLaakariHelper.createEntity(em).getOpintooikeusKaytossa()!!
        val ensimmaisenOpas = ensimmainen.opintoopas!!
        val ensimmaisenErikoisala = ensimmainen.erikoisala!!
        val toinenErikoisala = ErikoisalaHelper.createEntity(nimi = "Toinen erikoisala").also(em::persist)

        val toinen = ErikoistuvaLaakariHelper.createEntity(em, erikoisala = toinenErikoisala).getOpintooikeusKaytossa()!!
        em.flush()
        em.clear()

        assertThat(toinen.opintoopas!!.id).isNotEqualTo(ensimmaisenOpas.id)
        assertThat(em.find(Opintoopas::class.java, ensimmaisenOpas.id).erikoisala!!.id).isEqualTo(ensimmaisenErikoisala.id)
        assertThat(em.find(Opintooikeus::class.java, toinen.id).erikoisala!!.id).isEqualTo(toinenErikoisala.id)
    }

    @Test
    fun `nimenomaisesti annettu opintoopas ja erikoisala voidaan jakaa`() {
        val ensimmainen = ErikoistuvaLaakariHelper.createEntity(em).getOpintooikeusKaytossa()!!
        val toinen = ErikoistuvaLaakariHelper.createEntity(
            em, erikoisala = ensimmainen.erikoisala, opintoopas = ensimmainen.opintoopas, yliopisto = ensimmainen.yliopisto, asetus = ensimmainen.asetus
        ).getOpintooikeusKaytossa()!!

        assertThat(toinen.opintoopas).isSameAs(ensimmainen.opintoopas)
        assertThat(toinen.erikoisala).isSameAs(ensimmainen.erikoisala)
    }

    @Test
    fun `tyoskentelyjakso kayttaa annettua opintooikeutta aiemmista tietueista riippumatta`() {
        ErikoistuvaLaakariHelper.createEntity(em)
        val toinen = ErikoistuvaLaakariHelper.createEntity(em).getOpintooikeusKaytossa()!!

        val jakso = TyoskentelyjaksoHelper.createEntity(em, opintooikeus = toinen)
        val paivitettyJakso = TyoskentelyjaksoHelper.createUpdatedEntity(em, opintooikeus = toinen)

        assertThat(jakso.opintooikeus).isSameAs(toinen)
        assertThat(paivitettyJakso.opintooikeus).isSameAs(toinen)
        assertThat(paivitettyJakso.tyoskentelypaikka).isNotSameAs(jakso.tyoskentelypaikka)
    }

    @Test
    fun `ilman kayttajaa luotu asiakirja ei liity aiempaan erikoistuvaan`() {
        val ensimmainen = ErikoistuvaLaakariHelper.createEntity(em).getOpintooikeusKaytossa()!!

        val asiakirja = AsiakirjaHelper.createEntity(em)

        assertThat(asiakirja.opintooikeus!!.id).isNotEqualTo(ensimmainen.id)
    }

    @Test
    fun `arviointiasteikko tulee annetun tyoskentelyjakson opintooppaasta`() {
        ErikoistuvaLaakariHelper.createEntity(em)
        val toinen = ErikoistuvaLaakariHelper.createEntity(em).getOpintooikeusKaytossa()!!
        val jakso = TyoskentelyjaksoHelper.createEntity(em, opintooikeus = toinen).also(em::persist)
        val arvioija = KayttajaHelper.createEntity(em).also(em::persist)

        val arviointi = SuoritusarviointiHelper.createEntity(em, tyoskentelyjakso = jakso, arvioinninAntaja = arvioija)

        assertThat(arviointi.tyoskentelyjakso).isSameAs(jakso)
        assertThat(arviointi.arvioinninAntaja).isSameAs(arvioija)
        assertThat(arviointi.arviointiasteikko).isSameAs(toinen.opintoopas!!.arviointiasteikko)
        assertThat(arviointi.arvioitavatKokonaisuudet.single().arvioitavaKokonaisuus!!.kategoria!!.erikoisala)
            .isSameAs(toinen.erikoisala)
    }

    @Test
    fun `annettu poissaolon syy ei vaihdu tietokannan ensimmaiseksi syyksi`() {
        val jakso = TyoskentelyjaksoHelper.createEntity(em).also(em::persist)
        em.persist(PoissaolonSyyHelper.createEntity())
        val toinenSyy = PoissaolonSyyHelper.createUpdatedEntity().also(em::persist)

        val keskeytys = KeskeytysaikaHelper.createEntity(em, jakso, poissaolonSyy = toinenSyy)

        assertThat(keskeytys.poissaolonSyy).isSameAs(toinenSyy)
        assertThat(keskeytys.tyoskentelyjakso).isSameAs(jakso)
    }

    @Test
    fun `asiakirjan opintooikeus tulee annetusta tyoskentelyjaksosta`() {
        ErikoistuvaLaakariHelper.createEntity(em)
        val opintooikeus = ErikoistuvaLaakariHelper.createEntity(em).getOpintooikeusKaytossa()!!
        val jakso = TyoskentelyjaksoHelper.createEntity(em, opintooikeus = opintooikeus).also(em::persist)

        val asiakirja = AsiakirjaHelper.createEntity(em, tyoskentelyjakso = jakso)

        assertThat(asiakirja.opintooikeus).isSameAs(opintooikeus)
        assertThat(asiakirja.tyoskentelyjakso).isSameAs(jakso)
    }

    @Test
    fun `annettu kayttaja maarittaa omistajan aiemmista erikoistuvista riippumatta`() {
        ErikoistuvaLaakariHelper.createEntity(em)
        val toinen = ErikoistuvaLaakariHelper.createEntity(em)
        val kayttajatili = requireNotNull(toinen.kayttaja?.user)

        val jakso = TyoskentelyjaksoHelper.createEntity(em, user = kayttajatili)
        val merkinta = PaivakirjamerkintaHelper.createEntity(em, user = kayttajatili)

        assertThat(jakso.opintooikeus).isSameAs(toinen.getOpintooikeusKaytossa())
        assertThat(merkinta.opintooikeus).isSameAs(toinen.getOpintooikeusKaytossa())
        assertThat(Testisuhteet.kayttaja(em, kayttajatili)).isSameAs(toinen.kayttaja)
    }
}
