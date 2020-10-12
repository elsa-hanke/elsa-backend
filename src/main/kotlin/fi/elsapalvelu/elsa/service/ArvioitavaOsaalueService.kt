package fi.elsapalvelu.elsa.service
import fi.elsapalvelu.elsa.service.dto.ArvioitavaOsaalueDTO
import java.util.Optional

/**
 * Service Interface for managing [fi.elsapalvelu.elsa.domain.ArvioitavaOsaalue].
 */
interface ArvioitavaOsaalueService {

    /**
     * Save a arvioitavaOsaalue.
     *
     * @param arvioitavaOsaalueDTO the entity to save.
     * @return the persisted entity.
     */
    fun save(arvioitavaOsaalueDTO: ArvioitavaOsaalueDTO): ArvioitavaOsaalueDTO

    /**
     * Get all the arvioitavaOsaalues.
     *
     * @return the list of entities.
     */
    fun findAll(): MutableList<ArvioitavaOsaalueDTO>

    /**
     * Get the "id" arvioitavaOsaalue.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    fun findOne(id: Long): Optional<ArvioitavaOsaalueDTO>

    /**
     * Delete the "id" arvioitavaOsaalue.
     *
     * @param id the id of the entity.
     */
    fun delete(id: Long)
}
