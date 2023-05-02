package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.domain.enumeration.ErikoisalaTyyppi
import fi.elsapalvelu.elsa.service.criteria.NimiErikoisalaAndAvoinCriteria
import fi.elsapalvelu.elsa.service.dto.*
import fi.elsapalvelu.elsa.service.dto.enumeration.ValmistumispyynnonHyvaksyjaRole
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.multipart.MultipartFile

interface ValmistumispyyntoService {
    fun findErikoisalaTyyppiByOpintooikeusId(opintooikeusId: Long): ErikoisalaTyyppi

    fun findOneByOpintooikeusId(opintooikeusId: Long): ValmistumispyyntoDTO?

    fun findSuoritustenTila(
        opintooikeusId: Long,
        erikoisalaTyyppi: ErikoisalaTyyppi
    ): VanhentuneetSuorituksetDTO

    fun findAllForVastuuhenkiloByCriteria(
        userId: String,
        valmistumispyyntoCriteria: NimiErikoisalaAndAvoinCriteria,
        pageable: Pageable
    ): Page<ValmistumispyyntoListItemDTO>

    fun findAllForVirkailijaByCriteria(
        userId: String,
        valmistumispyyntoCriteria: NimiErikoisalaAndAvoinCriteria,
        pageable: Pageable
    ): Page<ValmistumispyyntoListItemDTO>

    fun findOneByIdAndVastuuhenkiloOsaamisenArvioijaUserId(
        id: Long,
        userId: String
    ): ValmistumispyyntoOsaamisenArviointiDTO?

    fun findOneByIdAndVirkailijaUserId(
        id: Long,
        userId: String
    ): ValmistumispyynnonTarkistusDTO?

    fun findOneByIdAndVastuuhenkiloHyvaksyjaUserId(
        id: Long,
        userId: String
    ): ValmistumispyynnonTarkistusDTO?

    fun create(
        opintooikeusId: Long,
        uusiValmistumispyyntoDTO: UusiValmistumispyyntoDTO
    ): ValmistumispyyntoDTO

    fun update(
        opintooikeusId: Long,
        uusiValmistumispyyntoDTO: UusiValmistumispyyntoDTO
    ): ValmistumispyyntoDTO

    fun updateOsaamisenArviointiByOsaamisenArvioijaUserId(
        id: Long,
        userId: String,
        osaamisenArviointiDTO: ValmistumispyyntoOsaamisenArviointiFormDTO
    ): ValmistumispyyntoDTO

    fun updateValmistumispyyntoByHyvaksyjaUserId(
        id: Long,
        userId: String,
        hyvaksyntaFormDTO: ValmistumispyyntoHyvaksyntaFormDTO
    ): ValmistumispyynnonTarkistusDTO

    fun updateTarkistusByVirkailijaUserId(
        id: Long,
        userId: String,
        valmistumispyynnonTarkistusDTO: ValmistumispyynnonTarkistusUpdateDTO,
        laillistamistodistus: MultipartFile?
    ): ValmistumispyynnonTarkistusDTO?

    fun existsByOpintooikeusId(opintooikeusId: Long): Boolean

    fun findArviointienTilaByIdAndOsaamisenArvioijaUserId(
        id: Long,
        userId: String
    ): ValmistumispyyntoArviointienTilaDTO?

    fun getValmistumispyynnonHyvaksyjaRole(userId: String): ValmistumispyynnonHyvaksyjaRole?

    fun getValmistumispyynnonAsiakirja(
        userId: String,
        valmistumispyyntoId: Long,
        asiakirjaId: Long
    ): AsiakirjaDTO?

    fun getValmistumispyynnonAsiakirjaVirkailija(
        valmistumispyyntoId: Long,
        yliopistoId: Long?,
        asiakirjaId: Long
    ): AsiakirjaDTO?
}
