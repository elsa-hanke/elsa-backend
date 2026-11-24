package fi.elsapalvelu.elsa.web.rest

import fi.elsapalvelu.elsa.cleanup.OpintooikeusCleanupOrchestrator
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/private/api")
class CleanupResource(
    private val cleanupOrchestrator: OpintooikeusCleanupOrchestrator
) {

    @GetMapping("/cleanup/{opintooikeusId}")
    fun cleanup(@PathVariable opintooikeusId: Long) {
        // This endpoint is for testing purposes only. It should not be used in production.
        // It will clear all data from the database.
        cleanupOrchestrator.clearContent(opintooikeusId)
    }

}
