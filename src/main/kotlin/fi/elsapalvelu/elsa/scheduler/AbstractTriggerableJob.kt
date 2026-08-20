package fi.elsapalvelu.elsa.scheduler

import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class AbstractTriggerableJob : TriggerableJob {
    protected val log: Logger = LoggerFactory.getLogger(javaClass)
}

