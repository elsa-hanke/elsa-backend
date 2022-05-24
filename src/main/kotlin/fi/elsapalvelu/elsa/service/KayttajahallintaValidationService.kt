package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.KayttajaDTO
import fi.elsapalvelu.elsa.service.dto.UserDTO
import fi.elsapalvelu.elsa.service.dto.kayttajahallinta.KayttajahallintaKayttajaDTO

interface KayttajahallintaValidationService {
    fun validateNewVastuuhenkiloYliopistotAndErikoisalat(kayttajahallintaKayttajaDTO: KayttajahallintaKayttajaDTO): Boolean

    fun validateExistingVastuuhenkiloYliopistotAndErikoisalat(
        kayttajahallintaKayttajaDTO: KayttajahallintaKayttajaDTO,
        existingKayttajaDTO: KayttajaDTO
    ): Boolean

    fun validateVirkailijaIsAllowedToCreateKayttajaByYliopistoId(
        virkailijaUserDTO: UserDTO,
        yliopistoId: Long
    ): Boolean

    fun validateVirkailijaIsAllowedToManageErikoistuvaLaakari(
        virkailijaUserDTO: UserDTO,
        kayttajaId: Long
    ): Boolean

    fun validateVirkailijaIsAllowedToManageKayttaja(virkailijaUserDTO: UserDTO, kayttajaId: Long): Boolean
}
