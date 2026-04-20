import com.boomi.execution.ExecutionUtil
import groovy.json.JsonSlurper
import java.util.logging.Level

/**
 * Centralized logger wrapper that adds process context (execution ID
 * and custom tags) to every log line. Keeps log output greppable
 * across a distributed run — search by executionId or by tag values.
 */
class ContextLogger {

    private final def baseLogger
    private final String executionId
    private final Map<String, String> tags

    ContextLogger(Map<String, String> tags = [:]) {
        this.baseLogger  = ExecutionUtil.getBaseLogger()
        this.executionId = ExecutionUtil.getExecutionId() ?: 'local'
        this.tags        = tags
    }

    void info(String message)  { log(Level.INFO, message, null) }
    void warn(String message)  { log(Level.WARNING, message, null) }
    void error(String message, Throwable t = null) { log(Level.SEVERE, message, t) }

    private void log(Level level, String message, Throwable t) {
        def tagStr = tags.collect { k, v -> "${k}=${v}" }.join(' ')
        def line   = "[${executionId}] ${tagStr} ${message}".trim()
        if (t) {
            baseLogger.log(level, line, t)
        } else {
            baseLogger.log(level, line)
        }
    }
}

/**
 * Wraps any thrown exception with document-level context so downstream
 * error handling (or Boomi alerting) sees *which* record failed.
 */
class DocumentProcessingError extends RuntimeException {
    int documentIndex
    String recordId

    DocumentProcessingError(int index, String recordId, String msg, Throwable cause) {
        super("Doc ${index} (id=${recordId}): ${msg}", cause)
        this.documentIndex = index
        this.recordId      = recordId
    }
}

// ---- Main processing loop ----
def logger = new ContextLogger([source: 'orders-api'])

for (int i = 0; i < dataContext.getDataCount(); i++) {
    def inputStream = dataContext.getStream(i)
    def props       = dataContext.getProperties(i)
    def recordId    = 'unknown'

    try {
        def bytes  = inputStream.bytes
        def record = new JsonSlurper().parse(new ByteArrayInputStream(bytes))
        recordId   = record.id?.toString() ?: 'unknown'

        logger.info("Processing record ${recordId}")

        // ... do work here ...

        dataContext.storeStream(new ByteArrayInputStream(bytes), props)
        logger.info("Completed record ${recordId}")
    } catch (Exception e) {
        logger.error("Failed to process document ${i}", e)
        throw new DocumentProcessingError(i, recordId, e.message, e)
    }
}
