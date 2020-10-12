package fi.elsapalvelu.elsa.service
import fi.elsapalvelu.elsa.service.dto.ErikoistuvaLaakariDTO
import java.util.Optional

/**
 * Service Interface for managing [fi.elsapalvelu.elsa.domain.ErikoistuvaLaakari].
 */
interface ErikoistuvaLaakariService {

    /**
     * Save a erikoistuvaLaakari.
     *
     * @param erikoistuvaLaakariDTO the entity to save.
     * @return the persisted entity.
     */
    fun save(erikoistuvaLaakariDTO: ErikoistuvaLaakariDTO): ErikoistuvaLaakariDTO

    /**
     * Get all the erikoistuvaLaakaris.
     *
     * @return the list of entities.
     */
    fun findAll(): MutableList<ErikoistuvaLaakariDTO>
    /**
     * Get all the [ErikoistuvaLaakariDTO] where Hops is `null`.
     *
     * @return the {@link MutableList} of entities.
     */
    fun findAllWhereHopsIsNull(): MutableList<ErikoistuvaLaakariDTO>
    /**
     * Get all the [ErikoistuvaLaakariDTO] where Koejakso is `null`.
     *
     * @return the {@link MutableList} of entities.
     */
    fun findAllWhereKoejaksoIsNull(): MutableList<ErikoistuvaLaakariDTO>

    /**
     * Get the "id" erikoistuvaLaakari.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    fun findOne(id: Long): Optional<ErikoistuvaLaakariDTO>

    /**
     * Delete the "id" erikoistuvaLaakari.
     *
     * @param id the id of the entity.
     */
    fun delete(id: Long)
}
