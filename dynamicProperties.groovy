import com.boomi.execution.ExecutionUtil
import groovy.json.JsonSlurper
import java.text.SimpleDateFormat

/**
 * Helper for stamping Dynamic Document Properties (DDPs) and
 * Dynamic Process Properties (DPPs) at runtime.
 *
 * Boomi key conventions:
 *   DDP -> set on the per-document Properties object with the
 *          "document.dynamic.userdefined." prefix.
 *   DPP -> set via ExecutionUtil; visible to the whole process run.
 */
class PropertyStamper {

    private static final String DDP_PREFIX = 'document.dynamic.userdefined.'

    /** Sets a Dynamic Document Property on the given Properties object. */
    static void setDdp(Properties props, String name, String value) {
        props.setProperty(DDP_PREFIX + name, value ?: '')
    }

    /** Sets a Dynamic Process Property (visible to the whole process run). */
    static void setDpp(String name, String value) {
        ExecutionUtil.setDynamicProcessProperty(name, value ?: '', false)
    }

    /** Builds a timestamped filename like "orders_20261119_143052.json". */
    static String buildFilename(String prefix, String extension) {
        def timestamp = new SimpleDateFormat('yyyyMMdd_HHmmss').format(new Date())
        return "${prefix}_${timestamp}.${extension}"
    }

    /** Returns the current environment tag, falling back to "unknown". */
    static String getEnvironment() {
        return ExecutionUtil.getDynamicProcessProperty('env') ?: 'unknown'
    }
}

// ---- Main processing loop ----
for (int i = 0; i < dataContext.getDataCount(); i++) {
    def inputStream = dataContext.getStream(i)
    def props       = dataContext.getProperties(i)

    // Buffer the bytes so we can peek at the payload AND still pass it through.
    def bytes  = inputStream.bytes
    def record = new JsonSlurper().parse(new ByteArrayInputStream(bytes))

    // Per-document stamps
    PropertyStamper.setDdp(props, 'filename',
        PropertyStamper.buildFilename('order', 'json'))
    PropertyStamper.setDdp(props, 'routingKey', record.region ?: 'default')
    PropertyStamper.setDdp(props, 'customerId', record.customerId?.toString())

    // Process-wide stamp (last value wins across documents)
    PropertyStamper.setDpp('lastProcessedAt', new Date().toString())

    // Pass the original payload through unchanged
    dataContext.storeStream(new ByteArrayInputStream(bytes), props)
}
