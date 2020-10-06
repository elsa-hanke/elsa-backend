package fi.oulu.elsa.service
import fi.oulu.elsa.service.dto.EpaOsaamisalueDTO
import java.util.Optional

/**
 * Service Interface for managing [fi.oulu.elsa.domain.EpaOsaamisalue].
 */
interface EpaOsaamisalueService {

    /**
     * Save a epaOsaamisalue.
     *
     * @param epaOsaamisalueDTO the entity to save.
     * @return the persisted entity.
     */
    fun save(epaOsaamisalueDTO: EpaOsaamisalueDTO): EpaOsaamisalueDTO

    /**
     * Get all the epaOsaamisalues.
     *
     * @return the list of entities.
     */
    fun findAll(): MutableList<EpaOsaamisalueDTO>

    /**
     * Get the "id" epaOsaamisalue.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    fun findOne(id: Long): Optional<EpaOsaamisalueDTO>

    /**
     * Delete the "id" epaOsaamisalue.
     *
     * @param id the id of the entity.
     */
    fun delete(id: Long)
}
