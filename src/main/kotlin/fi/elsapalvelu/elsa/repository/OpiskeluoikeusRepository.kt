package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Opiskeluoikeus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository


@Repository
interface OpiskeluoikeusRepository : JpaRepository<Opiskeluoikeus, Long>
