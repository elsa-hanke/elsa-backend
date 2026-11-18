package fi.elsapalvelu.elsa.service.arkistointi.job.payload

/** Fixed diagnostics only: never retain the invalid payload or its parsing exception. */
class ArchivePayloadException : IllegalArgumentException("ARCHIVE_PAYLOAD_INVALID")
