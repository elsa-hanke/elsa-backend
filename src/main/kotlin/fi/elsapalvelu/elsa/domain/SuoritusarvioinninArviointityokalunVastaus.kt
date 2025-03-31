package fi.elsapalvelu.elsa.domain

import jakarta.persistence.*
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable

@Entity
@Table(name = "suoritusarvioinnin_arviointityokalun_vastaus")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class SuoritusarvioinninArviointityokalunVastaus(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "suoritusarviointi_id", nullable = false)
    var suoritusarviointi: Suoritusarviointi? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "arviointityokalu_id", nullable = false)
    var arviointityokalu: Arviointityokalu? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "arviointityokalu_kysymys_id", nullable = false)
    var arviointityokaluKysymys: ArviointityokaluKysymys? = null,

    @Column(name = "teksti_vastaus")
    var tekstiVastaus: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "valittu_vaihtoehto_id")
    var valittuVaihtoehto: ArviointityokaluKysymysVaihtoehto? = null

) : Serializable
