package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.EtusivuSeurantajaksoDTO
import fi.elsapalvelu.elsa.service.dto.SeurantajaksoDTO
import fi.elsapalvelu.elsa.service.dto.SeurantajaksonTiedotDTO
import java.time.LocalDate

interface SeurantajaksoService {

    fun create(
        seurantajaksoDTO: SeurantajaksoDTO,
        opintooikeusId: Long
    ): SeurantajaksoDTO?

    fun update(
        seurantajaksoDTO: SeurantajaksoDTO,
        userId: String
    ): SeurantajaksoDTO

    fun findByOpintooikeusId(
        opintooikeusId: Long
    ): List<SeurantajaksoDTO>

    fun findByKouluttajaUserId(userId: String): List<SeurantajaksoDTO>

    fun findAvoinByKouluttajaUserId(userId: String): List<EtusivuSeurantajaksoDTO>

    fun findByIdAndKouluttajaUserId(id: Long, userId: String): SeurantajaksoDTO?

    fun findOne(id: Long, opintooikeusId: Long): SeurantajaksoDTO?

    fun findSeurantajaksonTiedot(
        opintooikeusId: Long,
        alkamispaiva: LocalDate,
        paattymispaiva: LocalDate,
        koulutusjaksot: List<Long>
    ): SeurantajaksonTiedotDTO

    fun findSeurantajaksonTiedot(id: Long, userId: String): SeurantajaksonTiedotDTO

    fun delete(id: Long, opintooikeusId: Long)
}
