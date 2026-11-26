package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.metadata.DynastyMetadataMapper
import fi.elsapalvelu.elsa.service.arkistointi.job.payload.ArchivePayloadReader
import java.time.Clock
import org.springframework.stereotype.Component

@Component
internal class DynastyArkistointiAdapterFactory(
    private val properties: ApplicationProperties,
    private val mapper: ObjectMapper,
    private val reader: ArchivePayloadReader,
    private val clock: Clock,
    private val steps: DynastyStepExecutor
) {
    fun create(profile: DynastyArchiveProfile, client: DynastyClient): DynastyArkistointiAdapter {
        val settings = profile.archiveProperties(properties.getArkistointi()).dynasty
        val metadata = DynastyMetadataMapper(profile, settings, mapper, reader, clock)
        return DynastyArkistointiAdapter(profile, client, metadata, steps, settings)
    }
}
