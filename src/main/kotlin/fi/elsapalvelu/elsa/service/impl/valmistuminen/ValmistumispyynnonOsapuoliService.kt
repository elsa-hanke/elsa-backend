package fi.elsapalvelu.elsa.service.impl.valmistuminen

import fi.elsapalvelu.elsa.config.YEK_ERIKOISALA_ID
import fi.elsapalvelu.elsa.domain.kayttaja.Kayttaja
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.domain.perustiedot.VastuuhenkilonTehtavatyyppiEnum
import fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto
import fi.elsapalvelu.elsa.repository.kayttaja.KayttajaRepository
import fi.elsapalvelu.elsa.repository.kayttaja.OpintooikeusRepository
import fi.elsapalvelu.elsa.repository.kayttaja.UserRepository
import fi.elsapalvelu.elsa.repository.valmistuminen.ValmistumispyynnonTarkistusRepository
import fi.elsapalvelu.elsa.repository.valmistuminen.ValmistumispyyntoRepository
import fi.elsapalvelu.elsa.required
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.service.constants.KAYTTAJA_NOT_FOUND_ERROR
import fi.elsapalvelu.elsa.service.constants.KAYTTAJA_YLIOPISTO_ERIKOISALA_NOT_FOUND_ERROR
import fi.elsapalvelu.elsa.service.constants.OPINTOOIKEUS_NOT_FOUND_ERROR
import fi.elsapalvelu.elsa.service.constants.VALMISTUMISPYYNTO_NOT_FOUND_ERROR
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class ValmistumispyynnonOsapuoliService(
    private val kayttajaRepository: KayttajaRepository,
    private val opintooikeusRepository: OpintooikeusRepository,
    private val userRepository: UserRepository,
    private val valmistumispyyntoRepository: ValmistumispyyntoRepository,
    private val tarkistusRepository: ValmistumispyynnonTarkistusRepository
) {

    fun haeKayttaja(userId: String): Kayttaja = kayttajaRepository.findOneByUserId(userId)
        .orElseThrow { EntityNotFoundException(KAYTTAJA_NOT_FOUND_ERROR) }

    fun haeOpintooikeus(id: Long): Opintooikeus = opintooikeusRepository.findByIdOrNull(id)
        ?: throw EntityNotFoundException(OPINTOOIKEUS_NOT_FOUND_ERROR)

    fun haeYliopisto(kayttaja: Kayttaja) =
        kayttaja.yliopistotAndErikoisalat.firstOrNull()?.yliopisto
        ?: throw EntityNotFoundException(KAYTTAJA_YLIOPISTO_ERIKOISALA_NOT_FOUND_ERROR)

    fun haeErikoisalaIds(kayttaja: Kayttaja) =
        kayttaja.yliopistotAndErikoisalat.map { it.erikoisala?.id.required() }

    fun paivitaYhteystiedot(kayttajatili: User?, sahkoposti: String?, puhelinnumero: String?) {
        kayttajatili?.let {
            it.email = sahkoposti
            it.phoneNumber = puhelinnumero
            userRepository.save(it)
        }
    }

    fun haeValmistumispyynto(
        id: Long,
        kayttaja: Kayttaja,
        yliopistoId: Long,
        tehtava: VastuuhenkilonTehtavatyyppiEnum
    ): Valmistumispyynto {
        val valmistumispyynto = valmistumispyyntoRepository.findByIdAndOpintooikeusYliopistoId(
            id,
            yliopistoId
        ) ?: throw valmistumispyyntoaEiLoydy()
        val yek = valmistumispyynto.opintooikeus?.erikoisala?.id == YEK_ERIKOISALA_ID
        val onkoTehtava = kayttaja.yliopistotAndErikoisalat.any { yliopistoErikoisala ->
            val tehtavat = yliopistoErikoisala.vastuuhenkilonTehtavat.map { it.nimi }
            if (yek) {
                tehtavat.contains(VastuuhenkilonTehtavatyyppiEnum.YEK_VALMISTUMINEN)
            } else {
                yliopistoErikoisala.erikoisala?.id ==
                    valmistumispyynto.opintooikeus?.erikoisala?.id &&
                    tehtavat.contains(tehtava)
            }
        }
        return valmistumispyynto.takeIf { onkoTehtava } ?: throw valmistumispyyntoaEiLoydy()
    }

    fun haeValmistumispyyntoOpintooikeudella(opintooikeusId: Long): Valmistumispyynto =
        valmistumispyyntoRepository.findByOpintooikeusId(opintooikeusId)
            ?: throw EntityNotFoundException(VALMISTUMISPYYNTO_NOT_FOUND_ERROR)

    fun haeOsaamisenArvioija(yliopistoId: Long, erikoisalaId: Long): Kayttaja =
        kayttajaRepository
            .findOneByAuthoritiesYliopistoErikoisalaAndVastuuhenkilonTehtavatyyppi(
                listOf(VASTUUHENKILO),
                yliopistoId,
                erikoisalaId,
                VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI
            ) ?: throw EntityNotFoundException(COMPETENCE_REVIEWER_NOT_FOUND)

    fun haeHyvaksyja(yliopistoId: Long, erikoisalaId: Long): Kayttaja {
        if (erikoisalaId == YEK_ERIKOISALA_ID) {
            return kayttajaRepository
                .findOneByAuthoritiesYliopistoAndVastuuhenkilonTehtavatyyppi(
                    listOf(VASTUUHENKILO),
                    yliopistoId,
                    VastuuhenkilonTehtavatyyppiEnum.YEK_VALMISTUMINEN
                ) ?: throw EntityNotFoundException(FINAL_APPROVER_NOT_FOUND)
        }
        return kayttajaRepository
            .findOneByAuthoritiesYliopistoErikoisalaAndVastuuhenkilonTehtavatyyppi(
                listOf(VASTUUHENKILO),
                yliopistoId,
                erikoisalaId,
                VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_HYVAKSYNTA
            ) ?: throw EntityNotFoundException(FINAL_APPROVER_NOT_FOUND)
    }

    fun onkoOsaamisenArviointiAvoin(userId: String, valmistumispyyntoId: Long): Boolean {
        val kayttaja = haeKayttaja(userId)
        val valmistumispyynto = haeValmistumispyynto(
            valmistumispyyntoId,
            kayttaja,
            haeYliopisto(kayttaja).id.required(),
            VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI
        )
        return valmistumispyynto.erikoistujanKuittausaika != null &&
            valmistumispyynto.vastuuhenkiloOsaamisenArvioijaKuittausaika == null
    }

    fun onkoVirkailijanTarkistusAvoin(userId: String, valmistumispyyntoId: Long): Boolean {
        val kayttaja = haeKayttaja(userId)
        val yliopistoId = kayttaja.yliopistot.first().id.required()
        val tarkistus = tarkistusRepository
            .findByValmistumispyyntoIdAndValmistumispyyntoOpintooikeusYliopistoId(
                valmistumispyyntoId,
                yliopistoId
            )
        val valmistumispyynto = tarkistus?.valmistumispyynto
            ?: valmistumispyyntoRepository.findByIdAndOpintooikeusYliopistoId(
                valmistumispyyntoId,
                yliopistoId
            )
        return valmistumispyynto?.erikoistujanKuittausaika != null &&
            (valmistumispyynto.opintooikeus?.erikoisala?.id == YEK_ERIKOISALA_ID ||
                valmistumispyynto.vastuuhenkiloOsaamisenArvioijaKuittausaika != null) &&
            valmistumispyynto.virkailijanKuittausaika == null
    }

    fun onkoLopullinenHyvaksyntaAvoin(userId: String, valmistumispyyntoId: Long): Boolean {
        val kayttaja = haeKayttaja(userId)
        val valmistumispyynto = haeValmistumispyynto(
            valmistumispyyntoId,
            kayttaja,
            haeYliopisto(kayttaja).id.required(),
            VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_HYVAKSYNTA
        )
        return valmistumispyynto.virkailijanKuittausaika != null &&
            valmistumispyynto.vastuuhenkiloHyvaksyjaKuittausaika == null
    }

    fun valmistumispyyntoaEiLoydy() = EntityNotFoundException(
        "Valmistumispyyntöä ei löydy tai sinulla ei ole oikeuksia tarkastella " +
            "kyseistä valmistumispyyntöä."
    )

    private companion object {
        const val COMPETENCE_REVIEWER_NOT_FOUND =
            "Vastuuhenkilöä, joka hyväksyisi osaamisen arvioinnin, ei löydy."
        const val FINAL_APPROVER_NOT_FOUND =
            "Vastuuhenkilöä, joka hyväksyisi valmistumispyynnon, ei löydy."
    }
}
