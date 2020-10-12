package fi.elsapalvelu.elsa.service
import fi.elsapalvelu.elsa.service.dto.YliopistoDTO
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.Optional

/**
 * Service Interface for managing [fi.elsapalvelu.elsa.domain.Yliopisto].
 */
interface YliopistoService {

    /**
     * Save a yliopisto.
     *
     * @param yliopistoDTO the entity to save.
     * @return the persisted entity.
     */
    fun save(yliopistoDTO: YliopistoDTO): YliopistoDTO

    /**
     * Get all the yliopistos.
     *
     * @return the list of entities.
     */
    fun findAll(): MutableList<YliopistoDTO>

    /**
     * Get all the yliopistos with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    fun findAllWithEagerRelationships(pageable: Pageable): Page<YliopistoDTO>
    /**
     * Get the "id" yliopisto.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    fun findOne(id: Long): Optional<YliopistoDTO>

    /**
     * Delete the "id" yliopisto.
     *
     * @param id the id of the entity.
     */
    fun delete(id: Long)
}
