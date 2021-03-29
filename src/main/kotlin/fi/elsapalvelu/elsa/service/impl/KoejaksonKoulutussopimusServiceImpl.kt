package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.KoejaksonKoulutussopimusRepository
import fi.elsapalvelu.elsa.service.KoejaksonKoulutussopimusService
import fi.elsapalvelu.elsa.service.dto.KoejaksonKoulutussopimusDTO
import fi.elsapalvelu.elsa.service.mapper.KoejaksonKoulutussopimusMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import javax.persistence.EntityNotFoundException


@Service
@Transactional
class KoejaksonKoulutussopimusServiceImpl(
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository,
    private val koejaksonKoulutussopimusMapper: KoejaksonKoulutussopimusMapper,
    private val koejaksonKoulutussopimusRepository: KoejaksonKoulutussopimusRepository
) : KoejaksonKoulutussopimusService {

    override fun create(
        koejaksonKoulutussopimusDTO: KoejaksonKoulutussopimusDTO,
        userId: String
    ): KoejaksonKoulutussopimusDTO {
        val kirjautunutErikoistuvaLaakari =
            erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)
        var koulutussopimus = koejaksonKoulutussopimusMapper.toEntity(koejaksonKoulutussopimusDTO)
        koulutussopimus.muokkauspaiva = LocalDate.now(ZoneId.systemDefault())
        koulutussopimus.koejakso = kirjautunutErikoistuvaLaakari?.koejakso
        koulutussopimus.koulutuspaikat.forEach { it.koulutussopimus = koulutussopimus }
        koulutussopimus.kouluttajat.forEach { it.koulutussopimus = koulutussopimus }
        koulutussopimus = koejaksonKoulutussopimusRepository.save(koulutussopimus)
        return koejaksonKoulutussopimusMapper.toDto(koulutussopimus)
    }

    override fun update(
        koejaksonKoulutussopimusDTO: KoejaksonKoulutussopimusDTO,
        userId: String
    ): KoejaksonKoulutussopimusDTO {

        var koulutussopimus =
            koejaksonKoulutussopimusRepository.findById(koejaksonKoulutussopimusDTO.id!!)
                .orElseThrow { EntityNotFoundException("Koulutussopimusta ei löydy") }

        val kirjautunutErikoistuvaLaakari =
            erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)

        val updatedKoulutussopimus =
            koejaksonKoulutussopimusMapper.toEntity(koejaksonKoulutussopimusDTO)

        if (kirjautunutErikoistuvaLaakari != null && kirjautunutErikoistuvaLaakari == koulutussopimus.koejakso?.erikoistuvaLaakari) {
            koulutussopimus.erikoistuvanNimi = updatedKoulutussopimus.erikoistuvanNimi
            koulutussopimus.erikoistuvanOpiskelijatunnus =
                updatedKoulutussopimus.erikoistuvanOpiskelijatunnus
            koulutussopimus.erikoistuvanSyntymaaika = updatedKoulutussopimus.erikoistuvanSyntymaaika
            koulutussopimus.erikoistuvanYliopisto = updatedKoulutussopimus.erikoistuvanYliopisto
            koulutussopimus.opintooikeudenMyontamispaiva =
                updatedKoulutussopimus.opintooikeudenMyontamispaiva
            koulutussopimus.koejaksonAlkamispaiva = updatedKoulutussopimus.koejaksonAlkamispaiva
            koulutussopimus.erikoistuvanPuhelinnumero =
                updatedKoulutussopimus.erikoistuvanPuhelinnumero
            koulutussopimus.erikoistuvanSahkoposti = updatedKoulutussopimus.erikoistuvanSahkoposti
            koulutussopimus.lahetetty = updatedKoulutussopimus.lahetetty
            koulutussopimus.muokkauspaiva = LocalDate.now(ZoneId.systemDefault())
            koulutussopimus.vastuuhenkilo = updatedKoulutussopimus.vastuuhenkilo
            koulutussopimus.vastuuhenkilonNimi = updatedKoulutussopimus.vastuuhenkilonNimi
            koulutussopimus.vastuuhenkilonNimike = updatedKoulutussopimus.vastuuhenkilonNimike
            koulutussopimus.kouluttajat.clear()
            koulutussopimus.kouluttajat.addAll(updatedKoulutussopimus.kouluttajat)
            koulutussopimus.kouluttajat.forEach { it.koulutussopimus = koulutussopimus }
            koulutussopimus.koulutuspaikat.clear()
            koulutussopimus.koulutuspaikat.addAll(updatedKoulutussopimus.koulutuspaikat)
            koulutussopimus.koulutuspaikat.forEach { it.koulutussopimus = koulutussopimus }

            if (updatedKoulutussopimus.lahetetty) {
                koulutussopimus.korjausehdotus = null
            }
        }

        koulutussopimus.kouluttajat.forEach {
            if (it.kouluttaja?.user?.id == userId) {
                val updatedKouluttaja =
                    updatedKoulutussopimus.kouluttajat.first { k -> k.id == it.id }
                it.nimi = updatedKouluttaja.nimi
                it.nimike = updatedKouluttaja.nimike
                it.toimipaikka = updatedKouluttaja.toimipaikka
                it.lahiosoite = updatedKouluttaja.lahiosoite
                it.postitoimipaikka = updatedKouluttaja.postitoimipaikka
                it.puhelin = updatedKouluttaja.puhelin
                it.sahkoposti = updatedKouluttaja.sahkoposti

                // Hyväksytty
                if (updatedKoulutussopimus.korjausehdotus == null) {
                    it.sopimusHyvaksytty = true
                    it.kuittausaika = LocalDate.now(ZoneId.systemDefault())
                }
                // Palautettu korjattavaksi
                else {
                    koulutussopimus.korjausehdotus = updatedKoulutussopimus.korjausehdotus
                    koulutussopimus.lahetetty = false
                }
            }
        }

        // Jos sopimus palautetaan, poistetaan kaikki aiemmat kuittaukset
        if (!koulutussopimus.lahetetty) {
            koulutussopimus.kouluttajat.forEach {
                it.sopimusHyvaksytty = false
                it.kuittausaika = null
            }
        }

        koulutussopimus = koejaksonKoulutussopimusRepository.save(koulutussopimus)
        return koejaksonKoulutussopimusMapper.toDto(koulutussopimus)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<KoejaksonKoulutussopimusDTO> {
        return koejaksonKoulutussopimusRepository.findById(id)
            .map(koejaksonKoulutussopimusMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOneByIdAndKouluttajaKayttajaUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonKoulutussopimusDTO> {
        return koejaksonKoulutussopimusRepository.findOneByIdAndKouluttajatKouluttajaUserId(
            id,
            userId
        ).map(koejaksonKoulutussopimusMapper::toDto)
    }

    override fun delete(id: Long) {
        koejaksonKoulutussopimusRepository.deleteById(id)
    }
}
