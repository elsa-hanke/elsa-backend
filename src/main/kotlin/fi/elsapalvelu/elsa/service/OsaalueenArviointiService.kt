package fi.elsapalvelu.elsa.service
import fi.elsapalvelu.elsa.service.dto.OsaalueenArviointiDTO
import java.util.Optional

/**
 * Service Interface for managing [fi.elsapalvelu.elsa.domain.OsaalueenArviointi].
 */
interface OsaalueenArviointiService {

    /**
     * Save a osaalueenArviointi.
     *
     * @param osaalueenArviointiDTO the entity to save.
     * @return the persisted entity.
     */
    fun save(osaalueenArviointiDTO: OsaalueenArviointiDTO): OsaalueenArviointiDTO

    /**
     * Get all the osaalueenArviointis.
     *
     * @return the list of entities.
     */
    fun findAll(): MutableList<OsaalueenArviointiDTO>

    /**
     * Get the "id" osaalueenArviointi.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    fun findOne(id: Long): Optional<OsaalueenArviointiDTO>

    /**
     * Delete the "id" osaalueenArviointi.
     *
     * @param id the id of the entity.
     */
    fun delete(id: Long)
}
