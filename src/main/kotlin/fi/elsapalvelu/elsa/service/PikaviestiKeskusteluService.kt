package fi.elsapalvelu.elsa.service
import fi.elsapalvelu.elsa.service.dto.PikaviestiKeskusteluDTO
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.Optional

/**
 * Service Interface for managing [fi.elsapalvelu.elsa.domain.PikaviestiKeskustelu].
 */
interface PikaviestiKeskusteluService {

    /**
     * Save a pikaviestiKeskustelu.
     *
     * @param pikaviestiKeskusteluDTO the entity to save.
     * @return the persisted entity.
     */
    fun save(pikaviestiKeskusteluDTO: PikaviestiKeskusteluDTO): PikaviestiKeskusteluDTO

    /**
     * Get all the pikaviestiKeskustelus.
     *
     * @return the list of entities.
     */
    fun findAll(): MutableList<PikaviestiKeskusteluDTO>

    /**
     * Get all the pikaviestiKeskustelus with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    fun findAllWithEagerRelationships(pageable: Pageable): Page<PikaviestiKeskusteluDTO>
    /**
     * Get the "id" pikaviestiKeskustelu.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    fun findOne(id: Long): Optional<PikaviestiKeskusteluDTO>

    /**
     * Delete the "id" pikaviestiKeskustelu.
     *
     * @param id the id of the entity.
     */
    fun delete(id: Long)
}
