package fi.elsapalvelu.elsa.service.kayttaja

import fi.elsapalvelu.elsa.service.dto.kayttaja.KayttajaDTO
import fi.elsapalvelu.elsa.service.dto.kayttaja.UserDTO
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
