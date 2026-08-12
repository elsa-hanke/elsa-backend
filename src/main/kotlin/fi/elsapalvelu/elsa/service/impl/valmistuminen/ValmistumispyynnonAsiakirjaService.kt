package fi.elsapalvelu.elsa.service.impl.valmistuminen

import fi.elsapalvelu.elsa.config.YEK_ERIKOISALA_ID
import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.kayttaja.Kayttaja
import fi.elsapalvelu.elsa.domain.perustiedot.VastuuhenkilonTehtavatyyppiEnum
import fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto
import fi.elsapalvelu.elsa.repository.kayttaja.AsiakirjaRepository
import fi.elsapalvelu.elsa.repository.kayttaja.KayttajaRepository
import fi.elsapalvelu.elsa.repository.valmistuminen.ValmistumispyyntoRepository
import fi.elsapalvelu.elsa.service.constants.KAYTTAJA_NOT_FOUND_ERROR
import fi.elsapalvelu.elsa.service.dto.kayttaja.AsiakirjaDTO
import fi.elsapalvelu.elsa.service.mapper.kayttaja.AsiakirjaMapper
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayInputStream

@Service
class ValmistumispyynnonAsiakirjaService(
    private val valmistumispyyntoRepository: ValmistumispyyntoRepository,
    private val asiakirjaRepository: AsiakirjaRepository,
    private val kayttajaRepository: KayttajaRepository,
    private val asiakirjaMapper: AsiakirjaMapper
) {

    @Transactional(readOnly = true)
    fun haeValmistumispyynnonAsiakirja(
        userId: String,
        valmistumispyyntoId: Long,
        asiakirjaId: Long
    ): AsiakirjaDTO? {
        val valmistumispyynto = valmistumispyyntoRepository.findByIdOrNull(valmistumispyyntoId)
            ?: return null
        val onkoKayttajaOsapuoli = listOf(
            valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.kayttaja,
            valmistumispyynto.vastuuhenkiloOsaamisenArvioija,
            valmistumispyynto.vastuuhenkiloHyvaksyja,
            valmistumispyynto.virkailija
        ).any { it?.user?.id == userId }
        val kuuluukoAsiakirjaValmistumispyyntoon =
            valmistumispyynto.yhteenvetoAsiakirja?.id == asiakirjaId ||
                valmistumispyynto.liitteetAsiakirja?.id == asiakirjaId

        return if (onkoKayttajaOsapuoli && kuuluukoAsiakirjaValmistumispyyntoon) {
            asiakirjaRepository.findByIdOrNull(asiakirjaId)?.toLadattavaDto()
        } else {
            null
        }
    }

    @Transactional(readOnly = true)
    fun haeValmistumispyynnonAsiakirjaVirkailijalle(
        valmistumispyyntoId: Long,
        yliopistoId: Long?,
        asiakirjaId: Long
    ): AsiakirjaDTO? {
        val valmistumispyynto = valmistumispyyntoRepository.findByIdOrNull(valmistumispyyntoId)
            ?: return null
        val onkoSamaYliopisto = valmistumispyynto.opintooikeus?.yliopisto?.id == yliopistoId
        val kuuluukoAsiakirjaValmistumispyyntoon =
            valmistumispyynto.yhteenvetoAsiakirja?.id == asiakirjaId ||
                valmistumispyynto.liitteetAsiakirja?.id == asiakirjaId

        return if (onkoSamaYliopisto && kuuluukoAsiakirjaValmistumispyyntoon) {
            asiakirjaRepository.findByIdOrNull(asiakirjaId)?.toLadattavaDto()
        } else {
            null
        }
    }

    @Transactional(readOnly = true)
    fun haeTyoskentelyjaksonAsiakirja(
        userId: String,
        valmistumispyyntoId: Long,
        asiakirjaId: Long
    ): AsiakirjaDTO? {
        val vastuuhenkilo = haeKayttaja(userId)
        val valmistumispyynto = valmistumispyyntoRepository.findByIdOrNull(valmistumispyyntoId)
            ?: return null
        if (!vastuuhenkilo.voiHyvaksya(valmistumispyynto)) {
            return null
        }

        val asiakirja = asiakirjaRepository.findByIdOrNull(asiakirjaId) ?: return null
        if (asiakirja.tyoskentelyjakso?.opintooikeus?.id != valmistumispyynto.opintooikeus?.id) {
            return null
        }
        return asiakirja.toLadattavaDto()
    }

    private fun haeKayttaja(userId: String) =
        kayttajaRepository.findOneByUserId(userId)
            .orElseThrow { EntityNotFoundException(KAYTTAJA_NOT_FOUND_ERROR) }

    private fun Kayttaja.voiHyvaksya(valmistumispyynto: Valmistumispyynto): Boolean =
        yliopistotAndErikoisalat.any { yliopistoErikoisala ->
            val onkoSamaYliopisto =
                yliopistoErikoisala.yliopisto?.id == valmistumispyynto.opintooikeus?.yliopisto?.id
            val tehtavat = yliopistoErikoisala.vastuuhenkilonTehtavat.map { it.nimi }
            val onkoOikeaTehtava =
                if (valmistumispyynto.opintooikeus?.erikoisala?.id == YEK_ERIKOISALA_ID) {
                tehtavat.contains(VastuuhenkilonTehtavatyyppiEnum.YEK_VALMISTUMINEN)
            } else {
                yliopistoErikoisala.erikoisala?.id ==
                    valmistumispyynto.opintooikeus?.erikoisala?.id &&
                    tehtavat.contains(
                        VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_HYVAKSYNTA
                    )
            }
            onkoSamaYliopisto && onkoOikeaTehtava
        }

    private fun Asiakirja.toLadattavaDto(): AsiakirjaDTO = asiakirjaMapper.toDto(this).apply {
        asiakirjaData?.fileInputStream = ByteArrayInputStream(this@toLadattavaDto.asiakirjaData?.data)
    }
}
