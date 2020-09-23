package fi.oulu.elsa.service
import fi.oulu.elsa.service.dto.TyoskentelyjaksoDTO
import java.util.Optional

/**
 * Service Interface for managing [fi.oulu.elsa.domain.Tyoskentelyjakso].
 */
interface TyoskentelyjaksoService {

    /**
     * Save a tyoskentelyjakso.
     *
     * @param tyoskentelyjaksoDTO the entity to save.
     * @return the persisted entity.
     */
    fun save(tyoskentelyjaksoDTO: TyoskentelyjaksoDTO): TyoskentelyjaksoDTO

    /**
     * Get all the tyoskentelyjaksos.
     *
     * @return the list of entities.
     */
    fun findAll(): MutableList<TyoskentelyjaksoDTO>
    /**
     * Get all the [TyoskentelyjaksoDTO] where Tyoskentelypaikka is `null`.
     *
     * @return the {@link MutableList} of entities.
     */
    fun findAllWhereTyoskentelypaikkaIsNull(): MutableList<TyoskentelyjaksoDTO>
    /**
     * Get all the [TyoskentelyjaksoDTO] where Arviointi is `null`.
     *
     * @return the {@link MutableList} of entities.
     */
    fun findAllWhereArviointiIsNull(): MutableList<TyoskentelyjaksoDTO>

    /**
     * Get the "id" tyoskentelyjakso.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    fun findOne(id: Long): Optional<TyoskentelyjaksoDTO>

    /**
     * Delete the "id" tyoskentelyjakso.
     *
     * @param id the id of the entity.
     */
    fun delete(id: Long)
}
